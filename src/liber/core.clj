(ns liber.core
  (:require [com.stuartsierra.component :refer [start stop]]
            [liber.system :as system]))


(def system (atom nil))
(defn- add-shutdown-hook []
  (.addShutdownHook (Runtime/getRuntime) (Thread. #(swap! system stop))))

(defn -main [& args]
  (let [port 9090]
    (reset! system (system/create-system port))
    (swap! system start)
    ;;(swap! system stop)
    ))
