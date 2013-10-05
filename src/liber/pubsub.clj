(ns liber.pubsub
  (:require [taoensso.carmine :as car :refer [wcar]])
  (:use liber.lifecycle
        [clojure.tools.logging :only (trace debug info warn error)])
)

(defprotocol PubSub
  (create-subscription! [this sub-type id callback] )
  (destroy-subscription! [this subscription])
  (broadcast! [this sub-type id message])
  )


(defrecord RedisServer [redis-spec]
  Lifecycle
  PubSub
  (start [this]
         (info "Start redis")
         (info "PING server, response:" (wcar redis-spec (car/ping)))
         (assoc this :redis redis-spec :subscriptions (atom {})))
  (stop [this]
        (info "Stop redis")
        ;; TODO kill existing subscriptions
        (wcar redis-spec
              (doseq [sub @(:subscriptions this)]
                (car/close-listener sub)
                )
              )
        (reset! (:subscriptions this) nil)
        (dissoc this :redis))
  (create-subscription! [this sub-type id callback]
                        (let [existing-subs (:subscriptions this)
                              sub-key (str sub-type "+" id)
                              new-subscription (wcar redis-spec (car/with-new-pubsub-listener
                                                                 {sub-key callback}
                                                                 (car/subscribe sub-key)))]
                          (swap! existing-subs assoc new-subscription)
                          new-subscription
                          )
                        )
  (destroy-subscription! [this subscription]
                         (swap! (:subscriptions this) dissoc subscription)
                         (wcar redis-spec (car/close-listener subscription))
                         )
  (broadcast! [this sub-type id message]
              (car/publish (str id) message)
              )

  )


(defn new-redis-server [redis-spec]
  (->RedisServer redis-spec))
