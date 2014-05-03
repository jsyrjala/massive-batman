(ns liber.system
  (:require [clojure.tools.logging :refer [debug]]
            [com.stuartsierra.component :as component]
            [liber.database :as db]
            [liber.database.events :as events]
            [liber.database.migration :as migration]
            [liber.pubsub :as pubsub]
            [liber.handler :as handler]
            [liber.websocket :as websocket]
            [liber.server :as server]
            [plumbing.core :refer [defnk]]
            [com.redbrainlabs.system-graph :as system-graph]))

(defnk database [db-spec]
  (db/map->Database {:db-spec db-spec :data (atom {})}))

(defnk migrator [db-spec]
  (migration/map->DatabaseMigrator {:db-spec db-spec}))

(defnk pubsub-service []
  (pubsub/map->ClojurePubSub {:channels (atom {})}))

(defnk event-service [database pubsub-service]
  (events/map->SqlEventService {:database database :pubsub-service pubsub-service}))

(defnk websocket [pubsub-service event-service]
  (websocket/new-websocket pubsub-service event-service))

(defnk routes-swagger [event-service websocket]
  (handler/new-routes event-service websocket))

(defnk httpkit-server [port routes migrator]
  (server/new-httpkit-server port routes))


(def ruuvi-system-graph
  {:database database
   :migrator migrator
   :pubsub-service pubsub-service
   :websocket websocket
   :event-service event-service
   :routes routes-swagger
   })

(def file-db-spec {:connection-uri "jdbc:h2:~/ruuvidb/test;DATABASE_TO_UPPER=TRUE;TRACE_LEVEL_FILE=4"
                   :classname "org.h2.Driver"
                   :datasource-classname "org.h2.jdbcx.JdbcDataSource"
                   :username ""
                   :password ""
                   :max-connections-per-partition 20
                   :partition-count 4
                   :max-connections 80})

(def mem-db-spec {:connection-uri "jdbc:h2:mem:test;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=4"
                  :classname "org.h2.Driver"
                  :datasource-classname "org.h2.jdbcx.JdbcDataSource"
                  :username ""
                  :password ""
                  :max-connections-per-partition 20
                  :partition-count 4
                  :max-connections 80})

(defn- create-system [graph options]
  (system-graph/init-system graph options))

(defn create-prod-system [port]
  (create-system (assoc ruuvi-system-graph :server httpkit-server)
                 {:port port
                  :db-spec file-db-spec}))

(defn create-dev-system [port]
  (create-system (assoc ruuvi-system-graph :server httpkit-server)
                 {:port port
                  :db-spec mem-db-spec}))

(defn create-war-system []
  (create-system ruuvi-system-graph
                 {:db-spec file-db-spec}))
