(ns ^:figwheel-always loop-recall.core
    (:require-macros [loop-recall.macros :refer [inspect]]
                     [cljs.core.async.macros :as asyncm :refer (go go-loop)]
                     [loop-recall.material :as mui])
    (:require [ajax.core :refer [GET POST]]
              [loop-recall.navbar :refer [navbar]]
              [loop-recall.routes :refer [hook-browser-navigation!]]
              [loop-recall.storage :as store :refer [conn set-system-attrs! system-attr]]
              [loop-recall.theme :refer [color-theme]]
              [rum.core :as rum :refer-macros [defc defcs defcc] :include-macros true]))

(enable-console-print!)

(defn toggle-answer []
  (let [previous (system-attr @conn :show-answer?)]
    (set-system-attrs! :show-answer? (not previous))))

(def garib-relay-mixin
  {:did-mount (fn [{[query] :rum/args :as state}]
                (GET "http://localhost:3000/graph_ql/query"
                    {:params          {:query (js/encodeURIComponent query)}
                     :response-format :transit
                     :handler         (fn [resp]
                                        (swap! (:rum/local state)
                                               assoc :is-loading? false
                                               :data (resp "data")))})
                state)})

(defcs garib-relay < (rum/local {:is-loading? true
                                 :data        nil})
                     garib-relay-mixin
  [state query params children]
  (let [{:keys [is-loading? data]} @(:rum/local state)]
    (if is-loading?
      [:div.center
       (mui/circular-progress {:mode "indeterminate"})]
      (children data))))

(defn mod-shift [inc-dec i n]
  (set-system-attrs! :show-answer? false)
  (mod (+ inc-dec i) n))

(def next-index     (partial mod-shift 1))
(def previous-index (partial mod-shift -1))

(defc study-card [db question answer subtitle & {:keys [prev next]}]
  [:div.row
   [:div.col-xs-12.col-sm-10.col-sm-offset-1
    (mui/card
     (mui/card-title {:title "Card" :subtitle subtitle})

     (mui/card-text question)

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
          (mui/raised-button {:onClick toggle-answer :backgroundColor "#820000" :primary true :label "Wrong"})
          (mui/raised-button {:onClick toggle-answer :label "Almost"})
          (mui/raised-button {:onClick toggle-answer :secondary true :label "Correct"}))
         (mui/card-actions
          (mui/flat-button {:onClick toggle-answer :label "Show Answer"})))]

      [:div.col-xs-2.center
       (mui/icon-button
        {:iconClassName   "material-icons"
         :tooltipPosition "top-left"
         :tooltip         "Next Card"
         :onClick         next}
        "arrow_forward")]]

     (mui/card-text answer))]])

(defcs study-deck [state db]
  (garib-relay
   "query getCards { user(id: 1) { cards{id, question, answer} } }" ""
   (fn [data]
     (let [cards                     (get-in data ["user" "cards"])
           index                     (or (system-attr db :study/card-index) 0)
           total                     (count cards)
           {:strs [question answer]} (cards index)]
       (study-card db question answer (str (inc index) " of " total)
                   :prev #(set-system-attrs! :study/card-index (previous-index index total))
                   :next #(set-system-attrs! :study/card-index (next-index index total)))))))

(defc app < rum/reactive color-theme [conn]
  (let [db   (rum/react conn)
        page (system-attr db :page)]
    [:div
     (navbar)
     (study-deck db)
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

