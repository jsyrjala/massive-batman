(ns liber.websocket
  (:require [cheshire.core :as cheshire])
  (:use org.httpkit.server
        liber.lifecycle
        [clojure.tools.logging :only (trace debug info warn error)])
)

(defprotocol AsyncRingHandler
  (ring-handler [this]))

;;;;;;;;;;

(defn- decode-json [msg] (cheshire/parse-string msg true))
(defn- encode-json [msg] (cheshire/generate-string msg))

(def channel-hub (atom {}))

(defn- send-tracker! [ch tracker]
  (debug "Send tracker data to websocket")
  (send! ch tracker)
  )

(defn- send-json! [ch msg]
  (send! ch (encode-json msg)))

(defn- unsupported-message [ch data]
  (warn "unsupported message" data)
  (send-json! ch {:error "Unsupported message" :message data}))

(defn- ping [ch msg]
  (let [value (:ping msg)]
    (info "got ping" msg)
    (send! ch (encode-json {:pong value} ))))

(defn- subscribe! [ch pubsub tracker-id]
  (info "subscribe" tracker-id)
  (swap! channel-hub (fn [channels]
                         (update-in channels
                                    [ch :trackers]
                                    (fn [tracker-ids]
                                      ;; TODO add listener
                                      (assoc tracker-ids tracker-id nil)
                                      )))))

(defn- unsubscribe! [ch pubsub tracker-id]
  (info "unsubscribe" tracker-id)
  (swap! channel-hub (fn [channels]
                       (update-in channels [ch :trackers]
                                  (fn [tracker-ids]
                                    ;; TODO remove listener
                                    (dissoc tracker-ids tracker-id)
                                    )))))

(defn- unsubscribe-all! [ch pubsub]
  (info "unsubscribe all")
  (swap! channel-hub (fn [channels]
                       (doseq [tracker-id (get-in channels [ch :trackers])]
                         ;; TODO remove listeners
                         )
                       (dissoc channels ch)
                       )))

(defn- open-channel [ch pubsub request]
  (info "Connection started")
  (swap! channel-hub assoc ch {:request request})
  (send-json! ch {:hello "Server V1"})
  )


(defn- new-event [ch pubsub data]
  (unsupported-message ch data))

(defn- parse-message [ch pubsub msg]
  (let [data (decode-json msg)]
    (cond (:ping data) (ping ch data)
          (:subscribe data) (subscribe! ch pubsub (:ids data))
          (:unsubscribe data) (unsubscribe! ch pubsub (:ids data))
          (:event data) (new-event ch pubsub data)
          :else (unsupported-message ch data)
          )
    )
  )

(defn- close-channel [ch pubsub status]
  (trace "websocket connection closed" status)
  (swap! channel-hub dissoc ch)
  )

(defrecord WebSocket [pubsub]
  Lifecycle
  AsyncRingHandler
  (start [this] this)
  (stop [this] this)
  (ring-handler [this]
                (fn [request]
                  (with-channel request channel
                    (open-channel channel pubsub request)
                    (on-receive channel #(parse-message channel pubsub %))
                    (on-close channel #(close-channel channel pubsub %))
                    ))))

(defn new-websocket [pubsub]
  (->WebSocket pubsub))
