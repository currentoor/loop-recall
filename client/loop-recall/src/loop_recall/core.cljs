(ns ^:figwheel-always loop-recall.core
    (:require-macros [loop-recall.macros :refer [inspect]]
                     [cljs.core.async.macros :as asyncm :refer (go go-loop)]
                     [loop-recall.material :as mui])
    (:require [loop-recall.navbar :refer [navbar]]
              [loop-recall.routes :refer [hook-browser-navigation!]]
              [loop-recall.new-stuff :as new-stuff]
              [loop-recall.all-decks :as all-decks]
              [loop-recall.storage :as store :refer [conn set-system-attrs! system-attr]]
              [loop-recall.study :as study]
              [loop-recall.theme :refer [color-theme]]
              [loop-recall.utility :refer [query] :as util]
              [rum.core :as rum :refer-macros [defc defcs defcc] :include-macros true]))

(enable-console-print!)

(defc home-page [db]
  [:div.row
   [:div.col-xs-10.col-xs-offset-1
    [:h2 "Spaced Repetition Meets Machine Learning"]]
   [:div.col-xs-12.col-sm-10.col-sm-offset-1
    [:img {:src "http://loop-recall-assets.s3-us-west-1.amazonaws.com/images/visual.svg"
           :width "100%" :height "100%"
           :alt "Spaced Repetition"}]]])

(defcs home <
  {:did-mount (fn [{[lock] :rum/args :as state}]
                (.show lock)
                state)}
  [state lock]
  [:div.login-box
   [:button {:on-click #(.show lock)} "signin"]])

(defcs logged-in [state db lock conn]
  (let [page (system-attr db :page)]
    [:div
     (navbar)

     (condp = page
       :new       (new-stuff/page db)
       :study     (study/page db)
       :all-decks (all-decks/page db)
       (home-page db))]))

(defn get-id-token [lock]
  (let [prev-id-token (js/localStorage.getItem "userToken")
        auth-hash     (.parseHash lock js/window.location.hash)]
    (if (and (not prev-id-token) auth-hash)
      (do
        ;; Set localStorage if auth-hash has a token.
        (if-let [id-token (.-id_token auth-hash)]
          (.setItem js/localStorage "userToken" id-token))
        (if (.-error auth-hash)
          (do
            (.removeItem js/localStorage "userToken")
            (js/console.log "Error signing in" auth-hash)))))
    (if prev-id-token
      prev-id-token
      (.getItem js/localStorage "userToken"))))

(def auth0
  {:will-mount (fn [state]
                 (let [lock (js/Auth0Lock. "HpjUc70r2FEMpZl8Mj4ziGHpDIG0AeU5", "looprecall.auth0.com")]
                   (set-system-attrs! :lock lock :id-token (get-id-token lock))
                   state))
   :did-mount  (fn [state]
                 (let [[lock id-token] (system-attr @conn :lock :id-token)]
                   (.getProfile
                   lock id-token
                   (fn [err profile]
                     (if err
                       (js/console.log "Error loading profile", err))
                     (set-system-attrs! :profile profile))))
                 state)})

(defcs app < rum/reactive color-theme auth0
  [state]
  (let [db              (rum/react conn)
        [lock id-token] (system-attr @conn :lock :id-token)]
    (if id-token
      (logged-in db lock conn)
      (home lock))))

(rum/mount (app) (js/document.getElementById "app"))

(defonce history
  (hook-browser-navigation!))

;;; Load initial data.
(defonce due-cards-fetch (util/fetch-due-cards))
(defonce decks-fetch (util/fetch-decks))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

