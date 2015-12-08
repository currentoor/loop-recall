(ns loop-recall.navbar
  (:require-macros [loop-recall.macros :refer [inspect]]
                   [loop-recall.material :as mui])
  (:require
   [loop-recall.storage :as store :refer [conn set-system-attrs! system-attr]]
   [rum.core :as rum :refer-macros [defc defcs defcc] :include-macros true]))

(defcs navbar < (rum/local false) [state]
  (let [show-drop-down? (:rum/local state)
        this            (:rum/react-component state)
        menu-items      [{ :route "#/study" :text "Study" }
                         { :route "#/new" :text "New" }
                         { :route "#/all_decks" :text "All" }
                         { :type js/window.MaterialUI.MenuItem.Types.SUBHEADER :text "Account" }
                         { :route "#/about" :text "About" }
                         { :route "#/logout" :text "Logout" }]]
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

(defcs logged-out-navbar < (rum/local false) [state show-login-modal]
  (let [show-drop-down? (:rum/local state)
        this            (:rum/react-component state)
        menu-items      [{ :route "#/about" :text "About" }
                         { :route "#/login" :text "Login/Signup" }]]
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
                    (aset js/window.location "href" (.-route menu-item))
                    (if (= "#/login" (.-route menu-item))
                      (show-login-modal)))})]))
