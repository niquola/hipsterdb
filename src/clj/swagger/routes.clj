(ns swagger.routes
  (:require [clojure.zip :as z]
            [clojure.string :as str]))

(defn method? [k]
  (contains? #{"GET" "PUT" "POST" "DELETE"} (str/upper-case (name k))))

(defn to-path [k]
  (if (vector? k)
    (str "{" (name (first k)) "}")
    k))

(defn reduce-layer [acc r]
  (reduce (fn [{cp :current-path :as acc} [k v]]
            (cond
              (and (map? v) (or (string? k) (vector? k)))
              (reduce-layer (update-in acc [:current-path] str "/" (to-path k)) v)

              (method? k)
              (assoc-in acc [:paths cp (str/lower-case (name k))] (if-let [m (when (var? v) (meta v))]
                                                                    (or (:swagger m) {})
                                                                    {:x-swagger-handler (pr-str v)}
                                                                    ))))
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
   :externalDocs {:url "??"
                  :description "??"}
   :schemes ["http" "https"]
   :basePath "/"
   :host "aidbox.io"
   :info {:title "saveme"
          :description "saveme"
          :version "0.1"}
   :swagger "2.0"})

(defn mk-swagger [r]
  (fn [req]
    {:body (build-swagger r)}))
