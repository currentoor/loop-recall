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

(defc card [db {:keys [question answer] :as data}]
  [:div.row
   [:div.col-xs-10.col-xs-offset-1
    (card/simple-card db data)]])

(defcs deck < (rum/local false) [state db deck-id name]
  (let [expanded? (:rum/local state)]
    [:div.row
     [:div.col-xs-12.col-sm-10.col-sm-offset-1
      (mui/card
       [:div.row
        [:div.col-xs-10
         (mui/card-title {:title name})]
        [:div.col-xs-2.right
         (mui/icon-button {:iconClassName "material-icons"
                           :onClick       #(swap! expanded? not)}
                          (if @expanded? "expand_less" "expand_more"))]]
       (if @expanded?
         (query
          (str "query getDeck { cardsFromDeck(deck_id: " deck-id ") { deck_id, question, answer }}")
          (fn [{cards "cardsFromDeck"}]
            (inspect cards)
            [:div
             (for [c cards] (card db c))]))))]]))

(defc page [db]
  [:div.page
   (for [{:keys [remote-id name]} (store/all-decks)]
     (deck db remote-id name))])

