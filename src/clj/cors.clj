(ns cors
  (:require
   [clojure.tools.logging :as log]))

(defn cors-options [{meth :request-method hs :headers}]
  (when (= :options meth)
    (let [headers (get hs "access-control-request-headers")
          method  (get hs "access-control-request-method")]
      (log/debug "CORS:\n\tRequest-Headers:" headers "Request-Method" methods)

      {:status 200
       :body "preflight complete"
       :headers {"Access-Control-Allow-Headers" headers
                 "Access-Control-Allow-Methods" method}})))

(defn allow [origin resp]
  (log/debug "CORS allowed for " origin)
  (merge-with
    merge resp
    {:headers
     {"Access-Control-Allow-Origin" origin
      "Access-Control-Allow-Credentials" "true"
      "Access-Control-Expose-Headers" "Location, Content-Location, Category, Content-Type, X-total-count"}}))

(defn cors-origins
  "May check if allow CORS access here"
  [{hs :headers}]
  (get hs "origin"))

(defn wrap-cors
  "Cross-origin resource sharing middle-ware"
  [h]
  (fn [req]
    (if-let [origin (cors-origins req)]
      (allow origin (or (cors-options req) (h req)))
      (h req))))
