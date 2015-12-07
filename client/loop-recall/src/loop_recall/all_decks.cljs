(ns loop-recall.all-decks
  (:require-macros [loop-recall.macros :refer [inspect]]
                   [loop-recall.material :as mui])
  (:require
   [loop-recall.cards.dynamic :as dyn]
   [loop-recall.card :as card]
   [loop-recall.storage :as store :refer [conn set-system-attrs! system-attr]]
   [loop-recall.utility :refer [query]]
   [loop-recall.theme :refer [markdown->html]]
   [rum.core :as rum :refer-macros [defc defcs defcc] :include-macros true]))

(defc card [db {:keys [question answer] :as data} atm]
  [:div.row
   [:div.col-xs-10.col-xs-offset-1
    (card/simple-card db data atm)]])

(defcs deck < (rum/local false) [state db local-id remote-id name]
  (let [expanded? (:rum/local state)
        this      (:rum/react-component state)]
    [:div.row
     [:div.col-xs-12.col-sm-10.col-sm-offset-1
      (mui/card
       [:div.row
        [:div.col-xs-9
         (mui/card-title {:title name})]
        [:div.col-xs-1
         (mui/icon-button {:iconClassName "material-icons"
                           :onClick       #(swap! expanded? not)}
                          (if @expanded? "expand_less" "expand_more"))]
        [:div.col-xs-1
         (mui/icon-button {:style         {:right "-12px"}
                           :onClick       #(or (swap! expanded? not)
                                                (.show (.. this -refs -deleteModal)))
                           :iconClassName "material-icons"}
                          "delete")]]
       (mui/dialog {:title   "Delete"
                    :actions [{:text "cancel"}
                              {:text       "submit"
                               :onTouchTap #(and (store/delete-deck local-id remote-id)
                                                 (.dismiss (.. this -refs -deleteModal)))
                               }]
                    :ref     "deleteModal"}
                   "Are you sure you want to delete this deck?")
       (if @expanded?
         (query
          (str "query getDeck { cardsFromDeck(deck_id: " remote-id ") { id, deck_id, question, answer }}")
          (fn [{cards "cardsFromDeck"} atm]
            [:div
             (for [c cards] (card db c atm))]))))]]))

(defc page [db]
  [:div.page
   (for [{:keys [id remote-id name]} (store/all-decks)]
     (deck db id remote-id name))])

