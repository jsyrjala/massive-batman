(ns liber.database
  (:require [com.stuartsierra.component :refer [Lifecycle]]
            [clojure.tools.logging :refer [trace debug info warn error]]
            [clojure.java.jdbc :as jdbc])
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

(defprotocol Datasource
  (datasource [this]))

(defrecord Database [db-spec data]
  Lifecycle
  Datasource
  (start [this]
         (debug "Start database connection")
         (let [pool (make-pool db-spec)]
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
