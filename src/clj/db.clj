(ns db
  (:require
    [clojure.java.jdbc :as jdbc]
    [clojure.string :as cs]
    [honeysql.core :as sql]
    [honeysql.types :as types]
    [clj-time.core :as t]
    [clj-pg.pool :as poole]
    [clj-pg.honey :as pghoney]
    [clj-pg.coerce :as coerce]
    [clj-time.coerce :as tc]
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [clj-pg.easy :as easy])
  (:refer-clojure :exclude [update])
  (:import org.postgresql.util.PGobject))

(def DB-URL "jdbc:postgresql://localhost:5432/DB_NAME?user=nicola&password=nicola&stringtype=unspecified")

(defn get-datasource [db-name]
  (let [database-url DB-URL]
    (assert database-url "Please initialize DATABASE_URL")
    {:idle-timeout       1000
     :minimum-idle       0
     :maximum-pool-size  2
     :connection-init-sql "SET plv8.start_proc = 'plv8_init'"
     :data-source.url     (cs/replace database-url #"DB_NAME" (name db-name))}))

(defn shutdown-connections []
  (easy/shutdown-connections))

(defn shutdown-connections-for-db [db-name]
  (easy/shutdown-connections-for-db db-name))

(defmacro with-db [db-name & body]
  `(easy/with-db ~db-name get-datasource ~@body))

(defmacro transaction  [& body]
  `(easy/transaction ~@body))

(defmacro info [& msg]
  `(log/info ~@msg))

(defmacro rollback-transaction  [& body]
  `(easy/try-with-rollback ~@body))

(defn exec! [sql]
  (pghoney/exec! easy/*db* sql))

(defn create-db [db-name & [template]]
  (easy/create-database db-name template))

(defn drop-db [db-name]
  (easy/drop-database db-name))

(defn create [spec ent]
  (easy/create spec ent))

(defn delete [spec ent]
  (easy/delete spec ent))

(defn execute [& args]
  (apply easy/execute args))

(defn query [& args]
  (apply easy/query args))

(defn debug-query [& args]
  (apply easy/debug-query args))

(defn query-first [& args]
  (apply easy/query-first args))

(defn query-value [& args]
  (apply easy/query-value args))

(defn update [spec ent]
  (easy/update spec ent))

(defn table-exists? [tbl]
  (easy/table-exists? tbl))

(defn database-exists? [db]
  (easy/database-exists? db))

(defn init-db []
  (when-not (table-exists? :app.schema)
    (execute "create schema if not exists app")
    (execute (jdbc/create-table-ddl
              :app.schema
              [:name :text "PRIMARY KEY"]
              [:created_at :timestamp "DEFAULT CURRENT_TIMESTAMP"]))))

(defn migration-exists? [nm]
  (easy/query-first {:select [:*] :from [:app.schema] :where [:= :name nm]}))

(defmacro migrate-up [nm & body]
  `(when-not (migration-exists? ~(str nm))
     (info "migrate-up" ~(str nm))
     ~@body
     (create {:table :app.schema} {:name ~(str nm)})))

(defmacro migrate-down [nm & body]
  `(when (migration-exists? ~(str nm))
     (info "migrate-down" ~(str nm))
     ~@body
     (d! :app.schema ["name=?" ~(str nm)])))

(defn create-table [& args]
  (easy/execute (apply jdbc/create-table-ddl args)))

(defn drop-table
  ([tbl] (easy/execute (str "DROP TABLE " (name tbl))))
  ([tbl opts] (easy/execute (str "DROP TABLE " (when (:force opts) " IF EXISTS ") (name tbl) (when (:cascade opts) " CASCADE")))))

(defn raw [& args]
  "proxy to honey.sql/raw"
  (apply sql/raw args))

(defn call [& args]
  "proxy to honey.sql/call"
  (apply sql/call args))

(comment

  (shutdown-connections)

  (with-db "postgres"
    (query {:select [:*]
            :from [:pg_roles]})))
