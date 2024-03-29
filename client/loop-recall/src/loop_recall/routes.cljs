(ns loop-recall.routes
  (:require
   [loop-recall.navbar :refer [navbar]]
   [loop-recall.storage :as store :refer [conn set-system-attrs! system-attr]]
   [loop-recall.theme :refer [color-theme]]
   [goog.events :as events]
   [goog.history.EventType :as EventType]
   [rum.core :as rum :refer-macros [defc defcs defcc] :include-macros true]
   [secretary.core :as secretary :refer-macros [defroute]])
  (:import goog.History))

(defroute "/" []
  (set-system-attrs! :page :study))
(defroute "/study" []
  (set-system-attrs! :page :study))
(defroute "/new" []
  (set-system-attrs! :page :new))
(defroute "/all_decks" []
  (set-system-attrs! :page :all-decks))
(defroute "/about" []
  (set-system-attrs! :page :about))
(defroute "/logout" []
  (set-system-attrs! :id-token nil)
  (.removeItem js/localStorage "userToken"))

;; History
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))
