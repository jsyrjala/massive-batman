(ns liber.handler
  (:require [compojure.api.sweet :refer
             [defapi swagger-ui swagger-docs swaggered context
              GET* POST* DELETE*]]
            [schema.core :refer [optional-key enum] :as schema]
            [ring.swagger.schema :refer [field]]
            [ring.util.http-response :refer
             [ok not-found! unauthorized unauthorized! bad-request!]]
            ;;[liber.websocket :as ws]
            [liber.domain :refer
             [Tracker NewTracker Session Event NewEvent User NewUser
              Group NewGroup UserLogin] :as domain]
            [liber.parse :refer [parse-timestamp] :as parse]
            [com.stuartsierra.component :as component]
            [clj-time.core :refer [now time-zone-for-id]]
            [clj-time.format :refer [formatter unparse]]
            [clojure.tools.logging :refer (trace debug info warn error)]
            [liber.database.events :as events]
            [liber.websocket :as ws]
            [liber.middleware :as middleware]
            [liber.security :as security]
            [clojure.set :refer [rename-keys]]
            [liber.util :as util]
            [slingshot.slingshot :refer [throw+ try+]]
            )
  (:import [org.joda.time DateTime])
  )

(def prefix "/api/v1-dev")

(def date-time-formatter (formatter "YYYY-MM-dd'T'HH:mm:ss.SSSZ"
                                    (time-zone-for-id "UTC")))

(def ^:dynamic
  ^{:doc ""}
  *event-service* )

(def ^:dynamic
  ^{:doc ""}
  *websocket* )

;; put time formatting to middleware
(defn timestamp [] (unparse date-time-formatter (now)))
;; TODO move to somewhere
(defn auth-new-event [event-service new-event]
  (let [tracker-code (-> new-event :tracker_code)
        tracker (events/get-tracker event-service :code tracker-code)
        auth (security/authentication-status new-event tracker :mac)]
    (auth :authenticated-tracker)
  ))



;; convert to data->domain multimethod
(defn user->domain [e]
  (-> e (select-keys [:id :name])))

(defn event->domain [e]
  (-> (select-keys e [:id
                      :tracker_id
                      :event_session_id
                      :event_time
                      :created_at
                      ])
      (rename-keys {:created_at :store_time})
      (assoc :location
        (-> e
            (select-keys [:latitude
                          :longitude
                          :horizontal_accuracy
                          :vertical_accuracy
                          :speed
                          :heading
                          :satellite_count
                          :altitude])
            (rename-keys {:horizontal_accuracy :accuracy})
            util/remove-nils))
      util/remove-nils))

;; TODO move to domain
(defmulti data->domain (fn [data-type data] data-type))

(defmethod data->domain :tracker [data-type tracker]
  (util/map-func (fn [tracker]
                   (-> tracker
                       (select-keys [:id :name :description
                                     :tracker_code :latest_activity
                                     :created_at])
                       (rename-keys {:created_at :created_on})))
                 tracker))
(defmethod data->domain :event [data-type event]
  (util/map-func event->domain event))

(defn process-new-event [event-service new-event]
  (let [{:keys [tracker_code]} new-event
        tracker (events/get-tracker event-service :code tracker_code)
        _ (when-not (auth-new-event event-service new-event)
            (unauthorized! {:error "Invalid tracker_code or mac"}))
        norm-new-event (try (domain/new-event->domain new-event)
                         (catch Exception e
                           (bad-request! {:error (.getMessage e)})))
        created (events/create-event! event-service tracker norm-new-event)]
    (ok {:created (-> created :id)})))

(defn process-login [event-service user-login]
  (let [user (try+
                 (events/login event-service user-login)
                 (catch :login-failed {}
                   (unauthorized! {:error "Invalid username or password"})))
        ]
    ;; TODO create auth token
    ;; store UUID to somewhere
    (ok {:user (user->domain user)
         :auth-token (util/uuid)})
  ))

(defn auth-tracker
  "Check if user is authorized to view tracker data"
  [request tracker-id]
  ;; TODO
  (info "auth-tracker" tracker-id " OK")
)

(defapi app
  (swagger-ui "/")
  (swagger-docs "/api/api-docs"
                :title "RuuviTracker REST API"
                :apiVersion "v1-dev"
                :description "RuuviTracker is an OpenSource Tracking System. See http://www.ruuvitracker.fi/ for more details."
                :termsOfServiceUrl nil
                :contact nil
                :license nil
                :licenseUrl nil
                )
  (swaggered
   "Meta"
   :description "Information about API"
   (context "/api/v1-dev" []
            (GET* "/ping" []
                  :summary "Get server version and time."
                  (ok {:ruuvi-tracker-protocol-version "1"
                       :server-software "RuuviTracker Server/0.1.0"
                       :time (timestamp)}))
            ))

  (swaggered
   "WebSocket"
   :description "WebSocket endpoints."
   (context "/api/v1-dev" []
            (GET* "/websocket" []
                  :summary "WebSocket endpoint"
                  (ws/ring-handler *websocket*)
                  )))

  (swaggered
   "Users"
   :description "Login and logout. New user registration."
   (context "/api/v1-dev" []
            (GET* "/users" []
                  :summary "Get users"
                  :return [User]
                  (ok [{:not-implemented :yet}])
                  )
            (POST* "/users" []
                   :summary "Register a new user"
                   :body [new-user NewUser]
                   :return User
                   (ok (-> (events/create-user! *event-service* new-user)
                           user->domain)))
            (GET* "/users/:user-id" []
                  :path-params [user-id :- Long]
                  :summary "Get user details"
                  :return User
                  (ok (-> (events/get-user *event-service* user-id)
                          user->domain)))
            (POST* "/auth-tokens" []
                   :summary "Login user"
                   :body [user-login UserLogin]
                   (process-login *event-service* user-login))
            (DELETE* "/auth-tokens" []
                     :summary "Logout user"
                     (ok ""))
            ))


  (swaggered
   "Trackers"
   :description "Query and configure tracking devices."
   (context "/api/v1-dev" []
            (GET* "/trackers" [:as req]
                  :return [Tracker]
                  :summary "Fetch all trackers (visible for current user)."
                  (ok
                   (data->domain :tracker
                                 (events/get-visible-trackers *event-service*))))

            (POST* "/trackers" []
                   :body [new-tracker NewTracker]
                   :return Tracker
                   :summary "Create a new Tracker"
                   (not-found! "not implemented yet"))

            (GET* "/trackers/:tracker-id" [:as request]
                  :path-params [tracker-id :- Long]
                  :return Tracker
                  :summary "Fetch single tracker"
                  (auth-tracker request tracker-id)
                  (not-found! "not implemented yet"))

            (GET* "/trackers/:tracker-id/events" [:as request]
                  :path-params [tracker-id :- Long]
                  :return [Event]
                  :summary "Fetch events for tracker"
                  :query-params [{maxResults :- (field Long {:description "Max number of result"})
                                  100}
                                 {eventTimeStart :- DateTime nil}
                                 {eventTimeEnd   :- DateTime nil}
                                 {storeTimeStart :- DateTime nil}
                                 {storeTimeEnd   :- DateTime nil}]

                  (auth-tracker request tracker-id)
                  (let [found-events
                        (events/search-events
                         *event-service*
                         {:tracker-id  tracker-id
                          :event-start eventTimeStart
                          :event-end   eventTimeEnd
                          :store-start storeTimeStart
                          :store-end   storeTimeEnd
                          :limit       maxResults})]
                    (ok (data->domain :event found-events)))
                  )
            (GET* "/trackers/:tracker-id/sessions" [:as request]
                  :path-params [tracker-id :- Long]
                  ;; TODO order (s/enum :latest :latestStored)
                  :return [Session]
                  :summary "Fetch tracking sessions for tracker"
                  (auth-tracker request tracker-id)
                  (not-found! "not implemented yet"))

            ;; TODO onko tarpeen?
            (GET* "/trackers/:tracker-id/events/:order" [:as request]
                  :path-params [tracker-id :- Long
                                order :- (enum :latest :latestStored)]
                  :query-params [{maxResults :- (field Long {:description "Max number of result"})
                                  nil}
                                 {eventTimeStart :- DateTime nil}
                                 {eventTimeEnd :- DateTime nil}
                                 {storeTimeStart :- DateTime nil}
                                 {storeTimeEnd :- DateTime nil}]
                  :return [Event]
                  :summary ""
                  (auth-tracker request tracker-id)
                  (not-found! "not implemented yet"))


            (GET* "/trackers/:tracker-id/users" [:as request]
                  :path-params [tracker-id :- Long]
                  :return [User]
                  :summary "Fetch users related to a tracker. TODO"
                  (auth-tracker request tracker-id)
                  (not-found! "not implemented yet"))

            (GET* "/trackers/:tracker-id/groups" [:as request]
                  :path-params [tracker-id :- Long]
                  :return [Group]
                  (auth-tracker request tracker-id)
                  :summary "Fetch groups related to a tracker. TODO"
                  (not-found! "not implemented yet"))

            (POST* "/trackers/:tracker-id/groups" [:as request]
                   :path-params [tracker-id :- Long]
                   :return [Group]
                   :summary "Add group to a tracker. TODO"
                   (auth-tracker request tracker-id)
                   (not-found! "not implemented yet"))

            (DELETE* "/trackers/:tracker-id/groups/:group-id" [:as request]
                     :path-params [tracker-id :- Long
                                   group-id :- Long]
                     :summary "Delete a group from a tracker. TODO"
                     (auth-tracker request tracker-id)
                     (not-found! "not implemented yet"))
            ))

  (swaggered
   "Sessions"
   :description "Query tracking sessions."
   (context "/api/v1-dev" []
            (GET* "/sessions" []
                  :return [Session]
                  :summary "Get sessions"
                  (not-found! "not implemented yet"))
            (GET* "/sessions/:session-id" []
                  :path-params [session-id :- Long]
                  :return Session
                  :summary "Get session details."
                  (not-found! "not implemented yet"))
            (GET* "/sessions/:session-id/events" []
                  :path-params [session-id :- Long]
                  :return [Event]
                  :summary "Get events related to session"
                  (not-found! "not implemented yet"))
            )
   )

  (swaggered
   "Events"
   :description "Query and store events and location data."
   (context "/api/v1-dev" []
            (POST* "/events" [:as request]
                   :body [new-event NewEvent]
                   :summary "Store a new event"
                   (process-new-event *event-service* new-event))
            (GET* "/events/:event-id" []
                  :path-params [event-id :- Long]
                  :return Event
                  :summary "Get event details"
                  (ok (-> (events/get-event *event-service* event-id)
                          event->domain)))
            ))

  (swaggered
   "Groups"
   :description "Query and manage user groups"
   (context "/api/v1-dev" []
            (GET* "/groups" []
                  :return [Group]
                  :summary "List groups. TODO"
                  (not-found! "not implemented yet"))
            (POST* "/groups" []
                   :body [new-event NewGroup ]
                   :return [Group]
                   :summary "Add new group. TODO"
                   (not-found! "not implemented yet"))
            (GET* "/groups/:group-id" []
                  :path-params [group-id :- Long]
                  :return Group
                  :summary "Get group details. TODO"
                  (not-found! "not implemented yet"))
            (DELETE* "/groups/:group-id" []
                     :path-params [group-id :- Long]
                     :summary "Get group details. TODO"
                  (not-found! "not implemented yet"))
            (GET* "/groups/:group-id/users" []
                  :path-params [group-id :- Long]
                  :return [User]
                  :summary "Get users that belong to a group. TODO"
                  (not-found! "not implemented yet"))
            (GET* "/groups/:group-id/trackers" []
                  :path-params [group-id :- Long]
                  :return [Tracker]
                  :summary "Get trackers that belong to a group. TODO"
                  (not-found! "not implemented yet"))
            ))

  )

(def request-counter (atom 0))

(defn wrap-component [handler event-service websocket]
  (fn wrap-component-req [req]
    (binding [*event-service* event-service
              *websocket* websocket]
      (handler req))))

(defrecord SwaggerRoutes [event-service websocket]
  component/Lifecycle
  (start [this]
         (debug "SwaggerRoutes starting")
         (assoc this :app (-> app
                              (wrap-component event-service websocket)
                              (middleware/wrap-request-logger request-counter))))
  (stop [this]
        (debug "SwaggerRoutes starting")
        (dissoc this :app))
  )


(defn new-routes [event-service websocket]
  (->SwaggerRoutes event-service websocket))
