(ns liber.database
  (:require [clojure.tools.logging :refer [debug error info]]
            [com.stuartsierra.component :refer [Lifecycle]])
  (:import [com.zaxxer.hikari HikariDataSource HikariConfig]))

(defn- make-hikari-pool [db-spec]
  (let [config (HikariConfig.)
        {:keys [datasource-classname
                connection-uri
                username
                password
                max-connections]} db-spec]
    (doto config
      (.setDataSourceClassName datasource-classname)
      (.setMaximumPoolSize max-connections)
      (.setConnectionTestQuery "VALUES 1")
      (.addDataSourceProperty "URL",  connection-uri)
      (.addDataSourceProperty "user" username)
      (.addDataSourceProperty "password" password)
      (.setPoolName "ruuvi-db-hikari"))
    (HikariDataSource. config)
  ))

(defprotocol Datasource
  (datasource [this]))

(defrecord Database [db-spec data]
  Lifecycle
  Datasource
  (start [this]
         (debug "Start database connection")
         (let [pool (make-hikari-pool db-spec)]
           (swap! data assoc :datasource pool)
           (assoc this :datasource pool)))
  (stop [this]
        (debug "Stop database connection")
        (try
          (.close (:datasource @data))
          (catch Exception e (error "failed to close" e)))
        (info "connection closed")
        (dissoc this :datasource))
  (datasource [this] @data)
  )


(defn new-database [db-spec]
  (Database. db-spec (atom {})))
