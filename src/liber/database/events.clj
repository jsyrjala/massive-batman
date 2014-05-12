(ns liber.database.events
  (:require [clj-time.core :as clj-time]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [debug]]
            [com.stuartsierra.component :refer [Lifecycle]]
            [liber.database :as db]
            [liber.database.dao :as dao]
            [liber.pubsub :as pubsub]))


(defn- current-timestamp [] (clj-time/now))

;;;;;;;;;;;;;;;

(defn- find-ext-values [data]
  (filter (fn [[k v]]
            (.startsWith (str (name k)) "X-"))
          data))

(defprotocol EventService
  "TODO document"
  (get-event [this id])
  (create-event! [this tracker event] "create-event! creates a new event")

  (create-user! [this user])
  (get-user [this id])
  (login [this user-login])

  (create-tracker! [this owner tracker])
  (get-tracker [this &{:keys [code id]}])
  (get-visible-trackers [this])
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
   (dao/get-event (db/datasource database) id))

  (create-event!
   [this tracker event]
   (jdbc/with-db-transaction
    [conn (db/datasource database)]
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
   (jdbc/with-db-transaction
    [conn (db/datasource database)]
    (dao/create-tracker! conn owner tracker)))

  ;; TODO transaction not needed?
  (get-tracker
   [this type id]
   (jdbc/with-db-transaction
    [conn (db/datasource database)]
    (condp = type
      :id (dao/get-tracker conn id)
      (dao/get-tracker-by-code conn id))))

  (get-visible-trackers
   [this]
   (dao/get-visible-trackers (db/datasource database)))

  (create-user!
   [this user]
   (jdbc/with-db-transaction
    [conn (db/datasource database)]
    (dao/create-user! conn user)))

  (get-user
   [this id]
   (dao/get-user (db/datasource database) id))

  (login
   [this user-login]
   (let [{:keys [username password]} user-login]
     (dao/login (db/datasource database) username password)))

  (create-group!
   [this owner group]
   (jdbc/with-db-transaction
    [conn (db/datasource database)]
    (dao/create-group! conn owner group)))
  )

(defn new-event-service [database pubsub-service]
  (->SqlEventService database pubsub-service))

