(ns loop-recall.study
  (:require-macros [loop-recall.macros :refer [inspect]]
                   [loop-recall.material :as mui])
  (:require [loop-recall.storage :as store :refer [conn set-system-attrs! system-attr]]
            [loop-recall.utility :refer [query]]
            [loop-recall.card :as card]
            [rum.core :as rum :refer-macros [defc defcs defcc] :include-macros true]))

(defn mod-shift [inc-dec i n]
  (set-system-attrs! :show-answer? false)
  (mod (+ inc-dec i) n))

(def next-index     (partial mod-shift 1))
(def previous-index (partial mod-shift -1))

(defcs page [state db]
  (let [cards                     (vec (store/due-cards))
        index                     (or (system-attr db :study/card-index) 0)
        total                     (count cards)]
    [:div.page
     (if (seq cards)
       (let [{:keys [deck question answer id deck-name remote-id]} (cards index)]
         (card/study-card db question answer id remote-id
                          deck-name
                          (str (inc index) " of " total)
                          :prev #(set-system-attrs! :study/card-index (previous-index index total))
                          :next #(set-system-attrs! :study/card-index (next-index index total)))))]))

