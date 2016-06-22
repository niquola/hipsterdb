(ns postgrest.core)

(defn list-relations
  "Doc string"
  {:swagger {:summary "list views and tables"}}
  [req])

(defn list-procs
  "Doc string"
  {:swagger {:summary "list stored procedures"}}
  [req]
  )

(defn query
  {:swagger {:summary "list stored procedures"}}
  [req])

(defn rpc-call
  {:swagger {:summary "list stored procedures"}}
  [req])


(def v1-routes
  {:GET #'list-relations
   [:relation] {:GET #'query}
   "rpc" {:GET #'list-procs
          :POST #'rpc-call}})
(def routes
  {"v1" v1-routes})
