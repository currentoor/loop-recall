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

(defn moded [idx total]
  (mod idx total))

(defcs page [state db]
  (let [cards  (vec (store/due-cards))
        index* (or (system-attr db :study/card-index) 0)
        total  (count cards)
        index  (moded index* total)]
    [:div.page
     (if-not (system-attr db :due-cards-loaded?)
       [:div.center
        (mui/circular-progress)
        (mui/circular-progress)
        (mui/circular-progress)]
       (if (seq cards)
         (let [{:keys [deck question answer id deck-name remote-id correct-interval]} (cards index)]
           (card/study-card db question answer correct-interval id remote-id
                            deck-name
                            (str (inc index) " of " total)
                            :prev #(set-system-attrs! :study/card-index (previous-index index total))
                            :next #(set-system-attrs! :study/card-index (next-index index total))))

         [:div.row
          [:div.col-xs-12.col-sm-10.col-sm-offset-1.col-md-8.col-md-offset-2.col-lg-6.col-lg-offset-3
           (mui/card-title "Congrats! You are finished studying for today.")]]
         ))]))
