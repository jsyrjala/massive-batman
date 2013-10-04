(ns liber.system
  (:use [liber.lifecycle]
        [clojure.tools.logging :only (trace debug info warn error)]
        )
  (:require [org.httpkit.server :as httpkit]
            [ring.middleware.reload :as reload]
            [liber.database :as db]
            [liber.route]
            )
  )

(defrecord Server [port ring-handler]
  Lifecycle
  (start [this]
         (info "Start http kit, port:" port)
         (assoc this
           :httpkit (httpkit/run-server ring-handler
                                        {:port port})))
  (stop [this]
        (info "Stop http kit, port:" port)
        ((:httpkit this))
        (dissoc this :httpkit)))


(defrecord LSystem [database server]
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
  (let [routes (-> #'liber.route/app reload/wrap-reload)
        database (db/new-database {:connection-uri "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1."
                                   :classname "org.h2.Driver"
                                   :username ""
                                   :password ""
                                   :max-connections-per-partition 20
                                   :partition-count 4})
        server (Server. port routes)
        system (LSystem. database server)]
    system))
