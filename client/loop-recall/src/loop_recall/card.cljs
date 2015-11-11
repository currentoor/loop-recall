(ns loop-recall.card
  (:require-macros [loop-recall.macros :refer [inspect]]
                   [loop-recall.material :as mui])
  (:require
   [loop-recall.cards.dynamic :as dyn]
   [loop-recall.storage :as store :refer [conn set-system-attrs! system-attr]]
   [loop-recall.utility :refer [query]]
   [loop-recall.theme :refer [markdown->html]]
   [rum.core :as rum :refer-macros [defc defcs defcc] :include-macros true]))

(defc new-card [db decks]
  [:div.row
   [:div.col-xs-12.col-sm-10.col-sm-offset-1.col-md-10.col-md-offset-1
    (mui/card
     (mui/card-title {:title "New Card"})
     [:div.row
      [:div.col-xs-3.col-xs-offset-1
       [:span "Target Deck"]]
      [:div.col-xs-6
       (mui/select-field {:value         (or (system-attr db :new/target-deck-remote-id)
                                             (-> decks first :remote-id))
                          :onChange      #(and (set-system-attrs! :new/target-deck-remote-id (aget %3 "remote-id")
                                                                  :new/target-deck-name      (aget %3 "name")))
                          :valueMember   "remote-id"
                          :displayMember "name"
                          :menuItems     decks})]]
     [:div.row
      [:div.col-xs-12
       (dyn/q&a "" "")]]
     [:div.row
      [:div.col-xs-6.center
       (mui/raised-button
        {:onClick #(inspect 'todo) :label "Preview"})]
      [:div.col-xs-6.center
       (mui/raised-button
        {:secondary true
         :onClick #(and (store/create-card
                         :deck-name (or (system-attr db :new/target-deck-name)
                                        (-> decks first :name))
                          :remote-deck-id (or (system-attr db :new/target-deck-remote-id)
                                              (-> decks first :remote-id))
                          :question (:question @dyn/macro-state)
                          :answer (:answer @dyn/macro-state))
                        (dyn/reset-state!))
         :label "Create"})]])]])

(defcc new-deck-input < rum/reactive [comp ref]
  (mui/text-field {:hintText          "Ex: Git Commands"
                   :fullWidth         true
                   :value             (rum/react ref)
                   :onChange          (fn [e]
                                        (reset! ref (.. e -target -value))
                                        (.forceUpdate comp))
                   :floatingLabelText "Name"}))


(def deck-name (atom {:text ""}))

(defc new-deck [db]
  [:div.row
   [:div.col-xs-12.col-sm-10.col-sm-offset-1.col-md-10.col-md-offset-1
    (mui/card
     (mui/card-title {:title "New Deck"})
     [:div.row
      [:div.col-xs-12.col-sm-10.col-sm-offset-1
       (new-deck-input
        (rum/cursor deck-name [:text]))]]
     [:div.row
      [:div.col-xs-6.center.col-xs-offset-6
       (mui/raised-button
        {:secondary true
         :onClick #(and (store/create-deck (:text @deck-name))
                        (reset! deck-name {:text ""}))
         :label "Create"})]])]])

(defn card-menu-options [& {:keys [on-edit on-delete]}]
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
                                   "delete")})
   (mui/list-divider)))

(defcs simple-header < (rum/local false) [state db remote-id question answer atm]
  (let [this            (:rum/react-component state)
        show-card-menu? (:rum/local state)]
    [:div
     [:div.row
      [:div.col-xs-10]
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
                                            :onTouchTap #(and
                                                          (store/simple-update-card
                                                           atm
                                                           :question (:question @dyn/macro-state)
                                                           :answer (:answer @dyn/macro-state)
                                                           :remote-id remote-id)
                                                          (.dismiss (.. this -refs -editModal)))}]
                  :autoDetectWindowHeight true
                  :modal                  true
                  :autoScrollBodyContent  true
                  :ref                    "editModal"}
                 [:div {:style {:min-height "500px"}}
                  (dyn/q&a (store/unescape question) (store/unescape answer))])

     (mui/dialog {:title   "Delete"
                  :actions [{:text "cancel"}
                            {:text       "submit"
                             :onTouchTap #(and (store/simple-delete-card atm remote-id)
                                               (.dismiss (.. this -refs -deleteModal)))}]
                  :ref     "deleteModal"}
                 "Are you sure you want to delete this card?")]))

(defc simple-card [db {:strs [id question answer] :as data} atm]
  (mui/card
   (simple-header db id question answer atm)
   [:div
    (mui/card-text
     [:div {:dangerouslySetInnerHTML {:__html (markdown->html (store/unescape question))}}])]
   [:div
    (mui/card-text
     [:div {:dangerouslySetInnerHTML {:__html (markdown->html (store/unescape answer))}}])]))

(defcs study-header < (rum/local false) [state db title subtitle card-id remote-id question answer atm]
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
                                            :onTouchTap #(and
                                                          (store/update-card
                                                           card-id
                                                           :question (:question @dyn/macro-state)
                                                           :answer (:answer @dyn/macro-state)
                                                           :remote-id remote-id)
                                                          (.dismiss (.. this -refs -editModal)))}]
                  :autoDetectWindowHeight true
                  :modal                  true
                  :autoScrollBodyContent  true
                  :ref                    "editModal"}
                 [:div {:style {:min-height "500px"}}
                  (dyn/q&a question answer)])

     (mui/dialog {:title   "Delete"
                  :actions [{:text "cancel"}
                            {:text       "submit"
                             :onTouchTap #(and (store/delete-card card-id remote-id)
                                               (.dismiss (.. this -refs -deleteModal)))}]
                  :ref     "deleteModal"}
                 "Are you sure you want to delete this card?")]))

(defn toggle-answer []
  (let [previous (system-attr @conn :show-answer?)]
    (set-system-attrs! :show-answer? (not previous))))

(defcs study-card [state db question answer card-id remote-id deckname subtitle & {:keys [prev next]}]
  [:div.row
   [:div.col-xs-12.col-sm-10.col-sm-offset-1.col-md-8.col-md-offset-2.col-lg-6.col-lg-offset-3
    (mui/card
     (study-header db deckname
                   subtitle
                   card-id
                   remote-id
                   question
                   answer)

     [:div.study-question
      (mui/card-text
       [:div {:dangerouslySetInnerHTML {:__html (markdown->html question)}}])]

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
                                             (store/answer-card
                                              card-id
                                              :remote-id remote-id
                                              :response  "0"))
                              :backgroundColor "#820000" :primary true :label "Wrong"})
          (mui/raised-button {:onClick #(and (toggle-answer)
                                             (store/answer-card
                                              card-id
                                              :remote-id remote-id
                                              :response  "3"))
                              :label "Almost"})
          (mui/raised-button {:onClick #(and (toggle-answer)
                                             (store/answer-card
                                              card-id
                                              :remote-id remote-id
                                              :response  "5"))
                              :secondary true :label "Correct"}))
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
        (mui/card-text
         [:div {:dangerouslySetInnerHTML {:__html (markdown->html answer)}}]))])]])
