(ns liber.database.dao
  (:require [clj-time.coerce :as time-conv]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :refer [trace debug info warn]]
            [java-jdbc.sql :as sql]
            [honeysql.core :as honey]
            [honeysql.helpers :refer [select from where merge-where order-by ] :as honeyh]
            [liber.util :as util]
            [crypto.password.scrypt :as kdf]
            [clojure.set :refer [rename-keys]]
            [slingshot.slingshot :refer [throw+ try+]]))

(defn- current-sql-timestamp [] (java.sql.Timestamp. (System/currentTimeMillis)))

(defn to-domain-data [sql-map]
  (into (array-map)
        (map (fn mapper [[k v]]
               (cond (instance? java.sql.Timestamp v)
                     [k (time-conv/from-sql-time v)]
                     (instance? BigDecimal v)
                     [k (double v)]
                     :default [k v]))
             sql-map)))

(defn to-sql-data [domain-map]
  (into (array-map)
        (map (fn mapper [[k v]]
               (if (instance? org.joda.time.DateTime v)
                 [k (time-conv/to-sql-time v)]
                 [k v]))
             domain-map)))

(defn to-domain [m]
  (util/map-func
   #(-> %
        util/remove-nils
        to-domain-data) m))

(defn to-sql [m]
  (-> m to-sql-data))

(defn- get-row [conn table predv]
   (first
    (jdbc/query conn (sql/select * table predv))))

(defn- get-by-id [conn table id]
  (get-row conn table ["id = ?" id]))

(defn- log-clean
  "Hides sensitive data (e.g. password_hash) while logging"
  [sql-data]
  (if (sql-data :password_hash)
    (assoc sql-data :password_hash "<secret>")
    sql-data))

(defmulti db->data (fn [data-type data] data-type))

(defmethod db->data :default [data-type data]
  (util/map-func (fn [x]
                   (-> x to-domain)
                   ) data))

(defn- insert! [conn table data]
  (let [sql-data (to-sql-data data)
        _   (trace "insert!" table (log-clean sql-data))
        row (first (jdbc/insert! conn table sql-data))
        id-keys [(keyword "scope_identity()")
                 (keyword "SCOPE_IDENTITY()")
                 :id]
        id (first (filter identity (map row id-keys)))]
    ;; return value for H2 is just {:scope_identity() <id>}
    ;; so make a query to fetch full row
    (to-domain (get-by-id conn table id))))

(defn- update! [conn table new-values where-values])

;; TODO move to security.clj?
(defn hash-password
  "Create password hash using scrypt algorithm"
  [password]
  (kdf/encrypt password))

(defn password-matches?
  [input-password stored-password]
  (kdf/check input-password stored-password))

;; users
(defn create-user!
  [conn user]
  (let [password (:password user)
        db-user (-> (select-keys user [:username :email :name])
                    (assoc :password_hash (hash-password password)))
        new-user (insert! conn :users db-user)]
    (dissoc new-user :password_hash)))

(defn get-user [conn id]
  (to-domain (get-by-id conn :users id)))

(defn get-user-by-username [conn username]
  (to-domain (get-row conn :users ["username = ?" username])))

(defn login [conn username password]
  (let [user (get-user-by-username conn username)]
    (when-not user
      (throw+ {:login-failed :user-not-found}))
    (when-not (password-matches? password (:password_hash user))
      (throw+ {:login-failed :invalid-password}))
    user))

(defn create-group!
  [conn owner group]
  (let [owner-id (or (:id owner) owner)
        db-group (assoc (select-keys group [:name])
                   :owner_id owner-id)
        row (insert! conn :groups db-group)]
    row))

;; trackers
(defn create-tracker! [conn owner tracker]
  (let [owner-id (or (:id owner) owner)
        db-tracker (assoc (select-keys tracker [:owner_id :tracker_code :name :password
                                                :shared_secret
                                                :description :public])
                     :owner_id owner-id)]
     (insert! conn :trackers db-tracker)))

(defn get-visible-trackers [conn]
  ;; TODO visibility check
  (let [data (jdbc/query conn (sql/select * :trackers))]
    (db->data :tracker data)))

(defn get-tracker [conn id]
  (get-by-id conn :trackers id))

(defn get-tracker-by-code [conn tracker-code]
  (get-row conn :trackers ["tracker_code = ?" tracker-code]))

;; add triggers to update updated_at fields to every table
(defn update-tracker-activity! [conn tracker-id timestamp]
  ;; TODO setting to event-time is correct?
  (jdbc/execute! conn
                 ["update trackers set latest_activity = greatest(?, latest_activity), updated_at = ? where id = ?"
                  (time-conv/to-sql-time timestamp) (current-sql-timestamp) tracker-id] ))

;; sessions
(defn create-session! [conn tracker-id session-code timestamp]
  (insert! conn :event_sessions {:tracker_id tracker-id
                                 :session_code session-code
                                 :first_event_time timestamp
                                 :latest_event_time timestamp}))
(defn get-session [conn id]
  (get-by-id conn :event_sessions id))

(defn get-event-session-for-code [conn tracker-id session-code]
  (get-row conn
           :event_sessions
           ["tracker_id = ? and session_code = ?"
            tracker-id session-code]))

(defn update-session-activity! [conn session-id timestamp]
   (jdbc/execute! conn
                  ["update event_sessions set latest_event_time = greatest(?, latest_event_time), updated_at = ? where id = ?"
                  (time-conv/to-sql-time timestamp) (current-sql-timestamp) session-id] ))

(defn get-or-create-session! [conn tracker-id session-code timestamp]
  ;; there may be a burst of events that start a session, than can
  ;; cause duplicate key errors
  (util/try-times
   3 50
   (let [existing-session (get-event-session-for-code conn tracker-id session-code)
         session-id (:id existing-session)]
     (if existing-session
       (do
         (update-session-activity! conn session-id timestamp)
         existing-session)
       (create-session! conn tracker-id session-code timestamp)
       ))))

;; events
(defn get-event [conn id]
  (to-domain (get-by-id conn :events id)))

;; TODO -> util
(defn and-where [query pred condition]
  (if pred
    (merge-where query condition)
    query))

(defn search-events
  ""
  [conn criteria]
  (let [criteria (to-sql criteria)
        {:keys [tracker-id
                session-id
                event-start
                event-end
                store-start
                store-end
                order-by
                offset
                limit
                order
                ]} criteria
        query
        (-> (select :*)
            (from :events)
            (and-where tracker-id  [:=  :tracker_id tracker-id])
            (and-where session-id  [:=  :event_session_id session-id])
            (and-where event-start [:>= :event_time event-start])
            (and-where event-end   [:<= :event_time event-end])
            (and-where store-start [:>= :created_at store-start])
            (and-where store-end   [:<= :created_at store-end])
            (honeyh/limit limit)
            (honeyh/offset (or offset 0))
            (honeyh/order-by [(or order :event_time) :desc])
            (honey/format)
            )]
    (-> (jdbc/query conn query)
        to-domain)
  ))

;; extension types and values
(defn get-ext-type [conn type]
  (get-row conn :event_extension_types ["name = ?" (str (name type)) ] ))

(defn create-ext-type! [conn type & [description]]
  (insert! conn :event_extension_types {:name (str (name type)) :description description}))

(defn get-or-create-ext-type! [conn name & description]
  (util/try-times
   3 50
   (let [existing-type (get-ext-type conn name)]
     (if existing-type
       existing-type
       (create-ext-type! conn name description)))))

(defn create-ext-value! [conn event key value]
  (let [event-id (:id event event)
        ext-type (get-or-create-ext-type! conn key)
        ext-type-id (:id ext-type)]
    (insert! conn :event_extension_values
             {:event_id event-id
              :event_extension_type_id ext-type-id
              :value value})))

(defn create-annotation! [conn event text]
  (let [event-id (:id event event)]
  (insert! conn :event_annotations {:event_id event-id
                                    :annotation text})))

(defn create-event! [conn tracker-id session-id event]
  (let [db-event (select-keys event [:event_time
                                     :latitude
                                     :longitude
                                     :horizontal_accuracy
                                     :vertical_accuracy
                                     :speed
                                     :heading
                                     :satellite_count
                                     :altitude])
        db-event (assoc db-event
                   :tracker_id tracker-id
                   :event_session_id session-id)]
    (insert! conn :events db-event)))

