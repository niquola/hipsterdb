(ns pgw.formats
  (:require
   [cheshire.generate :as cg]
   [yaml.core :as ym]
   [cheshire.core :as cc]
   [cognitect.transit :as transit])
  (:import (java.io ByteArrayInputStream ByteArrayOutputStream)
           org.postgresql.util.PGobject))


(cg/add-encoder
 org.postgresql.util.PGobject
 (fn [o json-generator]
   (.writeString json-generator
                 (pr-str o))))

(defn- to-transit-stream [clj & [frmt]]
  (let [out (ByteArrayOutputStream.)]
    (->  out
         (transit/writer :json {})
         (transit/write clj))
    out))


(def transit
  {:to (fn  [clj & [frmt]]
         (.toString (to-transit-stream clj [frmt])))

   :from (fn [str & [frmt]]
           (-> (cond
                 (string? str) (ByteArrayInputStream. (.getBytes str))
                 (= (type str) ByteArrayOutputStream) (ByteArrayInputStream. (.toByteArray str))
                 :else str)
               (transit/reader :json)
               (transit/read)))})


(def json
  {:from (fn [str]
           (if (string? str)
             (cc/parse-string str keyword)
             str))
   :to (fn [clj &  [options]]
         (cc/generate-string clj options))})

(def yaml
  {:from (fn from-yaml [s]
           (ym/parse-string s))
   :to (fn to-yalm [o]
         (ym/generate-string o))})




(def formats
  {"json" json
   "application/json" json
   "application/x-javascript" json
   "text/javascript" json
   "text/x-javascript" json
   "text/x-json" json

   "transit" transit
   "application/transit+json" transit

   "yaml" yaml
   "text/yaml" yaml
   "text/x-yaml" yaml
   "application/yaml" yaml
   "application/x-yaml" yaml})

(defn to [mime o]
  (if-let [fmt (get formats mime)]
    ((:to fmt) o)
    (str "UPS unkonw format " mime " "(pr-str o))))

(defn from [mime s]
  (if-let [fmt (get formats mime)]
    ((:to fmt) s)
    (str "UPS unkonw format " mime " "(pr-str s))))

(comment
  (from-transit
   (to-transit-stream {:a 1} :json))

  (from-transit
   (to-transit {:a 1} :json))
  )

