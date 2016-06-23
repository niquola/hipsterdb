(ns plswagger.definitions
  (:require [pg.core :as pg]
            [db :as db]))

(def type-map
  {"bool" "boolean"
   "int2" "integer"
   "char" "string"
   "character varying" "string"
   "oid"  "string"
   "int4" "integer"})

(defn get-type [{tp :data_type}]
  {:type (str (or (get type-map tp) tp))})

(defn table-to-schema [tbl-name]
  (let [cols (pg/columns {:table tbl-name})
        props  (reduce (fn [acc {fld :column_name :as r}]
                         (assoc acc (keyword fld)
                                (get-type r)))
                       {}
                       cols)]
    {:type "Object"
     :properties props
     :require (->> cols
                   (filter :is_nullable)
                   (mapv :column_name))}))

(defn with-params [& more]
  (into [{:name "_format" :in "query" :type "string"
          :description "Format",
          :enum ["text/asci" "json" "yaml"]
          :default "text/asci"}
         {:name "limit" :in "query" :type "integer"
          :default 30
          :description "Format"}
         ] more))

(defn tables
  {:swagger (fn [] {:summary "List tables" :parameters (with-params)})}
  [{params :params :as req}]
  {:body (pg/tables params)})

(defn columns
  {:swagger (fn [] {:summary "Columns" :parameters (with-params)})}
  [{params :params :as req}]
  {:body (pg/columns params)})

(defn table-def-swagger []
  {:summary "represent table as json schema"
   :tags ["definitions"]
   :parameters []})

(defn data
  {:swagger (fn [params]
              {:summary (str "Query data for " (:table params)) 
               :parameters (with-params
                             {:name "select",
                              :in "query",
                              :description "Format",
                              :type "array"
                              :items {:type "string"}
                              :enum (mapv :column_name (pg/columns {:table (:table params)}))})
               :responses {"200" {:schema {:type "array"
                                           :items (table-to-schema (:table params))}}}
               })}
  [{{sch :schema rel :table :as params} :params :as req}]
  {:body (pg/query {:from [(keyword (str sch "." rel))]} params)})

(defn get-schemata [params]
  (map :schema_name (pg/schemas params)))

(defn get-tables [params]
  (mapv :table_name (pg/tables params)))


(def routes
  {:schema   get-schemata
   [:schema] {:table get-tables
              [:table]  {:GET #'data}}})

(comment
  (db/with-db "nicola"
    (table-to-schema "test")))

