(ns graphql.core
  (:require [graphql.typesystem :as ts]))


(def table-to-type ts/table-to-type)

(defn to-graphql [ast]
  (with-out-str
    (doseq [[tn tp] ast]
      (println "type" tn " {")
      (doseq [[f t] tp]
        (println " " (name f) ": " t))
      (println "}"))))

(defn table [{{db :db rel :relation-name :as params} :params :as req}]
  {:headers {"Content-Type" "text"}
   :body (to-graphql (table-to-type rel))})

(def routes
  {"v1" {"introsp" {:GET #'table}
         "query" {:GET #'table}}})
