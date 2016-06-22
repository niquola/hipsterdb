(ns swagger.core
  (:require [pg.core :as pg]))

(def type-map
  {"bool" "Boolean"
   "int2" "Integer"
   "char" "String"
   "oid"  "String"
   "int4" "Integer"})

(defn get-type [{tp :typname}]
  {:type (str (or (get type-map tp) tp))})

{:properties {}}

(defn table-to-schema [tbl-name]
  (let [cols (pg/columns tbl-name)
        props  (reduce (fn [acc {fld :attname :as r}]
                            (assoc acc (keyword fld)
                                   (get-type r)))
                          {}
                          cols)]
    {:type "Object"
     :properties props
     :require (->> cols
                  (filter :attnotnull)
                  (mapv :attname))}))

(defn tables [{{rel :rel-name :as params} :params :as req}]
  {:body  (table-to-schema rel)})

(def routes
  {[:rel-name] {:GET #'tables}})


(comment
  (db/with-db "nicola"
    (table-to-schema "test")))


