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

(defrecord LSystem [database pubsub websocket routes server]
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
  (info "creating a new system")
  (let [;;routes (-> #'liber.route/app reload/wrap-reload)
        database (db/new-database {:connection-uri "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1."
                                   :classname "org.h2.Driver"
                                   :username ""
                                   :password ""
                                   :max-connections-per-partition 20
                                   :partition-count 4})
        pubsub (pubsub/new-redis-server {})
        websocket (websocket/new-websocket pubsub)
        routes (route/new-rest-routes websocket)
        server (->Server port routes)
        system (->LSystem database pubsub websocket routes server)]
    system))
