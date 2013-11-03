(ns liber.database.events
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
            [liber.util :as util]
            [liber.database :as db]
            [liber.pubsub :as pubsub]
            [liber.database.dao :as dao]
            [clj-time.core :as clj-time]
            )
  (:use [clojure.tools.logging :only (debug info warn error)]
        [liber.lifecycle]
        )
  )


(defn- current-timestamp [] (clj-time/now))

;;;;;;;;;;;;;;;

(defn- find-ext-values [data]
  (filter (fn [[k v]]
            (.startsWith (str (name k)) "X-"))
          data))

(defprotocol EventService
  (get-event [this id])
  (create-event! [this tracker event])
  (create-user! [this user])
  (create-tracker! [this owner tracker])
  (create-group! [this owner group]))

(defrecord SqlEventService [database pubsub-service]
  EventService
  Lifecycle
  (start [this]
         (debug "SqlEventService starting")
         this)
  (stop [this]
        (debug "SqlEventService stopping")
        this)

  (get-event
   [this id]
   (dao/get-tracker (db/datasource database) id))

  (create-event!
   [this tracker event]
   (jdbc/db-transaction
    [conn (db/datasource database)]
    (debug "creating event" event)
    ;; TODO move this stuff to dao
    (let [store-time (current-timestamp)
          event-time (get event :event_time store-time)
          event (assoc event :event_time event-time)
          tracker-id (or (:id tracker tracker))
          session-code (:session_code event "default")
          session (dao/get-or-create-session! conn tracker-id session-code event-time)
          new-event (dao/create-event! conn tracker-id (:id session) event)
          ext-values (find-ext-values event)
          annotation (:annotation event)]

      (dorun
       (map (fn [[k v]]
              (dao/create-ext-value! conn new-event k v))
            ext-values))

      (when annotation
        (dao/create-annotation! conn new-event annotation))

      (dao/update-tracker-activity! conn tracker-id event-time)
      ;; TODO convert event to structural, API format
      (pubsub/broadcast! pubsub-service :tracker tracker-id event)
      new-event
      )))

  (create-tracker!
   [this owner tracker]
   (jdbc/db-transaction
    [conn (db/datasource database)]
    (dao/create-tracker! conn owner tracker)))

  (create-user!
   [this user]
   (jdbc/db-transaction
    [conn (db/datasource database)]
    (dao/create-user! conn user)))

  (create-group!
   [this owner group]
   (jdbc/db-transaction
    [conn (db/datasource database)]
    (dao/create-group! conn owner group)))
  )

(defn new-event-service [database pubsub-service]
  (->SqlEventService database pubsub-service))

