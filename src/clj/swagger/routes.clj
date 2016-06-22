(ns swagger.routes
  (:require [clojure.zip :as z]
            [clojure.string :as str]))

(defn method? [k]
  (contains? #{"GET" "PUT" "POST" "DELETE"} (str/upper-case (name k))))

(defn to-path [k]
  (if (vector? k)
    (str "{" (name (first k)) "}")
    k))

(defn definition [m v]
  (if-let [sw (and m (:swagger m))]
    (if (fn? sw) (sw) sw)
    {:x-swagger-handler (pr-str v)}))

(defn reduce-layer [acc r]
  (reduce (fn [{cp :current-path :as acc} [k v]]
            (let [m (when (var? v) (meta v))
                  v (if (var? v) (deref v) v)]
              (cond
                (and (map? v)
                     (or (string? k)
                         (vector? k)))
                (-> (reduce-layer (update-in acc [:current-path] str "/" (to-path k)) v)
                    (assoc :current-path cp))

                (method? k)
                (-> acc 
                    (assoc-in [:paths cp (str/lower-case (name k))] (definition m v))
                    (assoc :current-path cp)))))
   acc r))

(defn build-paths [r]
  (reduce-layer {:paths {} :current-path ""} r))

(defn hndl
  "DOC STRING"
  [req]
  {:body "ok"})

(build-paths {:GET #'hndl
              "path" {:GET #'hndl
                      [:param] {:GET #'hndl}}})

(defn build-swagger [r]
  {:paths (:paths (build-paths r)) 
   :definitions {}
   :externalDocs {:description "PostgreSQL is your API"}
   :schemes ["http" "https"]
   :basePath "/"
   :host "localhost:8080"
   :info {:title "pgw"
          :description "pgw"
          :version "0.1"}
   :swagger "2.0"})

(defn mk-swagger [r]
  (fn [req]
    {:body (build-swagger r)}))
