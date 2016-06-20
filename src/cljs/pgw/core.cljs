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

(def state (atom {}))

(defn $index []
  [:div#repl
   [:style style]
   [:a {:href "#/graphql"} "GraphQL"]
   [:br]
   [:a {:href "#/swagger"} "REST (Swagger)"]
   [:br]
   [:a {:href "#/honeysql"} "HoneySQL"]])

(defn bind [k]
  (fn [ev]
    (swap! state assoc k (.. ev -target -value))))

(defn $graphql []
  [:div#repl
   [:style style]
   [:a {:href "#/"} "Home"]
   [:br]
   [:textarea {:on-change (bind :input) :value (:input @state)}]
   [:pre (pr-str @state)]
   ])

(def routes
  {:GET  #'$index
   "graphql" {:GET #'$graphql}})

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
