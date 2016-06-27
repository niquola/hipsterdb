(ns pg.core
  (:require [db :as db]
            [clojure.string :as str]))

(defn columns [tbl-name]
  (db/query {:select [:*]
              :from  [[:pg_attribute :a]
                      [:pg_class :c]
                      [:pg_type :t]]
              :where [:and
                      [:= :c.oid :a.attrelid]
                      [:= :a.atttypid :t.oid]
                      [:> :a.attnum 0]
                      [:= :c.relname (name tbl-name)]]}))


(defn select [params]
  (if-let [s (:select params)]
    (mapv keyword (str/split s #","))
    [:*]))

(defn ->limit [m params]
  (if-let [l (:limit params)]
    (assoc m :limit l)
    m)) 

(defn query [q params]
  (-> q
      (assoc :select (select params))
      (->limit params)
      (db/query)))

(def rel-desc-meta
  {:tags ["db"]
   :summary  "This is summary"
   :produces ["application/json" "application/xml"]})

(defn rel-desc
  "Table definition"
  {:swagger rel-desc-meta}
  [{{db :db rel :relation-name :as params} :params :as req}]
  {:body (query {:from [(keyword rel)]} params)})

(defn databases [params]
  (db/with-db "postgres" (query {:from [:pg_database]} params)))

(defn tables [params]
  (query {:from [:information_schema.tables]
          :where [:= :table_schema (:schema params)]} params))

(defn columns [params]
  (query {:from [:information_schema.columns]
          :where [:= :table_Name (params :table)]} params))

(defn schemas [params]
  (query {:from [:information_schema.schemata]
          :where [:= :schema_name "public"] #_[:not [:in :schema_name ["pg_catalog"]]]} params))

(defn dbs
  "List databases"
  [{params :params :as req}]
  {:body (databases params)})

(def routes
  {[:relation-name] {:GET #'rel-desc}})
