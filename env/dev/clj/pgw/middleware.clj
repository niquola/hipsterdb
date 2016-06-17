(ns pgw.middleware
  (:require [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [prone.middleware :refer [wrap-exceptions]]))

(defn wrap-middleware [handler]
  (-> handler
      (wrap-defaults site-defaults)
      wrap-exceptions))
