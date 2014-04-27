(ns liber.system
  (:require [clojure.tools.logging :refer [debug]]
            [com.stuartsierra.component :as component]
            [liber.database :as db]
            [liber.database.events :as events]
            [liber.database.migration :as migration]
            [liber.pubsub :as pubsub]
            [liber.resource :as resource]
            [liber.route :as route]
            [liber.handler :as handler]
            [liber.websocket :as websocket]
            [org.httpkit.server :as httpkit]
            [plumbing.core :refer [defnk]]
            [com.redbrainlabs.system-graph :as system-graph]))


(defrecord HttpKitServer [port routes]
  component/Lifecycle
  (start [this]
         (debug "Start http kit, port:" port)
         ;;(assoc this
         ;;  :httpkit (httpkit/run-server (route/ring-handler routes)
         ;;                               {:port port})))
         (assoc this
           :httpkit (httpkit/run-server (-> routes :app)
                                        {:port port})))
  (stop [this]
        (debug "Stop http kit, port:" port)
        ((:httpkit this))
        (dissoc this :httpkit)))

(defnk database [db-spec]
  (db/map->Database {:db-spec db-spec :data (atom {})}))

(defnk migrator [db-spec]
  (migration/map->DatabaseMigrator {:db-spec db-spec}))

(defnk pubsub-service []
  (pubsub/map->ClojurePubSub {:channels (atom {})}))

(defnk event-service [database pubsub-service]
  (events/map->SqlEventService {:database database :pubsub-service pubsub-service}))

(defnk websocket [pubsub-service event-service]
  (websocket/map->WebSocket {:pubsub-service pubsub-service :event-service event-service}))

(defnk routes [websocket resources]
  (route/map->RestRoutes {:websocket websocket :resources resources}))

(defnk routes-swagger [database]
  (handler/map->SwaggerRoutes {:database database}))


(defnk resources [event-service]
  (resource/map->JsonEventResources {:event-service event-service}))

(defnk httpkit-server [port routes]
  (map->HttpKitServer {:port port :routes routes}))


(def ruuvi-system-graph
  {:database database
   :migrator migrator
   :pubsub-service pubsub-service
   :websocket websocket
   :event-service event-service
   :routes routes-swagger
   :resources resources
   :httpkit-server httpkit-server
   })

(defn create-system [port]
  (system-graph/init-system ruuvi-system-graph
                            {:port port
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
                                            :partition-count 4}
                             }))
