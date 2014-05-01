(ns liber.core
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :refer [info]]
            [liber.system :as system])
  (:gen-class))

(def system-map)
(def handler)

(defn start-server [sys]
  (let [started (component/start sys)
        app (-> started :routes :app)]
    (alter-var-root #'system-map (constantly started))
    (alter-var-root #'handler (constantly app))))

(defn stop-server []
  (info "Shutting down system")
  (component/stop system-map)
  (info "System terminated"))

(defn init-handler []
  (start-server (system/create-war-system)))

(defn destroy-handler []
  (stop-server))

(defn- add-shutdown-hook []
  (.addShutdownHook (Runtime/getRuntime) (Thread. (fn [] (stop-server) ))))

(defn -main [& args]
  (let [port 9090]
    (add-shutdown-hook)
    (start-server (system/create-prod-system port))))
