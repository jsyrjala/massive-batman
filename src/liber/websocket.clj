(ns liber.websocket
  (:use org.httpkit.server)
  (:use [clojure.tools.logging :only (trace debug info warn error)])
)

(def channel-hub (atom {}))

(defn websocket-handler [request]
  (with-channel request channel
    ;; Store the channel somewhere, and use it to sent response to client when interesting event happened
    (swap! channel-hub assoc channel request)
    (trace "websocket connection started" request)
    (on-receive channel (fn [msg] (doseq [ch (keys @channel-hub)]
                                    (send! ch (str "got: " msg))
                                    )))
    (on-close channel (fn [status]
                        (trace "websocket connection closed" request)
                        ;; remove from hub when channel get closed
                        (swap! channel-hub dissoc channel)))
    ))


