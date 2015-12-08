(ns loop-recall.about
  (:require-macros [loop-recall.macros :refer [inspect]]
                   [loop-recall.material :as mui])
  (:require
   [rum.core :as rum :refer-macros [defc]]))

(defc page []
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
