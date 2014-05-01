(ns liber.server
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :refer [debug]]
            [org.httpkit.server :as httpkit])
  )

(defrecord HttpKitServer [port routes]
  component/Lifecycle
  (start [this]
         (debug "HttpKitServer starting. port:" port)
         (assoc this
           :httpkit (httpkit/run-server (-> routes :app)
                                        {:port port})))
  (stop [this]
        (debug "HttpKitServer stopping. port:" port)
        ((:httpkit this))
        (dissoc this :httpkit)))


(defn new-server [port routes]
  (->HttpKitServer port routes))
