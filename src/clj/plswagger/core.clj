(ns plswagger.core
  (:require [plswagger.definitions :as definitions]
            [pg.core :as pg]))

(defn meta 
  "Doc string"
  {:swagger {:summary "list views and tables"}}
  [req])

(def routes
  {"v1" #'definitions/routes})

