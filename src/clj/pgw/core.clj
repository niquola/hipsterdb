(ns pgw.core
    (:require
     [pgw.middleware :refer [wrap-middleware]]

     [clojure.tools.logging :as log]
     [garden.core :as css]
     [garden.units :as u]
     [hiccup.core :as html]
     [org.httpkit.server :as http]
     [ring.middleware.defaults :as rmd]
     [ring.middleware.resource :as rmr]
     [clojure.tools.logging :as log]
     [route-map.core :as route])
    (:gen-class))

(defn css [grd] [:style (css/css grd)])

(defn js [s] [:script {:type "text/javascript" :src s}])

(defn layout [cnt] 
  (html/html
   [:html
    [:head
     [:title "pgw"]
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     (css [:body {:padding (u/px 20)}])]
    [:body cnt
     (js "/js/app.js")]]))

(defn $index [req]
  {:body (layout [:div#app "Loading..."])
   :headers {"Content-Type" "text/html"}
   :status 200})

(defn $api [req]
  {:body "test"
   :headers {"Content-Type" "text/html"}
   :status 200})

(def routes
  {:GET #'$index
   "api" {:GET #'$api}})

(defn dispatch [{uri :uri meth :request-method :as req}]
  (if-let [r (route/match [meth uri] routes)]
    ((:match r) req)
    {:body (str "Page " uri " not found")
     :status 404}))

(def app (-> dispatch
             (rmd/wrap-defaults rmd/site-defaults)
             (rmr/wrap-resource "public")))

(defonce stop (atom nil))

(defn start []
  (log/info "Start server on " 8080)
  (reset! stop
          (http/run-server #'app {:port 8080})))

(defn -main [& args]
  (start))

(comment
  (@stop)
  (start))
