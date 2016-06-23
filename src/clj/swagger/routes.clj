(ns swagger.routes
  (:require [clojure.zip :as z]
            [clojure.string :as str]))

(defn method? [k]
  (contains? #{"GET" "PUT" "POST" "DELETE"} (str/upper-case (name k))))

(defn to-path [k]
  (if (vector? k)
    (str "{" (name (first k)) "}")
    k))

(defn definition [m v params]
  (if-let [sw (and m (:swagger m))]
    (if (fn? sw) (sw params) sw)
    {:x-swagger-handler (pr-str v)}))

(defn ordinary-path? [k] (string? k))
(defn parameter? [k] (vector? k))

(defn reduce-layer [acc r]
  (reduce
   (fn [{cp :current-path :as acc} [k v]]
     (let [m (when (var? v) (meta v))
           v (if (var? v) (deref v) v)]
       (-> (cond
             (ordinary-path? k)
             (reduce-layer (update-in acc [:current-path] str "/" k) v)

             (parameter? k)
             (let [gen (get r (first k))]
               (let [ps (gen (:params acc))]
                 (println gen)
                 (reduce (fn [acc pk]
                           (reduce-layer
                            (merge acc {:current-path (str cp "/" pk)
                                        :params (assoc (:params acc) (first k) pk)})
                             v))
                         acc ps))
               #_(reduce-layer (update-in acc [:current-path] str "/" (to-path k)) v))


             (method? k)
             (assoc-in acc [:paths cp (str/lower-case (name k))] (definition m v (:params acc)))

             :else acc)

           (assoc :current-path cp))))
   acc r))

(defn build-paths [r]
  (reduce-layer {:paths {} :current-path ""} r))

(defn hndl
  "DOC STRING"
  [req]
  {:body "ok"})

(build-paths {:GET #'hndl
              "path" {:GET #'hndl
                      :param   (fn [_] (println "Here") (mapv str (range 10)))
                      [:param] {:GET #'hndl}}})


