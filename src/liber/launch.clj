(ns liber.launch
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :refer [info]]
            [liber.system :as system]))

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

(defn create-war-system []
  (info "create war system")
  (start-server (system/create-war-system))
  (info "creae war ready"))

(defn create-prod-system [port]
  (start-server (system/create-prod-system port)))
