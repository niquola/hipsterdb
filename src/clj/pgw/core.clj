(ns pgw.core
  (:require
   [clojure.tools.logging :as log]
   [garden.core :as css]
   [pgw.db :as db]
   [garden.units :as u]
   [hiccup.core :as html]
   [org.httpkit.server :as http]
   [ring.middleware.defaults :as rmd]
   [ring.middleware.resource :as rmr]
   [graphql.core :as gq]
   [clojure.tools.logging :as log]
   [swagger.core :as swag]
   [swagger.routes :as swagr]
   [pg.core :as pg]
   [ring.util.codec :as codec]
   [pgw.formats :as fmt]
   [route-map.core :as route]
   [clojure.string :as str])
  (:gen-class))

(defn css [grd] [:style (css/css grd)])

(defn js [s] [:script {:type "text/javascript" :src s}])

(defn layout [cnt] 
  (html/html
   [:html
    [:head
     [:title "{{name}}"]
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     (css [:body {:padding (u/px 20)}])]
    [:body cnt
     (js "/js/app.js")]]))

(defn wrap-format [h]
  (fn [req]
    (let [fmt (or (get-in req [:params  :_format])
                  (get-in req [:headers "Content-Type"])
                  "json")
          res (h req)]
      (if-not (string? (:body res))
        (-> res
            (update-in [:body] (fn [o] (fmt/to fmt o)))
            (update-in [:headers "Content-Type"] (fn [f] (or f fmt))))
        res))))

(defn wrap-exception [h]
  (fn [req]
    (try (h req)
         (catch Exception e
           {:headers {"Content-Type" "text"}
            :body (pr-str e)
            :status 500}))))

(defn wrap-db [h]
  (fn [{{db :db} :params :as req}]
    (if db
      (db/with-db db (h req))
      (h req))))

(defn $index [req]
  {:body    (layout [:div#app "Loading..."])
   :headers {"Content-Type" "text/html"}
   :status 200})


(def routes
  (let [r {:GET #'$index
           "db" {:GET  #'pg/dbs
                 [:db] {:mw [#'wrap-db]
                        "pg" pg/routes
                        "graphql" gq/routes
                        "swagger" swag/routes}}}]
    (assoc r "swagger" {:GET (swagr/mk-swagger r)})))


(defn collect-mw [match]
  (->> (conj (:parents match) (:match match))
       (mapcat :mw)
       (filterv (complement nil?))))

(defn collect-params [match]
  (->> (conj (:parents match) (:match match))
       (map :params)
       (filterv (complement nil?))
       (apply merge {})))

(defn match-route [routes meth path]
  (route/match [meth path] routes))


(defn resolve-route [h routes]
  (fn [{uri :uri meth :request-method :as req}]
    (if-let [route (match-route routes meth uri)]
      (h (assoc req :route route))
      {:body (str "Page " uri " not found")
       :headers {"Content-Type" "text"}
       :status 404})))

(defn build-stack
  "wrap h with middlewares mws"
  [h mws]
  ((apply comp mws) h))

(defn dispatch [routes]
  (-> (fn [{handler :handler route :route :as req}]
        (let [mws     (collect-mw route)
              route-params (reduce (fn [acc [k v]] (assoc acc k (codec/url-decode v))) {} (:params route))
              extra-params (collect-params route)
              handler (get-in route [:match])
              req     (update-in req [:params] merge route-params (or extra-params {}))]
          (when (and mws (not (empty? mws)))
            (log/debug "Middle-wares: "   (pr-str mws)))
          ((build-stack handler mws) req)))
      (resolve-route routes)))

(def app (-> (dispatch routes)
             (wrap-exception)
             (wrap-format)
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
  (start)
  )
