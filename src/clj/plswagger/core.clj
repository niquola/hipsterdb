(ns plswagger.core
  (:require [plswagger.definitions :as definitions]))

(defn meta 
  "Doc string"
  {:swagger {:summary "list views and tables"}}
  [req])


(def v1-routes
  {[:db] {:GET #'meta
          "definitions" #'definitions/routes}})

(def routes
  {"v1" v1-routes})

