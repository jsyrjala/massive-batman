(ns liber.database.migration
  (:use [liber.lifecycle]
        [clojure.tools.logging :only (trace debug info warn error)]
        )
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.ddl :as ddl]
            [ragtime.sql.database :as ragtime-sql]
            [ragtime.core :as ragtime]
            )
  )

(defn- create-update-trigger [table]
  (let [name (str "tr_" table)])
  ;; TODO
  (str "CREATE TRIGGER " name " on table " table " on update set updated_at = now()"

  ))

(defn- drop-table [db table]
  (jdbc/db-do-commands
   db
   (ddl/drop-table table)))

(def migration-list
  [
   {:id "add-users-table"
    :up (fn [db]
          (jdbc/db-do-commands
           db
           (ddl/create-table :users
                             [:id "bigint auto_increment primary key"]
                             [:username "varchar(256) not null"]
                             [:password_has "varchar(256)"]
                             [:name "varchar(128)"]
                             [:email "varchar(256)"]
                             [:updated_at "timestamp not null default now()"]
                             [:created_at "timestamp not null default now()"]
                             )
           (ddl/create-index :ix_users_username :users [:username])))
    :down (fn [db] (drop-table db :users))}

   {:id "add-groups-table"
    :up (fn [db]
          (jdbc/db-do-commands
           db
           (ddl/create-table :groups
                             [:id "bigint auto_increment primary key"]
                             [:owner_id "bigint not null references users on delete cascade"]
                             [:name "varchar(128) not null"]
                             [:updated_at "timestamp not null default now()"]
                             [:created_at "timestamp not null default now()"]
                             )))
    :down (fn [db]
            (drop-table db :groups))}

   {:id "add-users-groups-table"
    :up (fn [db]
          (jdbc/db-do-commands
           db
           (ddl/create-table :users_groups
                             [:user_id "bigint not null references users on delete cascade"]
                             [:group_id "bigint not null references groups on delete cascade"]
                             [:name "varchar(128) not null"]
                             [:updated_at "timestamp not null default now()"]
                             [:created_at "timestamp not null default now()"]
                             ["primary key(user_id, group_id)"]
                             )
           (ddl/create-index :ix_users_groups :users_groups [:group_id])))
    :down (fn [db]
            (drop-table db :users_groups))}

   {:id "add-trackers-table"
    :up (fn [db]
          (jdbc/db-do-commands
           db
           (ddl/create-table :trackers
                             [:id "bigint auto_increment primary key"]
                             [:tracker_code "varchar(256) not null unique"]
                             [:latest_activity "timestamp"]
                             [:owner_id "bigint not null references users on delete cascade"]
                             [:public "boolean not null default false"]
                             [:shared_secret "varchar(64)"]
                             [:password "varchar(64)"]
                             [:name "varchar(256)"]
                             [:description "varchar(256)"]
                             [:updated_at "timestamp not null default now()"]
                             [:created_at "timestamp not null default now()"])
           (ddl/create-index :ix_trackers_name :trackers [:name]) ))
    :down (fn [db]
            (drop-table db :trackers))}

   {:id "add-event-sessions-table"
    :up (fn [db]
          (jdbc/db-do-commands
           db
           (ddl/create-table :event_sessions
                             [:id "bigint auto_increment primary key"]
                             [:tracker_id "bigint not null references trackers on delete cascade"]
                             [:latest_event_time "timestamp"]
                             [:first_event_time "timestamp"]
                             [:session_code "varchar(50) not null"]
                             [:updated_at "timestamp not null default now()"]
                             [:created_at "timestamp not null default now()"])
           (ddl/create-index :ix_event_sessions_code :event_sessions [:session_code :tracker_id] :unique)
           (ddl/create-index :ix_event_sessions_latest :event_sessions [:latest_event_time])
           ))
    :down (fn [db] (drop-table db :event_sessions))}

   {:id "add-events-table"
    :up (fn [db]
          (jdbc/db-do-commands
           db
           (ddl/create-table :events
                             [:id "bigint auto_increment primary key"]
                             [:tracker_id "bigint not null references trackers on delete cascade"]
                             [:event_session_id "bigint not null references event_sessions on delete cascade"]
                             [:event_time "timestamp not null"]
                             [:latitude "decimal"]
                             [:longitude "decimal"]
                             [:horizontal_accuracy "decimal(10,2)"]
                             [:vertical_accuracy "decimal(10,2)"]
                             [:satellite_count "integer"]
                             [:altitude "decimal"]
                             [:updated_at "timestamp not null default now()"]
                             [:created_at "timestamp not null default now()"]
                             )
           (ddl/create-index :ix_events_event_time :events [:tracker_id :event_time])
           (ddl/create-index :ix_events_created_at :events [:tracker_id :created_at])
           (ddl/create-index :ix_events_trackers_id :events [:tracker_id :event_session_id])
           ))
    :down (fn [db] (drop-table db :events))}

   {:id "add-event-annotations-table"
    :up (fn [db]
          (jdbc/db-do-commands
           db
           (ddl/create-table :event_annotations
                             [:id "bigint auto_increment primary key"]
                             [:event_id "bigint not null references events on delete cascade"]
                             [:annotation "varchar(256)"]
                             [:updated_at "timestamp not null default now()"]
                             [:created_at "timestamp not null default now()"]
                             )))
    :down (fn [db] (drop-table db :event_annotations))}

   {:id "add-event-extension-types-table"
    :up (fn [db]
          (jdbc/db-do-commands
           db
           (ddl/create-table :event_extension_types
                             [:id "bigint auto_increment primary key"]
                             [:name "varchar(256) not null"]
                             [:description "varchar(256)"]
                             [:updated_at "timestamp not null default now()"]
                             [:created_at "timestamp not null default now()"]
                             )))
    :down (fn [db] (drop-table db :event_extension_types))}

   {:id "add-event-extension-values-table"
    :up (fn [db]
          (jdbc/db-do-commands
           db
           (ddl/create-table :event_extension_values
                             [:id "bigint auto_increment primary key"]
                             [:event_id "bigint not null references events on delete cascade"]
                             [:event_extension_type_id "bigint not null references event_extension_types on delete cascade"]
                             [:value "varchar(256)"]
                             [:updated_at "timestamp not null default now()"]
                             [:created_at "timestamp not null default now()"]
                             )))
    :down (fn [db] (drop-table db :event_extension_values))}

   {:id "add-trackers-groups-table"
    :up (fn [db]
          (jdbc/db-do-commands
           db
           (ddl/create-table :trackers_groups
                             [:tracker_id "bigint not null references trackers on delete cascade"]
                             [:group_id "bigint not null references groups on delete cascade"]
                             [:updated_at "timestamp not null default now()"]
                             [:created_at "timestamp not null default now()"]
                             ["primary key(tracker_id, group_id)"]
                             )
           (ddl/create-index :ix_tracker_groups :trackers_groups [:group_id])))
    :down (fn [db] (drop-table db :trackers_groups))}
   ])

(defn- annotate
  "Add logging to migration functions."
  [migrations]
  (map (fn [entry]
         (let [id (:id entry)]
           {:id id
            :up (fn up [db]
                  (let [start (System/currentTimeMillis)]
                    (info "Execute migration" id)
                    ((:up entry) db)
                    (debug "Execution took" (- (System/currentTimeMillis) start) "msec"))
                  )
            :down (fn down [db]
                    (info "Rollback migration" id)
                    (let [start (System/currentTimeMillis)]
                      ((:down entry) db)
                      (debug "Rollback took" (- (System/currentTimeMillis) start) "msec") ))}))
       migrations))

(defprotocol Migrator
  (migrate-forward [this])
  (migrate-backward [this]))

(defrecord DatabaseMigrator [connection]
  Lifecycle
  Migrator
  (start [this] this)
  (stop [this] this)
  (migrate-forward [this]
                   (info "Execute database migrations")
                   (ragtime/migrate-all connection
                                        (annotate migration-list)))
  (migrate-backward [this]
                    (info "Rollback database migrations")
                    (ragtime/rollback-last connection 99999999999999))
  )

(defn create-migrator [db-spec]
  (let [connection (merge (ragtime-sql/->SqlDatabase) db-spec)]
    (->DatabaseMigrator connection)))
