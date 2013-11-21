(ns liber.core
  (:require [com.stuartsierra.component :refer [start stop]]
            [clojure.tools.logging :refer [trace debug info warn error]]
            [liber.system :as system]
            [org.httpkit.server :as httpkit])
  )


(def system (atom nil))
(defn- add-shutdown-hook []
  (.addShutdownHook (Runtime/getRuntime) (Thread. #(swap! system stop))))

(defn -main [& args]
  (let [port 9090]
    (reset! system (system/create-system port))
    (swap! system start)
    ;;(swap! system stop)
    ))
