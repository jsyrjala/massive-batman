(ns liber.core
  (:require [clojure.tools.cli :as cli])
  (:import [java.io File])
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

(def cli-options
  [["-p" "--port PORT" "Port number"
    :default 9091
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-c" "--config FILE" "Configuration file (TODO)"
    :parse-fn #(File. %)
    :validate [#(and (.exists %) (.isFile %) (.canRead %))
               "Configuration file must exist and must be readable"]]
   ["-h" "--help"]
   ])

(defn- print-help [opts]
  (println "RuuviTracker server")
  (println "See http://wiki.ruuvitracker.fi/ for more information")
  (println (opts :summary)))

(defn- print-errors [opts]
  (binding [*out* *err*]
    (println "RuuviTracker server start failed. Try option --help to get help.")
    (println (opts :errors))))

(defn -main [& args]
  (let [opts (cli/parse-opts args cli-options)]
    (when (-> opts :options :help)
      (print-help opts)
      (System/exit 0))
    (when (-> opts :errors)
      (print-errors opts)
      (System/exit 1))
    (let [{:keys [port]} (-> opts :options) ]
      (require '[liber.launch])
      (add-shutdown-hook)
      (start-server port)
      )))
