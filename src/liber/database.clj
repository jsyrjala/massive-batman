(ns liber.database
  (:use liber.lifecycle
        [clojure.tools.logging :only (trace debug info warn error)]
        )
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql])
  (:import [com.jolbox.bonecp BoneCPDataSource])
  )

(defn- make-pool [db-spec]
  (let [pool (BoneCPDataSource.)
        {:keys [classname
                connection-uri
                username
                password
                max-connections-per-partition
                partition-count]} db-spec]
    (.setDriverClass pool classname)
    (.setJdbcUrl pool connection-uri)
    (.setUsername pool username)
    (.setPassword pool password)
    (when max-connections-per-partition
      (.setMaxConnectionsPerPartition pool max-connections-per-partition))
    (when partition-count
      (.setPartitionCount pool partition-count))
    pool))

(defrecord Database [db-spec]
  Lifecycle
  (start [this]
         (info "Start database connection")
         (let [pool (make-pool db-spec)]
           (.close (.getConnection pool))
           (assoc this :datasource pool)))
  (stop [this]
        (info "Stop database connection")
        (.close (:datasource this))
        (dissoc this :datasource))
  )


(defn new-database [db-spec]
  (Database. db-spec))
