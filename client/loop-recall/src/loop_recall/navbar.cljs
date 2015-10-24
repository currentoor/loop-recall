(ns loop-recall.navbar
  (:require-macros [loop-recall.macros :refer [inspect]]
                   [loop-recall.material :as mui])
  (:require
   [loop-recall.storage :as store :refer [conn set-system-attrs! system-attr]]
   [rum.core :as rum :refer-macros [defc defcs defcc] :include-macros true]))

(defcs navbar < (rum/local false) [state]
  (let [show-drop-down? (:rum/local state)
        this            (:rum/react-component state)
        menu-items      [{ :route "#/" :text "Home" }
                         { :route "#/study" :text "Study" }
                         { :route "#/new" :text "Create New" }
                         { :route "#/all_cards" :text "All Cards" }
                         { :type js/window.MaterialUI.MenuItem.Types.SUBHEADER :text "Account" }
                         { :route "#/settings" :text "Settings" }
                         ]]
    [:div
     (mui/app-bar
      {:title                    "LoopRecall"
       :onLeftIconButtonTouchTap (fn []
                                   (.toggle (.. this -refs -leftNav)))})
     (mui/left-nav
      {:menuItems menu-items
       :docked    false
       :ref       "leftNav"
       :onChange  (fn [_ selected-idx menu-item]
                    (aset js/window.location "href" (.-route menu-item)))})]))
