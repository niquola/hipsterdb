(ns graphql.typesystem
  (:require [pg.core :as pg]))



(def type-map
  {"bool" "Boolean"
   "int2" "Integer"
   "char" "String"
   "oid"  "String"
   "int4" "Integer"})

(defn get-type [{tp :typname nn :attnotnull}]
  (str (or (get type-map tp) tp) (when nn "!")))

[:a.attname :field]
[:t.typname :type]
[:a.attnotnull :as :not_null]

(defn table-to-type [tbl-name]
  (let [fields (pg/columns tbl-name)
        gq-fields (reduce (fn [acc {fld :attname :as r}]
                            (assoc acc (keyword fld)
                                   (get-type r)))
                          {}
                          fields)]
    {tbl-name gq-fields}))


(comment
  (db/with-db "postgres"
    (table-to-type "pg_class"))

  (db/with-db "nicola"
    (table-to-type "test"))
  )

