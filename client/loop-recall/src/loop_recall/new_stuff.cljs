(ns loop-recall.new-stuff
  (:require-macros [loop-recall.macros :refer [inspect]]
                   [loop-recall.material :as mui])
  (:require [loop-recall.storage :as store :refer [conn set-system-attrs! system-attr]]
            [loop-recall.utility :refer [query]]
            [loop-recall.card :as card]
            [rum.core :as rum :refer-macros [defc defcs defcc] :include-macros true]))

(defc page [db]
  [:div.page
   [:div.row
    [:div.col-xs-12.col-sm-10.col-sm-offset-1.col-md-6.col-md-offset-0.new-card-form
     (card/new-card db (store/all-decks))]
    [:div.col-xs-12.col-sm-10.col-sm-offset-1.col-md-6.col-md-offset-0
     (card/new-deck db)]]])

