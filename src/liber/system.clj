(ns liber.system
  (:require [com.stuartsierra.component :refer [Lifecycle start stop]]
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

(defrecord Server [port routes]
  Lifecycle
  (start [this]
         (debug "Start http kit, port:" port)
         (assoc this
           :httpkit (httpkit/run-server (route/ring-handler routes)
                                        {:port port})))
  (stop [this]
        (debug "Stop http kit, port:" port)
        ((:httpkit this))
        (dissoc this :httpkit)))

(defrecord LSystem [migrator database pubsub websocket event-service routes server]
  Lifecycle
  (start [this]
         (debug "Start system")
         (reduce (fn [system key]
                   (update-in system [key] start))
                 this (keys this)))
  (stop [this]
        (debug "Stop system")
        (reduce (fn [system key]
                  (update-in system [key] stop))
                this (reverse (keys this)))))

(defn create-system [port]
  (info "Creating a new system")
  (let [db-spec {:connection-uri "jdbc:h2:mem:test;DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1"
                 :classname "org.h2.Driver"
                 :username ""
                 :password ""
                 :max-connections-per-partition 20
                 :partition-count 4}
        db-spec-file {:connection-uri "jdbc:h2:file;DATABASE_TO_UPPER=TRUE"
                      :classname "org.h2.Driver"
                      :username ""
                      :password ""
                      :max-connections-per-partition 20
                      :partition-count 4}
        database (db/new-database db-spec)
        migrator (migration/create-migrator db-spec)
        pubsub-service (pubsub/new-pubsub-server)
        event-service (events/new-event-service database pubsub-service)
        websocket (websocket/new-websocket pubsub-service event-service)
        resources (resource/new-event-resources event-service)
        routes (route/new-rest-routes websocket resources)
        server (->Server port routes)
        system (->LSystem migrator database pubsub-service websocket event-service routes server)]
    ;; TODO tmp
    (migration/migrate-forward migrator)
    ;; (migration/migrate-backward migrator)
    ;;(events/create-tracker! event-service {})
    system))

