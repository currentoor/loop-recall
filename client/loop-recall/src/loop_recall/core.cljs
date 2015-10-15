(ns ^:figwheel-always loop-recall.core
    (:require-macros [loop-recall.macros :refer [inspect]]
                     [cljs.core.async.macros :as asyncm :refer (go go-loop)]
                     [loop-recall.material :as mui])
    (:require [loop-recall.navbar :refer [navbar]]
              [loop-recall.routes :refer [hook-browser-navigation!]]
              [loop-recall.storage :as store :refer [conn set-system-attrs! system-attr]]
              [loop-recall.theme :refer [color-theme]]
              [rum.core :as rum :refer-macros [defc defcs defcc] :include-macros true]))

(enable-console-print!)

(defn toggle-answer []
  (let [previous (system-attr @conn :show-answer?)]
    (set-system-attrs! :show-answer? (not previous))))

(defc study-card [db]
  [:div.row
   [:div.col-md-6.col-md-offset-3
    (mui/card
     (mui/card-title {:title "Card" :subtitle "1 out of 1"})

     (mui/card-text "Who framed Roger Rabbit?")

     [:div.row
      [:div.col-md-2.center
       (mui/icon-button
        {:iconClassName   "material-icons"
         :tooltipPosition "top-right"
         :tooltip         "Previous Card"
         :onClick          toggle-answer}
        "arrow_back")]

      [:div.col-md-8.center
       (if (system-attr db :show-answer?)
         (mui/card-actions
          (mui/raised-button {:onClick toggle-answer :backgroundColor "#820000" :primary true :label "Wrong"})
          (mui/raised-button {:onClick toggle-answer :label "Almost"})
          (mui/raised-button {:onClick toggle-answer :secondary true :label "Correct"}))
         (mui/card-actions
          (mui/flat-button {:onClick toggle-answer :label "Show Answer"})))]

      [:div.col-md-2.center
       (mui/icon-button
        {:iconClassName   "material-icons"
         :tooltipPosition "top-left"
         :tooltip         "Next Card"
         :onClick         toggle-answer}
        "arrow_forward")]
      ]

     (mui/card-text "No idea!"))]])

(defc app < rum/reactive color-theme [conn]
  (let [db   (rum/react conn)
        page (system-attr db :page)]
    [:div
     (navbar)
     (study-card db)
     ;; (condp = page
     ;;   (global-dash db)
     ;;   )
     ]))

(rum/mount (app conn) (js/document.getElementById "app"))

(defonce history
  (hook-browser-navigation!))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

