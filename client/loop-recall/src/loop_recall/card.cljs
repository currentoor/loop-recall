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

   [:div.col-xs-3.col-sm-offset-9.create-card-btn
    (mui/raised-button {:onClick   submit-cb
                        :secondary true
                        :label     submit-text})]])

(defmulti q&a (fn [_ type] type))

(defmethod q&a :new [db _]
  (let [question (or (system-attr db :new/card-question) "")
        answer   (or (system-attr db :new/card-answer) "")]
    (q&a-base
     :question    question
     :question-cb (handle-change-cb :new/card-question)
     :answer      answer
     :answer-cb   (handle-change-cb :new/card-answer)
     :submit-text "Create Card"
     :submit-cb   #(set-system-attrs! :new/card-question ""
                                      :new/card-answer   ""))))

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

