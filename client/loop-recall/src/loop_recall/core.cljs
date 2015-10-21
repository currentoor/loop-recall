(ns ^:figwheel-always loop-recall.core
    (:require-macros [loop-recall.macros :refer [inspect]]
                     [cljs.core.async.macros :as asyncm :refer (go go-loop)]
                     [loop-recall.material :as mui])
    (:require [loop-recall.navbar :refer [navbar]]
              [loop-recall.routes :refer [hook-browser-navigation!]]
              [loop-recall.storage :as store :refer [conn set-system-attrs! system-attr]]
              [loop-recall.study :as study]
              [loop-recall.theme :refer [color-theme]]
              [loop-recall.utility :refer [query mutation]]
              [rum.core :as rum :refer-macros [defc defcs defcc] :include-macros true]))

(enable-console-print!)

(defc new-card [db]
  (mui/paper
   {:zDepth 1}
   [:div.row
    [:div.col-xs-12.col-sm-6
     (mui/text-field {:hintText          "Who?"
                      :floatingLabelText "Question"})]
    [:div.col-xs-12.col-sm-6
     (mui/text-field {:hintText          "me?"
                      :floatingLabelText "Answer"})]

    [:div.col-xs-6
     (mui/raised-button {:onClick   #(set-system-attrs! :new/card-question ""
                                                        :new/card-answer "")
                         :secondary true
                         :label     "Create Card"})]
    ]))

(defc app < rum/reactive color-theme [conn]
  (let [db   (rum/react conn)
        page (inspect (system-attr db :page))]
    [:div
     (navbar)
     (new-card db)
     (study/page db)

     ;; (condp = page
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

