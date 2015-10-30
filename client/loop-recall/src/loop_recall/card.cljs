(ns loop-recall.card
  (:require-macros [loop-recall.macros :refer [inspect]]
                   [loop-recall.material :as mui])
  (:require [loop-recall.storage :as store :refer [conn set-system-attrs! system-attr]]
            [loop-recall.utility :refer [query]]
            [rum.core :as rum :refer-macros [defc defcs defcc] :include-macros true]))

(defn- handle-change-cb [key]
  (fn [e]
    (set-system-attrs! key (.. e -target -value))))

(defcs q&a-base [state & {:keys [question    question-cb
                                 answer      answer-cb
                                 submit-text submit-cb]}]
  [:div.row
   [:div.col-xs-12.col-sm-10.col-sm-offset-1
    (mui/text-field {:hintText          "Ex: Ex: How do you remove all files from staging?"
                     :multiLine         true
                     :fullWidth         true
                     :rows              2
                     :value             question
                     :onChange          question-cb
                     :floatingLabelText "Question"})]
   [:div.col-xs-12.col-sm-10.col-sm-offset-1
    (mui/text-field {:hintText          "Ex: Ex: git reset HEAD -- ."
                     :multiLine         true
                     :fullWidth         true
                     :rows              2
                     :value             answer
                     :onChange          answer-cb
                     :floatingLabelText "Answer"})]

   ])

(defmulti q&a (fn [_ type] type))

(defmethod q&a :new [db _]
  (let [question (or (system-attr db :new/card-question) "")
        answer   (or (system-attr db :new/card-answer) "")]
    (q&a-base
     :question    question
     :question-cb (handle-change-cb :new/card-question)
     :answer      answer
     :answer-cb   (handle-change-cb :new/card-answer))
    [:div.col-xs-6.col-xs-offset-6.create-card-btn.center
     (mui/raised-button {:onClick   #(and (store/create-card
                                           :deck-id  (or (system-attr db :new-stuff/target-deck-id)
                                                         (-> (store/all-decks) first :id))
                                           :question (system-attr db :new/card-question)
                                           :answer   (system-attr db :new/card-answer))
                                          (set-system-attrs! :new/card-question ""
                                                             :new/card-answer ""))
                         :secondary true
                         :label     "Create Card"})]))

(defmethod q&a :edit [db _]
  (let [question (or (system-attr db :edit/card-question) "")
        answer   (or (system-attr db :edit/card-answer) "")]
    (q&a-base
     :question    question
     :question-cb (handle-change-cb :edit/card-question)
     :answer      answer
     :answer-cb   (handle-change-cb :edit/card-answer))))

(defcs new-card [state db]
  ;; (query
  ;;  "query getDecks { decks {id, name} }"
  ;;  (fn [data]
  ;;    [:div
  ;;     (mui/card
  ;;      [:div.row
  ;;       [:div.col-xs-10.col-xs-offset-1.target-deck
  ;;        (mui/select-field {:value         (or (system-attr db :new-stuff/target-deck-id)
  ;;                                              (-> (data "decks") first (get "id")))
  ;;                           :onChange      #(set-system-attrs! :new-stuff/target-deck-id (.-id %3))
  ;;                           :valueMember   "id"
  ;;                           :displayMember "name"
  ;;                           :menuItems     (sort-by #(get % "name") (data "decks"))
  ;;                           })]]
  ;;      (q&a db :new))]))
  )

(defn card-menu-options
  [& {:keys [on-edit on-delete]}]
  (mui/list
   (mui/list-divider)
   (mui/list-item
    {:primaryText "Edit"
     :onClick     on-edit
     :leftIcon    (mui/icon-button {:style         {:right "-12px"}
                                    :iconClassName "material-icons"}
                                   "edit")})
   (mui/list-divider)
   (mui/list-item
    {:primaryText "Delete"
     :onClick     on-delete
     :leftIcon    (mui/icon-button {:style         {:right "-12px"}
                                    :iconClassName "material-icons"}
                                   "delete")})))

(defn load-q-&-a [question answer]
  (set-system-attrs! :edit/card-question question)
  (set-system-attrs! :edit/card-answer answer))

(defcs study-header < (rum/local false) [state db title subtitle card-id remote-id question answer]
  (let [this            (:rum/react-component state)
        show-card-menu? (:rum/local state)]
    [:div
     [:div.row
      [:div.col-xs-10
       (mui/card-title {:title title :subtitle subtitle})]
      [:div.col-xs-2.right
       (mui/icon-button {:iconClassName "material-icons"
                         :onClick       #(swap! show-card-menu? not)}
                        "more_horiz")]]

     (if @show-card-menu?
       [:div.row
        [:div.col-xs-12
         (card-menu-options :on-edit   #(.show (.. this -refs -editModal))
                            :on-delete #(.show (.. this -refs -deleteModal)))]])

     (mui/dialog {:title                  "Edit"
                  :actions                [{:text "cancel"}
                                           {:text       "submit"
                                            :onTouchTap #(and (store/update-card
                                                               card-id
                                                               :question (system-attr db :edit/card-question)
                                                               :answer (system-attr db :edit/card-answer)
                                                               :remote-id remote-id)
                                                              (.dismiss (.. this -refs -editModal)))}]
                  :onShow                 #(load-q-&-a question answer)
                  :autoDetectWindowHeight true
                  :autoScrollBodyContent  true
                  :ref                    "editModal"}
                 (q&a db :edit))

     (mui/dialog {:title   "Delete"
                  :actions [{:text "cancel"}
                            {:text       "submit"
                             ;; :onTouchTap #(and (store/delete-card card-id)
                             ;;                   (.dismiss (.. this -refs -deleteModal)))
                             }]
                  :ref     "deleteModal"}
                 "Are you sure you want to delete this card?")]
    ))

(defn toggle-answer []
  (let [previous (system-attr @conn :show-answer?)]
    (set-system-attrs! :show-answer? (not previous))))

(defc study-card [db question answer card-id remote-id deckname subtitle & {:keys [prev next]}]
  [:div.row
   [:div.col-xs-12.col-sm-10.col-sm-offset-1.col-md-8.col-md-offset-2.col-lg-6.col-lg-offset-3
    (mui/card
     (study-header db deckname
                   subtitle
                   card-id
                   remote-id
                   question
                   answer)

     [:div..study-question
      (mui/card-text question)]

     [:div.row
      [:div.col-xs-2.center
       (mui/icon-button
        {:iconClassName   "material-icons"
         :tooltipPosition "top-right"
         :tooltip         "Previous Card"
         :onClick          prev}
        "arrow_back")]

      [:div.col-xs-8.center
       (if (system-attr db :show-answer?)
         (mui/card-actions
          (mui/raised-button {:onClick #(and (toggle-answer)
                                             ;; (mutate (str "mutation bar { answerCard(card_id: \"" card-id
                                             ;;              "\", user_id: \"" 1
                                             ;;              "\", response: 0) {due_date_str} }"))
                                             )
                              :backgroundColor "#820000" :primary true :label "Wrong"})
          (mui/raised-button {:onClick toggle-answer :label "Almost"})
          (mui/raised-button {:onClick toggle-answer :secondary true :label "Correct"}))
         (mui/card-actions
          (mui/flat-button {:onClick toggle-answer :label "Show Answer"})))]

      [:div.col-xs-2.center
       (mui/icon-button
        {:iconClassName   "material-icons"
         :tooltipPosition "top-left"
         :tooltip         "Next Card"
         :onClick         next}
        "arrow_forward")]]

     [:div.study-answer
      (if (system-attr db :show-answer?)
        (mui/card-text answer))])]])
