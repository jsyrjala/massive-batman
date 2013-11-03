(ns liber.pubsub
  "Publish-subscribe API"
  (:require [liber.lifecycle :refer [Lifecycle]]
            [clojure.tools.logging :refer [trace debug info warn error]])
  )

(defprotocol PubSub
  (subscribe! [this subscriber sub-type sub-id callback])
  (unsubscribe! [this subscriber sub-type sub-id])
  (unsubscribe-all! [this subscriber])
  (broadcast! [this sub-type sub-id message]))

;; TODO move to internal ns?
(defrecord ClojurePubSub
  [channels]
  Lifecycle
  PubSub
  (start [this]
         (debug "ClojurePubSub starting")
         this)
  (stop [this]
        (debug "ClojurePubSub stopping")
        (reset! channels {})
        this)
  (subscribe! [this subscriber sub-type sub-id callback]
              (debug "subscribe!" sub-type sub-id)
              (swap! channels (fn [old]
                                (update-in old [sub-type sub-id]
                                           (fn [old-atom] (or old-atom (atom nil))))))
              (add-watch (get-in @channels [sub-type sub-id]) subscriber
                         (fn [key reference old-state new-state]
                           (callback subscriber new-state)))
              )
  (unsubscribe! [this subscriber sub-type sub-id]
                (debug "unsubscribe" sub-type sub-id)
                (remove-watch (get-in @channels [sub-type sub-id]) subscriber))
  (unsubscribe-all! [this subscriber]
                    ;; FIXME dumb implementation, loops over everything
                    (let [data @channels]
                      (doseq [sub-type data]
                        (doseq [sub-id (last sub-type)]
                          (remove-watch (last sub-id) subscriber)
                          ))))
  (broadcast! [this sub-type sub-id message]
              (debug "broadcast!")
              (when-let [item (get-in @channels [sub-type sub-id])]
                (reset! item message)) )
  )

(defn new-pubsub-server []
  (->ClojurePubSub (atom {})))
