(ns liber.websocket
  (:require [cheshire.core :as cheshire]
            [liber.pubsub :as pubsub])
  (:use org.httpkit.server
        liber.lifecycle
        [clojure.tools.logging :only (trace debug info warn error)])
)

(defprotocol AsyncRingHandler
  (ring-handler [this]))

;;;;;;;;;;

(defn- decode-json [msg] (cheshire/parse-string msg true))
(defn- encode-json [msg] (cheshire/generate-string msg))

(defn- send-json! [ch msg]
  (send! ch (encode-json msg)))

(defn- send-tracker! [ch tracker]
  (debug "Send tracker data to websocket")
  (send-json! ch tracker))

(defn- unsupported-message [ch data]
  (warn "unsupported message" data)
  (send-json! ch {:error "Unsupported message" :message data}))

(defn- send-tracker-update [channel message]
  (info "got tracker update" message)
  (send-json! channel message))

(defn- ping [ch pubsub-service msg]
  (let [value (:ping msg)]
    (info "got ping" msg)
    (send-json! ch {:pong value} )))

(defn- subscribe! [ch pubsub-service tracker-id]
  (info "subscribe tracker" tracker-id)
  (pubsub/subscribe! pubsub-service ch :tracker tracker-id send-tracker-update))

(defn- unsubscribe! [ch pubsub-service tracker-id]
  (info "unsubscribe tracker" tracker-id)
  (pubsub/unsubscribe! pubsub-service ch :tracker tracker-id))

(defn- unsubscribe-all! [ch pubsub-service]
  (info "unsubscribe all")
  (pubsub/unsubscribe-all! pubsub-service ch))

(defn- open-channel [ch pubsub-service request]
  (info "Connection started")
  ;; authenticate or close
  (send-json! ch {:hello "Server V1"}))

(defn- new-event [ch pubsub-service message]
  ;; TODO validate format
  ;; validate existance of tracker
  ;; validate autorization to tracker
  (info "new-event" message)
  (let [{:keys [event tracker_id data]} message]
    (pubsub/broadcast! pubsub-service :tracker (str tracker_id) message)
    )
  )

(defn- parse-message [ch pubsub-service msg]
  (let [data (decode-json msg)]
    (cond (:ping data) (ping ch pubsub-service data)
          (:subscribe data) (subscribe! ch pubsub-service (:ids data))
          (:unsubscribe data) (unsubscribe! ch pubsub-service (:ids data))
          (:event data) (new-event ch pubsub-service data)
          :else (unsupported-message ch data)
          )))

(defn- close-channel [ch pubsub-service status]
  (info "websocket connection closed" status)
  (unsubscribe-all! ch pubsub-service))

(defrecord WebSocket [pubsub-service]
  Lifecycle
  AsyncRingHandler
  (start [this] this)
  (stop [this] this)
  (ring-handler [this]
                (fn [request]
                  (with-channel request channel
                    (open-channel channel pubsub-service request)
                    (on-receive channel #(parse-message channel pubsub-service %))
                    (on-close channel #(close-channel channel pubsub-service %))
                    ))))

(defn new-websocket [pubsub-service]
  (->WebSocket pubsub-service))
