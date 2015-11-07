(ns ^:figwheel-always loop-recall.core
    (:require-macros [loop-recall.macros :refer [inspect]]
                     [cljs.core.async.macros :as asyncm :refer (go go-loop)]
                     [loop-recall.material :as mui])
    (:require [loop-recall.navbar :refer [navbar]]
              [loop-recall.routes :refer [hook-browser-navigation!]]
              [loop-recall.new-stuff :as new-stuff]
              [loop-recall.all-decks :as all-decks]
              [loop-recall.storage :as store :refer [conn set-system-attrs! system-attr]]
              [loop-recall.study :as study]
              [loop-recall.theme :refer [color-theme]]
              [loop-recall.utility :refer [query] :as util]
              [rum.core :as rum :refer-macros [defc defcs defcc] :include-macros true]))

(enable-console-print!)

(defc home-page [db]
  [:div.row
   [:div.col-xs-10.col-xs-offset-1
    [:h2 "Welcome to LoopRecall!"]]
   [:div.col-xs-12.col-sm-10.col-sm-offset-1
    [:img {:src "http://loop-recall-assets.s3-us-west-1.amazonaws.com/images/visual.svg"
           :width "100%" :height "100%"
           :alt "Spaced Repetition"}]]])

(defc app < rum/reactive color-theme [conn]
  (let [db   (rum/react conn)
        page (system-attr db :page)]
    [:div
     (navbar)

     (condp = page
       :new       (new-stuff/page db)
       :study     (study/page db)
       :all-decks (all-decks/page db)
       (home-page db))]))

(rum/mount (app conn) (js/document.getElementById "app"))

(defonce history
  (hook-browser-navigation!))

;;; Load initial data.
(defonce due-cards-fetch (util/fetch "query getDueCards { dueCards {id, question, answer, deck{id, name}} }"
                                     store/insert-due-cards))
(defonce decks-fetch (util/fetch "query getDecks { decks {id, name} }"
                                 store/insert-decks))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

