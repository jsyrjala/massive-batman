(ns liber.core
  (:gen-class))

(def handler)

(defn init-handler []
  (require '[liber.launch])
  (eval `(liber.launch/create-war-system))
  (alter-var-root #'handler (constantly (eval `liber.launch/handler))))

(defn stop-server []
  (eval `(liber.launch/stop-server)))

(defn start-server [port]
  (eval `(liber.launch/create-prod-system ~port)))

(defn destroy-handler []
  (stop-server))

(defn- add-shutdown-hook []
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. stop-server)))

(defn -main [& args]
  (require '[liber.launch])
  (let [port 9090]
    (add-shutdown-hook)
    (start-server port)
    ))
