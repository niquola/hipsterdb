(ns web.core
  (:require
   [clojure.tools.logging :as log]
   [garden.core :as css]
   [db :as db]
   [cors :as cors]
   [garden.units :as u]
   [hiccup.core :as html]
   [org.httpkit.server :as http]
   [ring.middleware.defaults :as rmd]
   [ring.middleware.resource :as rmr]
   [graphql.core :as gq]
   [clojure.tools.logging :as log]
   [swagger.core :as swag]
   [swagger.routes :as swagr]
   [ring.util.codec :as codec]
   [formats :as fmt]
   [postgrest.core :as postgrest]
   [plswagger.core :as plswagger]
   [route-map.core :as route]
   [oauth.core :as oauth]
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
    (let [
          res (h req)
          fmt (or (get-in res [:headers "Content-Type"])
                  (get-in req [:params  :_format])
                  (get-in req [:headers "Content-Type"])
                  "application/json")
          ]
      (if-not (string? (:body res))
        (-> res
            (update-in [:body] (fn [o] (fmt/to fmt o)))
            (assoc-in [:headers "Content-Type"] (fmt/transform fmt)))
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
      (db/with-db db
        (when-let [jwt (:jwt req)]
          (db/execute (str "SET ROLE " (:sub jwt))))
        (h req))
      (h req))))

(defn $index [req]
  {:body    (layout [:div#app "Loading..."])
   :headers {"Content-Type" "text/html"}
   :status 200})

(declare routes)

(defn root-swagger [req]
  {:body 
   {:paths {"/" {:get {:summary "ui"}}
            "/swagger-ui/index.html" {:get {:summary "Swagger UI"}}
            "/db/{db}/swagger" {:get {:summary "DB API Swagger SPEC"}}}
    :externalDocs {:description "PostgreSQL is your API"}
    :schemes ["http" "https"]
    :basePath "/"
    :host "localhost:8080"
    :info {:title "pgw"
           :description "pgw"
           :version "0.1"}
    :swagger "2.0"}})

(declare db-api-routes)

(defn db-api-spec [{{db :db :as params} :params :as req}]
  (let [base-path (str "/" db)]
    {:body
     {:paths (:paths (swagr/build-paths db-api-routes)) 
      :externalDocs {:description "PostgreSQL is your API"}
      :schemes ["http" "https"]
      :basePath base-path
      :host "localhost:8080"
      :info {:title "pgw"
             :description "pgw"
             :version "0.1"}
      :swagger "2.0"
      :securityDefinitions
      {:implicit {:type "oauth2"
                  :authorizationUrl "/auth"
                  :flow "implicit"}}}}))

(def db-api-routes
  {"swagger" {:GET #'db-api-spec}
   ;;"postgrest" #'postgrest/routes
   "plswagger" #'plswagger/routes
   ;;"graphql"   #'gq/routes
   })

(def routes
  {:mw [oauth/wrap-token wrap-db]
   :GET #'$index
   "auth"  #'oauth/routes
   "swagger" {:GET  #'root-swagger}
   [:db] #'db-api-routes})


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
             (cors/wrap-cors)
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
  (start))
