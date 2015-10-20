(ns loop-recall.study
  (:require-macros [loop-recall.macros :refer [inspect]]
                   [loop-recall.material :as mui])
  (:require [loop-recall.storage :as store :refer [conn set-system-attrs! system-attr]]
            [loop-recall.utility :refer [poor-mans-relay]]
            [rum.core :as rum :refer-macros [defc defcs defcc] :include-macros true]))

(defn toggle-answer []
  (let [previous (system-attr @conn :show-answer?)]
    (set-system-attrs! :show-answer? (not previous))))

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

     (if (system-attr db :show-answer?)
       (mui/card-text answer)))]])

(defcs page [state db]
  (poor-mans-relay
   "query getCards { user(id: 1) { cards{id, question, answer} } }"
   (fn [data]
     (let [cards                     (get-in data ["user" "cards"])
           index                     (or (system-attr db :study/card-index) 0)
           total                     (count cards)
           {:strs [question answer]} (cards index)]
       (study-card db question answer (str (inc index) " of " total)
                   :prev #(set-system-attrs! :study/card-index (previous-index index total))
                   :next #(set-system-attrs! :study/card-index (next-index index total)))))))

