(ns liber.resource
  (:use [liberator.core :only [request-method-in handle-unauthorized]]
        [liberator.representation :only [ring-response]]
        [clojure.tools.logging :only (trace debug info warn error)]
        [liber.pubsub :as pubsub]
        )
  (:require [liberator.core :as liberator])
)

(def resource-defaults {:available-media-types ["application/json"]
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
;;+++++++++++

(def ping (resource :handle-ok {:ping :ok}))

;; trackers
(def trackers
  (resource
   :handle-ok (fn [ctx]
                (println "jee")
                {:b "jee"}
                )))


(defresource tracker [tracker-id]
  :handle-ok (fn [ctx]
               (println tracker-id)
               {:a tracker-id}))

(defresource tracker-events [tracker-id] )
(defresource tracker-sessions [tracker-id] )
(defresource tracker-users [tracker-id] )
(defresource tracker-groups [tracker-id] )
(defresource tracker-group [tracker-id group-id] )

;; events
(defresource events [pubsub-service]
  :allowed-methods [:post]
  ;; check tracker exists
  ;; check valid checksum
  :post! (fn [ctx]
           (let [data (-> ctx :request :body)]
             (info "new-event" data)
             (let [{:keys [tracker_id]} data]
               (pubsub/broadcast! pubsub-service :tracker tracker_id data)
               )))
  )
(defresource event [event-id]
  :allowed-methods [:get]
  ;; check authorization
  :exists? (fn [ctx]
             ;;(let [event (get-event event-id)]
             ;;  {::event event}
              ;; )
             )
  :handle-ok ::event)

;; users
(defresource users )
(defresource user [user-id] )

;; auth
(defresource auth-tokens )
(defresource auth-token [token] )

;; groups
(defresource groups )
(defresource group [group-id] )
(defresource group-users [group-id] )
(defresource group-trackers [group-id] )

;; sessions
(defresource sessions )
(defresource session [session-id] )
(defresource session-events [session-id] )

