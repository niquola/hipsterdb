(ns falcor.core
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

(defn rel-desc [{{db :db rel :relation-name :as params} :params :as req}]
  {:body (query {:from [(keyword rel)]} params)})

(defn dbs [{params :params :as req}]
  {:body (query {:from [:pg_database]} params)})

(def routes
  {[:relation-name] {:GET #'rel-desc}})
