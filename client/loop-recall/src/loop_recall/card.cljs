(ns loop-recall.card
  (:require-macros [loop-recall.macros :refer [inspect]]
                   [loop-recall.material :as mui])
  (:require [loop-recall.storage :as store :refer [conn set-system-attrs! system-attr]]
            [loop-recall.utility :refer [query mutation]]
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

   [:div.col-xs-6.col-xs-offset-6.create-card-btn.center
    (mui/raised-button {:onClick   submit-cb
                        :secondary true
                        :label     submit-text})]])

(defmulti q&a (fn [_ type] type))

(defmethod q&a :new [db _]
  (let [question (or (system-attr db :new/card-question) "")
        answer   (or (system-attr db :new/card-answer) "")]
    (mutation
     (fn [mutate]
       (q&a-base
        :question    question
        :question-cb (handle-change-cb :new/card-question)
        :answer      answer
        :answer-cb   (handle-change-cb :new/card-answer)
        :submit-text "Create Card"
        :submit-cb   (fn [_]
                       (if (and (seq question) (seq answer))
                           (mutate (str "mutation bar { createCard(deck_id: 1,question: \""
                                     question
                                     "\", answer: \""
                                     answer
                                     "\", user_id: \"1\") {id} }")
                                #(set-system-attrs! :new/card-question ""
                                                    :new/card-answer   "")))))))))

(defmethod q&a :edit [db _]
  (let [question (or (system-attr db :edit/card-question) "")
        answer   (or (system-attr db :edit/card-answer) "")]
    (q&a-base
     :question    question
     :question-cb (handle-change-cb :edit/card-question)
     :answer      answer
     :answer-cb   (handle-change-cb :edit/card-answer)
     :submit-text "Update Card"
     :submit-cb   #(set-system-attrs! :edit/card-question ""
                                      :edit/card-answer   ""))))

(defcs new-card [state db]
  (query
   "query getDecks { decks {id, name} }"
   (fn [data]
     (inspect (data "decks"))
     [:div
      (mui/card
       [:div.row
        [:div.col-xs-10.col-xs-offset-1.target-deck
         (mui/select-field {:value         (or (system-attr db :new-stuff/target-deck-id)
                                               (-> (data "decks") first (get "id")))
                            :onChange      #(set-system-attrs! :new-stuff/target-deck-id (.-id %3))
                            :valueMember   "id"
                            :displayMember "name"
                            :menuItems     (sort-by #(get % "name") (data "decks"))
                            })]]
       (q&a db :new))])))

(defn toggle-answer []
  (let [previous (system-attr @conn :show-answer?)]
    (set-system-attrs! :show-answer? (not previous))))

(defc study-card [db question answer subtitle & {:keys [prev next]}]
  [:div.row
   [:div.col-xs-12.col-sm-10.col-sm-offset-1.col-md-8.col-md-offset-2.col-lg-6.col-lg-offset-3
    (mui/card
     (mui/card-title {:title "Card" :subtitle subtitle})

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
          (mui/raised-button {:onClick toggle-answer :backgroundColor "#820000" :primary true :label "Wrong"})
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

     (if (system-attr db :show-answer?)
       (mui/card-text answer)))]])
