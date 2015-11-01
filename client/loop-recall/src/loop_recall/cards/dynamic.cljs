(ns loop-recall.cards.dynamic
  (:require-macros [loop-recall.macros :refer [inspect]]
                   [loop-recall.material :as mui])
  (:import [goog Delay])
  (:require [loop-recall.storage :as store :refer [conn set-system-attrs! system-attr]]
            [loop-recall.theme :refer [markdown->html]]
            [rum.core :as rum :refer-macros [defc defcs defcc] :include-macros true]))

;;;; These components do not conform to functional declarative programming norms.
;;;; I made this compromise to hack around inconsistancies with requestAnimationFrame
;;;; and material-ui component updates.

(def macro-state
  (atom {:question ""
         :answer   ""}))

(defcc input-wrapper < rum/reactive [comp hint label ref]
  (mui/text-field {:hintText          hint
                   :multiLine         true
                   :fullWidth         true
                   :rows              2
                   :value             (rum/react ref)
                   :onChange          (fn [e]
                                        (reset! ref (.. e -target -value))
                                        (.forceUpdate comp))
                   :floatingLabelText label}))

(defc q&a <
  {:will-mount (fn [state]
                 (swap! macro-state
                        assoc :question (-> state :rum/args first))
                 (swap! macro-state
                        assoc :answer (-> state :rum/args second))
                 state)}
  [question answer]
  [:div.row
   [:div.col-xs-12.col-sm-10.col-sm-offset-1
    (input-wrapper
     "Ex: How do you remove all files from staging?"
     "Question"
     (rum/cursor macro-state [:question]))]
   [:div.col-xs-12.col-sm-10.col-sm-offset-1
    (input-wrapper
     "Ex: Ex: git reset HEAD -- ."
     "Answer"
     (rum/cursor macro-state [:answer]))]])

;; (defmethod q&a :new [db _]
;;   (let [question (or (system-attr db :new/card-question) "")
;;         answer   (or (system-attr db :new/card-answer) "")]
;;     (q&a-base
;;      :question    question
;;      :question-cb (handle-change-cb :new/card-question)
;;      :answer      answer
;;      :answer-cb   (handle-change-cb :new/card-answer))
;;     [:div.col-xs-6.col-xs-offset-6.create-card-btn.center
;;      (mui/raised-button {:onClick   #(and (store/create-card
;;                                            :deck-id  (or (system-attr db :new-stuff/target-deck-id)
;;                                                          (-> (store/all-decks) first :id))
;;                                            :question (system-attr db :new/card-question)
;;                                            :answer   (system-attr db :new/card-answer))
;;                                           (set-system-attrs! :new/card-question ""
;;                                                              :new/card-answer ""))
;;                          :secondary true
;;                          :label     "Create Card"})]))

;; (defcs new-card [state db]
;;   (query
;;    "query getDecks { decks {id, name} }"
;;    (fn [data]
;;      [:div
;;       (mui/card
;;        [:div.row
;;         [:div.col-xs-10.col-xs-offset-1.target-deck
;;          (mui/select-field {:value         (or (system-attr db :new-stuff/target-deck-id)
;;                                                (-> (data "decks") first (get "id")))
;;                             :onChange      #(set-system-attrs! :new-stuff/target-deck-id (.-id %3))
;;                             :valueMember   "id"
;;                             :displayMember "name"
;;                             :menuItems     (sort-by #(get % "name") (data "decks"))
;;                             })]]
;;        (q&a db :new))]))
;;   )
