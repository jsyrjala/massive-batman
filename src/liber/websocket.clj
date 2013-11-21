(ns liber.websocket
  (:require [cheshire.core :as cheshire]
            [liber.pubsub :as pubsub]
            [liber.database.events :as events]
            [org.httpkit.server :refer [send! with-channel on-receive on-close]]
            [com.stuartsierra.component :refer [Lifecycle]]
            [liber.api-schema :as schema]
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

(defn- new-event [ch event-service message]
  ;; TODO move validation to event-service?
  ;; validate existance of tracker
  ;; validate autorization to tracker
  (let [{:keys [event tracker_id data]} message
        result (schema/validate-convert message schema/new-single-event-schema schema/new-event-conversion)]
    (cond (:errors result) (send-json! ch {:validation-errors (:errors result)})
          :default (do
                     (events/create-event! event-service tracker_id (:data result))
                     (send-json! ch {:success "OK"})
                     ))))

(defn- parse-message [ch pubsub-service event-service msg]
  (let [data (decode-json msg)]
    (try
      (cond (:ping data) (ping ch pubsub-service data)
            (:subscribe data) (subscribe! ch pubsub-service (:ids data))
            (:unsubscribe data) (unsubscribe! ch pubsub-service (:ids data))
            (:event data) (new-event ch event-service data)
            :else (unsupported-message ch data)
            )
      (catch Exception e
        (error e "Error while handling WebSocket message")
        (send-json! ch {:error :internal-server-error}))
      )))

(defn- close-channel [ch pubsub-service status]
  (debug "websocket connection closed" status)
  (unsubscribe-all! ch pubsub-service))

(defrecord WebSocket [pubsub-service event-service]
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
                    (on-receive channel #(parse-message channel pubsub-service event-service %))
                    (on-close channel #(close-channel channel pubsub-service %))
                    ))))

(defn new-websocket [pubsub-service event-service]
  (->WebSocket pubsub-service event-service))
