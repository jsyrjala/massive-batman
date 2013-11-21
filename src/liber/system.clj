(ns liber.system
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :refer [trace debug info warn error]]
            [org.httpkit.server :as httpkit]
            [ring.middleware.reload :as reload]
            [liber.database :as db]
            [liber.route :as route]
            [liber.resource :as resource]
            [liber.pubsub :as pubsub]
            [liber.websocket :as websocket]
            [liber.database.migration :as migration]
            [liber.database.events :as events]
            )
  )


(defrecord HttpKitServer [port routes]
  component/Lifecycle
  (start [this]
         (debug "Start http kit, port:" port)
         (assoc this
           :httpkit (httpkit/run-server (route/ring-handler routes)
                                        {:port port})))
  (stop [this]
        (debug "Stop http kit, port:" port)
        ((:httpkit this))
        (dissoc this :httpkit)))

(defn database []
  (component/using
   (db/map->Database {:data (atom {})})
   [:db-spec]))

(defn migrator []
  (component/using
   (migration/map->DatabaseMigrator {})
   [:db-spec]))

(defn pubsub-service []
  (component/using
   (pubsub/map->ClojurePubSub {:channels (atom {})})
   []))

(defn event-service []
  (component/using
   (events/map->SqlEventService {})
   [:database :pubsub-service]))

(defn websocket []
  (component/using
   (websocket/map->WebSocket {})
   [:pubsub-service :event-service]))

(defn routes []
  (component/using
   (route/map->RestRoutes {})
   [:websocket :resources]))

(defn resources []
  (component/using
   (resource/map->JsonEventResources {})
   [:event-service]))

(defn httpkit-server [port]
  (component/using
   (map->HttpKitServer {:port port})
   [:routes]))


(def ruuvi-components [:migrator :database :pubsub-service :websocket :event-service :routes :server])

(defrecord RuuviSystem [migrator database pubsub-service websocket event-service routes server]
  component/Lifecycle
  (start [this]
         (component/start-system this (keys this)))
  (stop [this]
        (component/stop-system this (keys this)))
  )

(defn create-system [port]
  (let [conf {:migrator (migrator)
              :database (database)
              :pubsub-service (pubsub-service)
              :websocket (websocket)
              :event-service (event-service)
              :routes (routes)
              :resources (resources)
              :server (httpkit-server port)
              :db-spec {:connection-uri "jdbc:h2:mem:test;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1"
                        :classname "org.h2.Driver"
                        :username ""
                        :password ""
                        :max-connections-per-partition 20
                        :partition-count 4}
              :db-spec-file {:connection-uri "jdbc:h2:file;DATABASE_TO_UPPER=TRUE"
                             :classname "org.h2.Driver"
                             :username ""
                             :password ""
                             :max-connections-per-partition 20
                             :partition-count 4}}]
    (map->RuuviSystem conf)))
