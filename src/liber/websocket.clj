(ns liber.websocket
  (:require [cheshire.core :as cheshire]
            [liber.pubsub :as pubsub]
            [org.httpkit.server :refer [send! with-channel on-receive on-close]]
            [liber.lifecycle :refer [Lifecycle]]
            [clojure.tools.logging :refer [trace debug info warn error]])
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
  (debug "got tracker update" message)
  (send-json! channel message))

(defn- ping [ch pubsub-service msg]
  (let [value (:ping msg)]
    (debug "got ping" msg)
    (send-json! ch {:pong value} )))

(defn- subscribe! [ch pubsub-service tracker-id]
  (debug "subscribe tracker" tracker-id)
  (pubsub/subscribe! pubsub-service ch :tracker tracker-id send-tracker-update))

(defn- unsubscribe! [ch pubsub-service tracker-id]
  (debug "unsubscribe tracker" tracker-id)
  (pubsub/unsubscribe! pubsub-service ch :tracker tracker-id))

(defn- unsubscribe-all! [ch pubsub-service]
  (debug "unsubscribe all")
  (pubsub/unsubscribe-all! pubsub-service ch))

(defn- open-channel [ch pubsub-service request]
  (debug "Connection started")
  ;; authenticate or close
  (send-json! ch {:hello "Server V1"}))

(defn- new-event [ch pubsub-service message]
  ;; TODO validate format
  ;; validate existance of tracker
  ;; validate autorization to tracker
  (debug "new-event" message)
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
  (debug "websocket connection closed" status)
  (unsubscribe-all! ch pubsub-service))

(defrecord WebSocket [pubsub-service]
  Lifecycle
  AsyncRingHandler
  (start [this]
         (debug "WebSocket handler starting")
         this)
  (stop [this]
        (debug "WebSocket handler stopping")
        this)
  (ring-handler [this]
                (fn [request]
                  (with-channel request channel
                    (open-channel channel pubsub-service request)
                    (on-receive channel #(parse-message channel pubsub-service %))
                    (on-close channel #(close-channel channel pubsub-service %))
                    ))))

(defn new-websocket [pubsub-service]
  (->WebSocket pubsub-service))
