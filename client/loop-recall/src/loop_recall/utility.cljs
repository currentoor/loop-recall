(ns loop-recall.utility
  (:require-macros [loop-recall.macros :refer [inspect]]
                   [loop-recall.material :as mui])
  (:import [goog Delay])
  (:require [ajax.core :refer [GET POST]]
            [rum.core :as rum :refer-macros [defc defcs defcc] :include-macros true]))

(defn debounce [f interval]
  (let [timeout (atom nil)]
    (fn [& args]
      (when-not (nil? @timeout)
        (.disposeInternal @timeout))
      (reset! timeout (Delay. #(apply f args)))
      (.start @timeout interval))))

(defn fetch [query cb]
  (GET (str js/window.apiRoot "graph_ql/query")
      {:params          {:query (js/encodeURIComponent query)}
       :headers         {"Authorization" (str "Bearer " (.getItem js/localStorage "userToken"))}
       :response-format :transit
       :handler         cb}))

(def ^:private query-mixin
  {:did-mount (fn [{[query] :rum/args :as state}]
                (GET (str js/window.apiRoot "graph_ql/query")
                    {:params          {:query (js/encodeURIComponent query)}
                     :headers         {"Authorization" (str "Bearer " (.getItem js/localStorage "userToken"))}
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
      (child data (:rum/local state)))))

(defn fetch-due-cards [local-insert]
  (fetch "query getDueCards { dueCards {id, question, answer, deck{id, name}} }"
             local-insert))

(defn fetch-decks [local-insert]
  (fetch "query getDecks { decks {id, name} }"
              local-insert))
