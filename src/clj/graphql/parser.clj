(ns graphql.parser
  (:require [clj-antlr.core :as antlr]
            [clojure.walk :as walk]))

(def parse* (antlr/parser "Graphql.g4"))

(defn definition [x & _] x)

(def document identity)

(defn fieldDefinition [& x]
  (reduce (fn [acc p]
            (assoc acc (first p) (second p)))
          {} x))

(defn fieldDefinitions [& x]
  (reduce
   (fn [acc fd]
     (assoc acc (keyword (:fieldDefName fd)) (str (:fieldDefType fd) (when (:fieldRequired fd) "!"))))
   {} x))

(defn typeDefName [nm & _] nm)

(defn typeDefinition [_ tn _ fields _]
  {:__type tn
   :fields fields})

(def transformers
  {:definition definition
   :document document
   :fieldDefinition fieldDefinition
   :fieldDefinitions fieldDefinitions
   :typeDefName  typeDefName
   :typeDefinition typeDefinition})

(defn to-ast [pt]
  (walk/postwalk
   (fn [x]
     (if-let [nt (and (seq? x) (first x) (get transformers (first x)))]
       (apply nt (rest x))
       x)) pt))

(defn parse [s] (to-ast (parse* s)))
