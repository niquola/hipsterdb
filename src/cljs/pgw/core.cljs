(ns pgw.core
  (:require [reagent.core :as reagent :refer [atom]]
            [garden.core :as css]
            [route-map.core :as rm]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:import goog.History))

(def style
  (css/css
   [:body {:font-size "18px"}]))

(defn $index []
  [:div#repl
   [:style style]
   [:h3 "Page"]
   [:a {:href "#/page"} "Page-1"]])

(defn $page []
  [:div#repl
   [:style style]
   [:h3 "Page"]
   [:a {:href "#/"} "Page"]])

(def routes
  {:GET  #'$index
   "page" {:GET #'$page}})

(defonce current-page (atom (:GET routes)))

(defn dispatch [event]
  (if-let [m (rm/match [:GET (.-token event)] routes)]
    (reset! current-page (:match m))
    (reset! current-page (fn [& args] [:h1 (str "Paget " (.-token event) " not found")]))))

(defn $current-page [] [:div [@current-page]])

(defn mount-root []
  (reagent/render [$current-page]
                  (.getElementById js/document "app")))
(defn init-navigation []
  (doto (History.)
    (events/listen EventType/NAVIGATE dispatch)
    (.setEnabled true)))

(defn init! []
  (init-navigation)
  (mount-root))
