(ns formats
  (:require
   [cheshire.generate :as cg]
   [yaml.core :as ym]
   [cheshire.core :as cc]
   [cognitect.transit :as transit]
   [clojure.string :as str])
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
  {:from (fn [s] (ym/parse-string s))
   :to (fn [o] (ym/generate-string o))})

(defn measure-columns [rs]
  (reduce (fn [acc r]
            (reduce (fn [acc [k v]]
                      (assoc acc k (max (or (get acc k) 0) (.length (name k)) (.length (str v))))
                      ) acc r))
          {} rs))

(defn pad [v l]
  (format (str "%-" (+ l 1) "s") v))

((:to asci) [{:a "aaaa" :b "ups"} {:a "............." :b "cc"}])

(def asci
  {:from (fn [s] (throw (Exception. "Not impl.")))
   :to (fn [o]
         (let [ks (measure-columns o)
               k-nms (keys ks)]
           (str 
            (str/join "|" (map #(pad (name %) (get ks %)) k-nms))
            "\n"
            (str/join "\n"
                      (for [r o]
                        (str/join "|" (for [k k-nms] (pad (get r k) (get ks k)))))))))})


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
   "application/x-yaml" yaml

   "text/asci" asci})

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
