(ns liber.core
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :refer [info]]
            [liber.system :as system]))

(def system-map)
(def handler)

(defn start-server [port]
  (let [s (system/create-prod-system port)
        started (component/start s)
        app (-> started :routes :app)]
    (alter-var-root #'system-map (constantly started))
    (alter-var-root #'handler (constantly app))
    )
  )

(defn stop-server []
  (component/stop system-map)
  )

(defn init-handler []
  ;; TODO should not start http-kit server
  (start-server 8083)
  )

(defn destroy-handler []
  (stop-server)
  )

(defn- add-shutdown-hook []
  (.addShutdownHook (Runtime/getRuntime) (Thread. (fn []
                                                    (info "Shutting down system")
                                                    (stop-server)
                                                    (info "System terminated")))))

(defn -main [& args]
  (let [port 9090]
    (add-shutdown-hook)
    (start-server port)
    ))
