(ns loop-recall.utility
  (:require-macros [loop-recall.macros :refer [inspect]]
                   [loop-recall.material :as mui])
  (:require [ajax.core :refer [GET POST]]
            [loop-recall.storage :as store :refer [conn set-system-attrs! system-attr]]
            [rum.core :as rum :refer-macros [defc defcs defcc] :include-macros true]))

(def ^:private mixin
  {:did-mount (fn [{[query] :rum/args :as state}]
                (GET "http://localhost:3000/graph_ql/query"
                    {:params          {:query (js/encodeURIComponent query)}
                     :response-format :transit
                     :handler         (fn [resp]
                                        (swap! (:rum/local state)
                                               assoc :is-loading? false
                                               :data (resp "data")))})
                state)})

(def ^:private init-state
  {:is-loading? true
   :data        nil})

(defcs poor-mans-relay < (rum/local init-state) mixin [state query child]
  (let [{:keys [is-loading? data]} @(:rum/local state)]
    (if is-loading?
      [:div.center
       (mui/circular-progress {:mode "indeterminate"})]
      (child data))))
