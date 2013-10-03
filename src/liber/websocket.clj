(ns liber.websocket
  (:use org.httpkit.server)
)

(def channel-hub (atom {}))

(defn websocket-handler [request]
  (with-channel request channel
    ;; Store the channel somewhere, and use it to sent response to client when interesting event happened
    (swap! channel-hub assoc channel request)
    (on-receive channel (fn [msg] (doseq [ch (keys @channel-hub)]
                                    (send! ch (str "got: " msg))
                                    )))
    (on-close channel (fn [status]
                        ;; remove from hub when channel get closed
                        (swap! channel-hub dissoc channel)))
    ))


