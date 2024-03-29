(ns loop-recall.storage
  (:require-macros [loop-recall.macros :refer [inspect]])
  (:require
   [ajax.core :refer [GET POST]]
   [loop-recall.utility :refer [query] :as util]
   [datascript.core :as d]))

(def schema {:card/question  {}
             :card/answer    {}
             :card/deck      {:db/valueType :db.type/ref}
             :card/due-date  {}

             :deck/name      {}
             :deck/cards     {:db/cardinality :db.cardinality/many
                              :db/valueType   :db.type/ref}
             :user           {}})

(defonce conn (d/create-conn schema))

(defn set-system-attrs!
  "Entity with id=0 is used for storing auxilary view information
   like filter value and selected group."
  [& args]
  (d/transact! conn
               (for [[attr value] (partition 2 args)]
                 (if value
                   [:db/add 0 attr value]
                   [:db.fn/retractAttribute 0 attr]))))

(defn system-attr
  "Get system attributes."
  ([db attr]
   (get (d/entity db 0) attr))
  ([db attr & attrs]
   (mapv #(system-attr db %) (concat [attr] attrs))))

;;; Queries

(defn user-info []
  (-> (d/q '[:find ?u-n ?u-e
             :where
             [?e :user/name ?u-n]
             [?e :user/email ?u-e]]
           @conn)
      first))

(defn due-cards []
  (let [db-result (d/q '[:find ?c ?question ?answer ?deck-id ?deckname ?remote-id ?c-intv
                         :where
                         [?c :card/question ?question]
                         [?c :card/answer ?answer]
                         [?c :card/due? true]
                         [?c :card/correct-interval ?c-intv]
                         [?c :card/remote-deck-id ?deck-id]
                         [?c :card/remote-id ?remote-id]
                         [?c :card/deck-name ?deckname]
                         ]
                       @conn)]
    ;; TODO clean this for sorted up?
    (->> (for [[id q a deck-id deck-name remote-id ?c-intv] db-result]
           {:id               id
            :question         q
            :answer           a
            :correct-interval ?c-intv
            :deck-id          deck-id
            :remote-id        remote-id
            :deck-name        deck-name})
         (sort-by :id))))

(defn all-decks []
  (let [db-result (d/q '[:find ?d ?deckname ?remote-id
                         :where
                         [?d :deck/name ?deckname]
                         [?d :deck/remote-id ?remote-id]]
                       @conn)]
    (->> (for [[id deck-name remote-id] db-result]
           {:id        id
            :name      deck-name
            :remote-id remote-id})
         (sort-by :name))))

(defn cards [deck-id]
  (let [db-result (d/q '[:find ?d ?c ?question ?answer ?learn-seq ?due-date ?remote-id
                         :where
                         [?d :deck/cards ?c]
                         [?c :card/question ?question]
                         [?c :card/answer ?answer]
                         [?c :card/remote-id ?remote-id]
                         [?c :card/learn-seq ?learn-seq]
                         [?c :card/due-date ?due-date]]
                       @conn)]
    (->> (filter #(= (first %) deck-id) db-result)
         (sort-by second)
         (map (fn [[_ id q a ls dd ri]]
                {:id        id
                 :question  q
                 :answer    a
                 :learn-seq ls
                 :remote-id ri
                 :due-date  dd})))))

(declare insert-due-cards)
(declare insert-decks)

(defn mutate [graph-ql & {:keys [refresh?] :or {refresh? true}}]
  (if (.getItem js/localStorage "userToken")
      (POST (str js/window.apiRoot "graph_ql/mutation")
       {:params          {:mutation graph-ql}
        :headers         {"Authorization" (str "Bearer " (.getItem js/localStorage "userToken"))}
        :response-format :transit
        :handler         #(and refresh?
                               (util/fetch-due-cards insert-due-cards)
                               (util/fetch-decks insert-decks))})))

;; Ghetto encoding because graph-ql is defined by strings.
(defn escape [s]
  (-> s
      (clojure.string/replace #"\\\\\\\"" "__999__")
      (clojure.string/replace #"\\\\\"" "__888__")
      (clojure.string/replace #"\\\"" "__777__")
      (clojure.string/replace #"\"" "__666__")))

(defn unescape [s]
  (-> s
      (clojure.string/replace #"__999__" "\\\\\\\"" )
      (clojure.string/replace #"__888__" "\\\\\"" )
      (clojure.string/replace #"__777__" "\\\"" )
      (clojure.string/replace #"__666__" "\"" )))

(defn create-card [& {:keys [deck-name remote-deck-id question answer] :as args}]
  (mutate (str "mutation bar { createCard(deck_id: " remote-deck-id
               ",question: \"" (escape question)
               "\", answer: \"" (escape answer)
               "\") {id} }"
               )
          :cb #(let [remote-id (get-in % ["data" "createCard" "id"])]
                 (d/transact conn [{:db/id               -1
                                    :card/remote-id      remote-id
                                    :card/question       question
                                    :card/answer         answer
                                    :card/due?           true
                                    :card/remote-deck-id remote-deck-id
                                    :card/deck-name      deck-name}]))))

(defn create-deck [name]
  (mutate (str "mutation bar { createDeck(name: \"" name "\") {id} }"
               )
          :cb #(let [_ (inspect %)
                     remote-id (inspect (get-in % ["data" "createDeck" "id"]))]
                 (d/transact conn [{:db/id          -1
                                    :deck/name      name
                                    :deck/remote-id remote-id
                                    :deck/cards     []}]))))

(defn update-card [id & {:keys [remote-id question answer] :as card}]
  (when (and question answer remote-id)
    (d/transact! conn [[:db/add id :card/question question]
                       [:db/add id :card/answer answer]])
    (mutate (str "mutation bar { updateCard(question: \"" (escape question)
                 "\", answer: \"" (escape answer)
                 "\", id: \"" remote-id
                 "\") {id} }")
            :refresh? false)))

(defn simple-update-card [atm & {:keys [remote-id question answer cb] :as card}]
  ;; Let the hacking commence!
  (when (and question answer remote-id)
    (swap! atm
           (fn [prev]
             (let [prev-cards (get-in prev [:data "cardsFromDeck"])
                   new-cards  (mapv
                               (fn [c]
                                 (if (= (c "id") remote-id)
                                   {"id"       remote-id
                                    "question" question
                                    "answer"   answer}
                                   c))
                               prev-cards)]
               (assoc-in prev [:data "cardsFromDeck"] new-cards))))
    (mutate (str "mutation bar { updateCard(question: \"" (escape question)
                 "\", answer: \"" (escape answer)
                 "\", id: \"" remote-id
                 "\") {id} }")
            :cb cb)))

(defn answer-card [id & {:keys [remote-id response]}]
  (when response
    (d/transact! conn [[:db/add id :card/due? false]])
    (mutate (str "mutation bar { answerCard(card_id: \""
                 remote-id
                 "\", response: " response
                 ") {due_date_str} }")
            :refresh? false)))

(defn delete-card [id remote-id]
  (d/transact! conn [[:db.fn/retractEntity id]])
  (mutate (str "mutation bar { deleteCard(id: \""
               remote-id
               "\") {id} }")
          :refresh? false))

(defn delete-deck [local-id remote-id]
  (d/transact! conn [[:db.fn/retractEntity local-id]])
  (mutate (str "mutation bar { deleteDeck(id: \""
               remote-id
               "\") {id} }")))

(defn simple-delete-card [atm remote-id]
  (swap! atm
         (fn [prev]
           (let [prev-cards (get-in prev [:data "cardsFromDeck"])
                 new-cards  (filter (fn [c] (not (= (c "id") remote-id)))
                             prev-cards)]
             (assoc-in prev [:data "cardsFromDeck"] new-cards))))
  (mutate (str "mutation bar { deleteCard(id: \""
               remote-id
               "\") {id} }")))

;; multi-method
(defn- normalize-card [{:strs [id correct_interval question answer deck] :as card}]
  (let [tempid    (-> id int (* -1))
        deck-id   (deck "id")
        deck-name (deck "name")]
    {:db/id                 tempid
     :card/remote-id        id
     :card/question         (unescape question)
     :card/answer           (unescape answer)
     :card/due?             true
     :card/correct-interval correct_interval
     :card/remote-deck-id   deck-id
     :card/deck-name        deck-name}))

(defn- normalize-deck [{:strs [id name cards]}]
  (let [tempid (-> id int (* -1))]
    {:db/id          tempid
     :deck/name      name
     :deck/remote-id id}))

(defn- remove-entities-locally [ids]
  (mapv (fn [id] (d/transact! conn [[:db.fn/retractEntity id]]))
        ids))

(defn insert-due-cards [data]
  (let [old-card-ids  (map :id (due-cards))
        cards         (get-in data ["data" "dueCards"])
        ncards        (mapv normalize-card cards)]
    (remove-entities-locally old-card-ids)
    (d/transact! conn ncards)))

(defn insert-decks [data]
  (let [old-deck-ids (map :id (all-decks))
        decks        (get-in data ["data" "decks"])
        ndecks       (mapv normalize-deck decks)]
    (remove-entities-locally old-deck-ids)
    (d/transact! conn ndecks)))
