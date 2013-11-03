(ns liber.resource
  (:require [liberator.core :refer [request-method-in handle-unauthorized]]
            [liberator.representation :refer [ring-response]]
            [clojure.tools.logging :refer [trace debug info warn error]]
            [liber.database.events :as events]
            [liberator.core :as liberator]
            [liberator.representation :as liberator-rep]
            [liber.lifecycle :refer [Lifecycle]]
            [liber.api-schema :as schema]
            [cheshire.core :as json]
            [clj-schema.validation :refer [validation-errors]]
            [liber.util :as util])
)

(def resource-defaults
  {:available-media-types ["application/json"]
   })

(defn- add-defaults [kvs]
  (merge resource-defaults (apply hash-map kvs)))


(defn- resource [& kvs]
    (fn [request] (liberator/run-resource request (add-defaults kvs))))

(defmacro defresource [name & kvs]
  (if (vector? (first kvs))
    (let [args (first kvs)
          kvs (rest kvs)]
      `(defn ~name [~@args]
         (fn [request#]
           (liberator/run-resource request# ~(add-defaults kvs)))))
    `(defn ~name [request#]
       (liberator/run-resource request# ~(add-defaults kvs)))))

(defn- json-response
  "Formats data map as JSON"
  [status data]
  (liberator-rep/ring-response {:status status
                                :body (json/generate-string data {:prettyPrint true})
                                  :header {:content-type "application/json"}
                              }))

(defn- validation-errors? [data schema]
  (info "do some validation" data)
  (let [errors (validation-errors schema data)]
      [(not (empty? errors)) {:validation-errors errors}]
    ))

(defprotocol EventResources
  (ping [this])
  ;; trackers
  (trackers [this])
  (tracker [this tracker-id])
  (tracker-events [this tracker-id])
  (tracker-sessions [this tracker-id])
  (tracker-users [this tracker-id])
  (tracker-groups [this tracker-id])
  (tracker-group [this tracker-id group-id])
  ;; events
  (events [this])
  (event [this event-id])
  ;; users
  (users [this])
  (user [this user-id])
  ;; auth-tokens
  (auth-tokens [this])
  (auth-token [this token] )
  ;; groups
  (groups [this])
  (group [this group-id] )
  (group-users [this group-id] )
  (group-trackers [this group-id] )
  ;; sessions
  (sessions [this])
  (session [this session-id] )
  (session-events [this session-id] )
  )

(defrecord JsonEventResources [event-service]
  Lifecycle
  EventResources
  (start [this] this)
  (stop [this] this)
  (ping [this] (resource :handle-ok {:ping :ok}))
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; trackers
  (trackers [this]
            (resource
             :handle-ok (fn [ctx]
                          (println "jee")
                          {:b "jee"}
                          )))

  (tracker [this tracker-id]
           (resource
            :handle-ok (fn [ctx]
                         (println tracker-id)
                         {:a tracker-id})))

  (tracker-events [this tracker-id] (resource))
  (tracker-sessions [this tracker-id] (resource))
  (tracker-users [this tracker-id] (resource))
  (tracker-groups [this tracker-id] (resource))
  (tracker-group [this tracker-id group-id] (resource))

  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; events
  (events [this]
          (resource
           :allowed-methods [:post]
           ;; check tracker exists
           ;; check valid checksum/password
           :authorized?
           :allowed? true
           :malformed? (fn [ctx]
                        (let [data (-> ctx :request :body)
                              errors (validation-errors schema/new-single-event-schema data)
                              converted-data (util/convert data schema/new-event-conversion)]
                          (debug errors (not (empty? errors)))
                          [(not (empty? errors)) {::validation-errors errors
                                                  ::data converted-data}]))
           :handle-malformed (fn [ctx]
                               (let [errors (ctx ::validation-errors)
                                     resp (json-response 400 {:error :malformed-data
                                                              :validation-errors errors} )]
                                 (debug "Malformed request" errors)
                                 resp))
           :post! (fn [ctx]
                    (let [data (-> ctx ::data)]
                      (info "new-event" data)
                      (let [{:keys [tracker_id]} data
                            new-event (events/create-event! event-service tracker_id data)]
                        {::created event} )))
           :handle-created (fn [ctx]
                             {:success "OK"
                              :created-event (ctx ::created)})
           ))

  (event [this event-id]
         (resource
          :allowed-methods [:get]
          ;; check authorization
          :exists? (fn [ctx]
                     ;;(let [event (get-event event-id)]
                     ;;  {::event event}
                     ;; )
                     )
          :handle-ok ::event))
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; users
  (users [this]
         (resource))
  (user [this user-id]
        (resource))
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; auth-tokens
  (auth-tokens [this]
         (resource))
  (auth-token [this user-id]
              (resource))
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; groups
  (groups [this]
          (resource))
  (group [this group-id]
         (resource))
  (group-users [this group-id]
               (resource))
  (group-trackers [this group-id]
                  (resource))
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; sessions
  (sessions [this]
            (resource))
  (session [this session-id]
           (resource))
  (session-events [this session-id]
                  (resource))


  )

(defn new-event-resources [event-service]
  (->JsonEventResources event-service)
  )

