(ns liber.system
  (:use [liber.lifecycle]
        [clojure.tools.logging :only (trace debug info warn error)]
        )
  (:require [org.httpkit.server :as httpkit]
            [ring.middleware.reload :as reload]
            [liber.database :as db]
            [liber.route :as route]
            [liber.pubsub :as pubsub]
            [liber.websocket :as websocket]
            [liber.database.migration :as migration]
            )
  )

(defrecord Server [port routes]
  Lifecycle
  (start [this]
         (info "Start http kit, port:" port)
         (assoc this
           :httpkit (httpkit/run-server (route/ring-handler routes)
                                        {:port port})))
  (stop [this]
        (info "Stop http kit, port:" port)
        ((:httpkit this))
        (dissoc this :httpkit)))

(defrecord LSystem [migrator database pubsub websocket routes server]
  Lifecycle
  (start [this]
         (info "Start system")
         (reduce (fn [system key]
                   (update-in system [key] start))
                 this (keys this)))
  (stop [this]
        (info "Stop system")
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
        websocket (websocket/new-websocket pubsub-service)
        routes (route/new-rest-routes websocket pubsub-service)
        server (->Server port routes)
        system (->LSystem migrator database pubsub-service websocket routes server)]
    ;; TODO tmp
    (migration/migrate-forward migrator)
    ;; (migration/migrate-backward migrator)

    system))

