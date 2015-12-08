(ns ^:figwheel-always loop-recall.core
    (:require-macros [loop-recall.macros :refer [inspect]]
                     [cljs.core.async.macros :as asyncm :refer (go go-loop)]
                     [loop-recall.material :as mui])
    (:require [loop-recall.navbar :refer [navbar logged-out-navbar]]
              [loop-recall.routes :refer [hook-browser-navigation!]]
              [loop-recall.new-stuff :as new-stuff]
              [loop-recall.all-decks :as all-decks]
              [loop-recall.storage :as store :refer [conn set-system-attrs! system-attr]]
              [loop-recall.study :as study]
              [loop-recall.theme :refer [color-theme]]
              [loop-recall.utility :refer [query] :as util]
              [rum.core :as rum :refer-macros [defc defcs defcc] :include-macros true]))

(enable-console-print!)

(defc home-page []
  [:div.page
   [:div.row
    [:div.col-xs-12.col-sm-10.col-sm-offset-1.col-md-8.col-md-offset-2
     (mui/card
      (mui/card-title {:title "Spaced Repetition Meets Machine Learning"})
      [:div.row
       [:div.col-xs-12.col-sm-10.col-sm-offset-1.col-md-8.col-md-offset-2
        (mui/card-text
         "LoopRecall is an app designed to make the learning process more efficient.
          We use machine learning algorithms to implement custom intelligently spaced
          repetition plans. Given a collection of knowledge you would like review and
          retain,consider the diagram below."
         [:ul
          [:li "Create flash cards based on the collection of knowledge."]
          [:li "Put the cards in the first box on the left."]
          [:li "Review each card according to the label on the box."]
          [:li "Based on your response move the card forwards or backwards based on arrows."]
          [:li "Repeat."]]
         "That is basically it, except the boxes are implemented in software and the labels
          (i.e. gap between reviews) on the boxes are custom generated based on your learning
          history.")
        [:img {:src "http://loop-recall-assets.s3-us-west-1.amazonaws.com/images/visual.svg"
               :width "100%" :height "100%"
               :alt "Spaced Repetition"}]
        (mui/card-text
         "If you like this app and feel charitable, "
         [:a {:href "https://www.khanacademy.org/donate" :target "_blank"} "here"]
         " is a great cause that could use more funding.")
        (mui/card-text "- Karan")]])]]])

(defcs home <
  {:did-mount (fn [{[lock] :rum/args :as state}]
                (.show lock)
                state)}
  [state lock]
  [:div
   (logged-out-navbar #(.show lock))
   [:div.login-box]
   (home-page)])

(defcs logged-in [state db lock conn]
  (let [page (system-attr db :page)]
    [:div
     (navbar)

     (case page
       :new       (new-stuff/page db)
       :about     (home-page)
       :all-decks (all-decks/page db)
       (study/page db))]))

(defn get-id-token [lock]
  (let [prev-id-token (js/localStorage.getItem "userToken")
        auth-hash     (.parseHash lock js/window.location.hash)]
    (if-not prev-id-token (set-system-attrs! :page :about)) ; Show about page only on login.
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
(defonce due-cards-fetch (util/fetch-due-cards store/insert-due-cards))
(defonce decks-fetch (util/fetch-decks store/insert-decks))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

