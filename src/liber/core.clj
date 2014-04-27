(ns liber.core
  (:gen-class)
  (:require [com.stuartsierra.component :refer [start stop]]
            [liber.system :as system]))

(comment
  (def system-map)
  (def handler)

  (defn init-handler []
    (let [s (system/ruuvi-system {:port 8083 :host "localhost"})
          started (component/start s)
          app (-> started :routes :app)]
      (alter-var-root #'system-map (constantly started))
      (alter-var-root #'handler (constantly app))
      )
    )

  (defn destroy-handler []
    (component/stop system-map)
    )
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
