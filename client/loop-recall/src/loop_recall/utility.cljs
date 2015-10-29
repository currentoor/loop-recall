(ns loop-recall.utility
  (:require-macros [loop-recall.macros :refer [inspect]]
                   [loop-recall.material :as mui])
  (:require [ajax.core :refer [GET POST]]
            [loop-recall.storage :as store :refer [conn set-system-attrs! system-attr]]
            [rum.core :as rum :refer-macros [defc defcs defcc] :include-macros true]))

(defn fetch [query cb]
  (GET "http://localhost:3000/graph_ql/query"
      {:params          {:query (js/encodeURIComponent query)}
       :response-format :transit
       :handler         cb}))

(def ^:private query-mixin
  {:did-mount (fn [{[query] :rum/args :as state}]
                (GET "http://localhost:3000/graph_ql/query"
                    {:params          {:query (js/encodeURIComponent query)}
                     :response-format :transit
                     :handler         (fn [resp]
                                        (swap! (:rum/local state)
                                               assoc :loading? false
                                               :data (resp "data")))})
                state)})

(def ^:private query-init-state
  {:loading? true
   :data     nil})

(defcs query < (rum/local query-init-state) query-mixin [state graph-ql child]
  (let [{:keys [loading? data]} @(:rum/local state)]
    (if loading?
      [:div.center
       (mui/circular-progress {:size 0.5})
       (mui/circular-progress {:size 0.5})
       (mui/circular-progress {:size 0.5})]
      (child data))))

(def ^:private mutation-mixin
  {:did-mount (fn [{[query] :rum/args :as state}]
                (GET "http://localhost:3000/graph_ql/query"
                    {:params          {:query (js/encodeURIComponent query)}
                     :response-format :transit
                     :handler         (fn [resp]
                                        (swap! (:rum/local state)
                                               assoc :loading? false
                                               :data (resp "data")))})
                state)})

(def ^:private mutation-init-state
  {:loading? false})

(defcs mutation < (rum/local mutation-init-state) [state child]
  (let [{:keys [loading? data]} @(:rum/local state)
        mutate (fn [graph-ql cb]
                 (swap! (:rum/local state) assoc :loading? true)
                 (POST "http://localhost:3000/graph_ql/mutation"
                     {:params          {:mutation graph-ql}
                      :response-format :transit
                      :handler         (fn [resp]
                                         (swap! (:rum/local state)
                                                assoc :loading? false)
                                         (cb resp))}))]
    [:div
     [:div.center {:style (if loading? {} {:visibility "hidden"})}
      (mui/circular-progress {:size 0.5})
      (mui/circular-progress {:size 0.5})
      (mui/circular-progress {:size 0.5})]
     (child mutate)]))

(defcs optimistic-mutation [state child]
  (let [mutate (fn [graph-ql cb]
                 (POST "http://localhost:3000/graph_ql/mutation"
                     {:params          {:mutation graph-ql}
                      :response-format :transit
                      :handler         (fn [resp]
                                         (cb resp))}))]
    (child mutate)))
