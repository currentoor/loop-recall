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

(defn handle-change-cb [key]
  (fn [e]
    (set-system-attrs! key (.. e -target -value))))

(defmulti q&a (fn [_ type] type))

(defmethod q&a :new [db _]
  (let [question (or (system-attr db :new/card-question) "")
        answer   (or (system-attr db :new/card-answer) "")]
    [:div.row
     [:div.col-xs-12.col-sm-10.col-sm-offset-1
      (mui/text-field {:hintText          "Ex: Ex: How do you remove all files from staging?"
                       :multiLine         true
                       :fullWidth         true
                       :rows              2
                       :onChange          (handle-change-cb :new/card-question)
                       :value             question
                       :floatingLabelText "Question"})]
     [:div.col-xs-12.col-sm-10.col-sm-offset-1
      (mui/text-field {:hintText          "Ex: Ex: git reset HEAD -- ."
                       :multiLine         true
                       :fullWidth         true
                       :rows              2
                       :value             answer
                       :onChange          (handle-change-cb :new/card-answer)
                       :floatingLabelText "Answer"})]

     [:div.col-xs-3.col-sm-offset-9.create-card-btn
      (mui/raised-button {:onClick   #(set-system-attrs! :new/card-question ""
                                                         :new/card-answer   "")
                          :secondary true
                          :label     "Create Card"})]
     ]
    ))

(defmethod q&a :edit [db _]
  (let [question (or (system-attr db :new/card-question) "")
        answer   (or (system-attr db :new/card-answer) "")]
    [:div
     ]))

(defc new-card [db]
  [:div.row.new-card-form
   [:div.col-xs-12.col-sm-10.col-sm-offset-1.col-md-8.col-md-offset-2.col-lg-6.col-lg-offset-3
    (mui/card
     (q&a db :new)
     )
    ]])

(defc app < rum/reactive color-theme [conn]
  (let [db   (rum/react conn)
        page (inspect (system-attr db :page))]
    [:div
     (navbar)
     (new-card db)
     ;(study/page db)

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

