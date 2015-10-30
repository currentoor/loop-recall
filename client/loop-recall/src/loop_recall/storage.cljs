(ns loop-recall.storage
  (:require-macros [loop-recall.macros :refer [inspect]])
  (:require
   [ajax.core :refer [GET POST]]
   [datascript.core :as d]))

(def schema {:card/question  {}
             :card/answer    {}
             :card/deck      {:db/valueType :db.type/ref}
             :card/learn-seq {}
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
  (let [db-result (d/q '[:find ?c ?question ?answer ?deck-id ?deckname ?remote-id
                         :where
                         [?c :card/question ?question]
                         [?c :card/answer ?answer]
                         [?c :card/due? true]
                         [?c :card/remote-deck-id ?deck-id]
                         [?c :card/remote-id ?remote-id]
                         [?c :card/deck-name ?deckname]
                         ]
                       @conn)]
    ;; TODO macro to clean this for sorted up.
    (->> (for [[id q a deck-id deck-name remote-id] db-result]
           {:id        id
            :question  q
            :answer    a
            :deck-id   deck-id
            :remote-id remote-id
            :deck-name deck-name})
         (sort-by :id))))

(defn all-decks []
  (let [db-result (d/q '[:find ?d ?deckname
                         :where
                         [?d :deck/name ?deckname]]
                       @conn)]
    (->> (for [[id deck-name] db-result]
           {:id   id
            :name deck-name})
         (sort-by :id))))


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

;; normalize
(defn create-deck [& {:keys [id name]}]
  (d/transact conn [{:db/id          -1
                     :deck/name      name
                     :deck/remote-id id
                     :deck/cards     []}]))

;; normalize
(defn create-card [& {:keys [deck-id question answer] :as args}]
  (when (and deck-id question answer)
    (d/transact conn [{:db/id          -1
                       :card/question  question
                       :card/answer    answer
                       :card/due?      true
                       ;:card/deck      (js/parseInt deck-id)
                       }
                      ])))

(defn mutate [graph-ql & {cb :cb}]
  (inspect graph-ql)
  (POST "http://localhost:3000/graph_ql/mutation"
      {:params          {:mutation graph-ql}
       :response-format :transit
       :handler         (if cb
                          #(cb %))}))

(defn update-card [id & {:keys [remote-id question answer response-quality] :as card}]
  (when question (d/transact! conn [[:db/add id :card/question question]]))
  (when answer (d/transact! conn [[:db/add id :card/answer answer]]))
  (when (and question answer remote-id)
    (mutate (str "mutation bar { updateCard(question: \"" question
                 "\", answer: \"" answer
                 "\", id: \"" remote-id
                 "\") {id} }")))
  ;; (when (and learn-seq response-quality)
  ;;   (let [{interval :days-to-next new-learn-seq :learn-seq}
  ;;         (algo/determine-next-interval response-quality learn-seq)
  ;;         interval* (if (= 0 response-quality) 1 interval)]
  ;;     (d/transact! conn [[:db/add id :card/learn-seq new-learn-seq]
  ;;                        [:db/add id :card/due-date (inc-by-interval (today) interval*)]])))
  )

(defn delete-card [id]
  (d/transact! conn [[:db.fn/retractEntity id]]))

;; multi-method
(defn- normalize-card [{:strs [id question answer deck] :as card}]
  (let [tempid    (-> id int (* -1))
        deck-id   (deck "id")
        deck-name (deck "name")]
    {:db/id               tempid
     :card/remote-id      id
     :card/question       question
     :card/answer         answer
     :card/due?           true
     :card/remote-deck-id deck-id
     :card/deck-name      deck-name}))

(defn- normalize-deck [{:strs [id name cards]}]
  (let [tempid (-> id int (* -1))]
    {:db/id          tempid
     :deck/name      name
     :deck/remote-id id}))

(defn insert-due-cards [data]
  (let [cards  (get-in data ["data" "user" "cards"])
        ncards (mapv normalize-card cards)]
    (d/transact! conn ncards)))

(defn insert-decks [data]
  (let [decks  (get-in data ["data" "decks"])
        ndecks (mapv normalize-deck decks)]
    (d/transact! conn ndecks)))
