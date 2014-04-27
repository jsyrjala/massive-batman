(ns liber.handler
  (:require [compojure.api.sweet :refer
             [defapi swagger-ui swagger-docs swaggered context
              GET* POST* DELETE*]]
            [ring.swagger.schema :refer [field]]
            [ring.util.http-response :refer [ok not-found]]
            ;;[liber.websocket :as ws]
            [liber.domain :refer
             [Tracker NewTracker Session Event NewEvent User NewUser
              Group NewGroup]]
            [com.stuartsierra.component :as component]
            [clj-time.core :refer [now time-zone-for-id]]
            [clj-time.format :refer [formatter unparse]]
            [clojure.tools.logging :refer (trace debug info warn error)]
            )
  )

(def prefix "/api/v1-dev")

(def date-time-formatter (formatter "YYYY-MM-dd'T'HH:mm:ss.SSSZ"
                                    (time-zone-for-id "UTC")))

(def ^:dynamic
  ^{:doc ""}
  *database* )

;; put time formatting to middleware
(defn timestamp [] (unparse date-time-formatter (now)))

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
                       :server-software "CLJ-RTServer/0.0.1"
                       :time (timestamp)}))
            ))

  (swaggered
   "WebSocket"
   :description "WebSocket endpoints."
   (context "/api/v1-dev/websocket" []
            (GET* "/websocket" []
                  :summary "WebSocket endpoint"
                  (do
                    (println "wsocket")
                    ;;ws/handler
                    )
                  )))

  (swaggered
   "Users"
   :description "Login and logout. New user registration."
   (context "/api/v1-dev" []
            (GET* "/users" []
                  :summary "Get users"
                  :return [User]
                  (ok [{:result 1
                       :db (:connection *database*)}])
                  )
            (POST* "/users" []
                   :summary "Register a new user"
                   :body [new-user NewUser]
                   :return User
                   (ok ""))
            (GET* "/users/:user-id" []
                  :path-params [user-id :- Long]
                  :summary "Get user details"
                  :return User
                  (ok ""))
            (POST* "/auth-tokens" []
                   :summary "Login user"
                   :return [User]
                   (ok ""))
            (DELETE* "/auth-tokens" []
                     :summary "Logout user"
                     (ok ""))
            ))


  (swaggered
   "Trackers"
   :description "Query and configure tracking devices."
   (context "/api/v1-dev" []
            (GET* "/trackers" [:as req]
                  ;; :return [Tracker]
                  :summary "Fetch all trackers (visible for current user)."
                  (do
                  (println "XXXX" req)
                  (ok [{:result 1 :x (str req) }])))
            (GET* "/trackers/:tracker-id" []
                  :path-params [tracker-id :- Long]
                  :return Tracker
                  :summary "Fetch single tracker"
                  (ok tracker-id))
            (GET* "/trackers/:tracker-id/events" []
                  :path-params [tracker-id :- Long]
                  :return [Event]
                  :summary "Fetch events for tracker"
                  (ok tracker-id))
            (GET* "/trackers/:tracker-id/sessions" []
                  :path-params [tracker-id :- Long]
                  :return [Session]
                  :summary "Fetch tracking sessions for tracker"
                  (ok "ok"))
            (GET* "/trackers/:tracker-id/users" []
                  :path-params [tracker-id :- Long]
                  :return [User]
                  :summary "Fetch users related to a tracker. TODO"
                  (not-found "not implemented yet"))
            (GET* "/trackers/:tracker-id/groups" []
                  :path-params [tracker-id :- Long]
                  :return [Group]
                  :summary "Fetch groups related to a tracker. TODO"
                  (not-found "not implemented yet"))
            (POST* "/trackers/:tracker-id/groups" []
                   :path-params [tracker-id :- Long]
                   :return [Group]
                   :summary "Add group to a tracker. TODO"
                   (not-found "not implemented yet"))
            (DELETE* "/trackers/:tracker-id/groups/:group-id" []
                     :path-params [tracker-id :- Long
                                   group-id :- Long]
                     :summary "Delete a group from a tracker. TODO"
                     (not-found "not implemented yet"))
            ))

  (swaggered
   "Sessions"
   :description "Query tracking sessions."
   (context "/api/v1-dev" []
            (GET* "/sessions" []
                  :return [Session]
                  :summary "Get sessions"
                  (ok "ok"))
            (GET* "/sessions/:session-id" []
                  :path-params [session-id :- Long]
                  :return Session
                  :summary "Get session details."
                  (ok "ok"))
            (GET* "/sessions/:session-id/events" []
                  :path-params [session-id :- Long]
                  :return [Event]
                  :summary "Get events related to session"
                  (ok "ok"))
            )
   )

  (swaggered
   "Events"
   :description "Query and store events and location data."
   (context "/api/v1-dev" []
            (POST* "/events" []
                   :body [new-event NewEvent]
                   :summary "Store a new event"
                   (ok {:result new-event})
                   )
            (GET* "/event/:event-id" []
                  :path-params [event-id :- Long]
                  :return Event
                  :summary "Get event details"
                  (ok ""))
            ))

  (swaggered
   "Groups"
   :description "Query and manage user groups"
   (context "/api/v1-dev" []
            (GET* "/groups" []
                  :return [Group]
                  :summary "List groups. TODO"
                  (not-found "not implemented yet"))
            (POST* "/groups" []
                   :body [new-event NewGroup ]
                   :return [Group]
                   :summary "Add new group. TODO"
                   (not-found "not implemented yet"))
            (GET* "/groups/:group-id" []
                  :path-params [group-id :- Long]
                  :return Group
                  :summary "Get group details. TODO"
                  (not-found "not implemented yet"))
            (DELETE* "/groups/:group-id" []
                     :path-params [group-id :- Long]
                     :summary "Get group details. TODO"
                  (not-found "not implemented yet"))
            (GET* "/groups/:group-id/users" []
                  :path-params [group-id :- Long]
                  :return [User]
                  :summary "Get users that belong to a group. TODO"
                  (not-found "not implemented yet"))
            (GET* "/groups/:group-id/trackers" []
                  :path-params [group-id :- Long]
                  :return [Tracker]
                  :summary "Get trackers that belong to a group. TODO"
                  (not-found "not implemented yet"))
            ))

  )


(defn wrap-component [handler database]
  (fn wrap-component-req [req]
    (binding [*database* database]
      (handler req))))

(defrecord SwaggerRoutes [database]
  component/Lifecycle
  (start [this]
         (debug "Routes::start")
         (assoc this :app (-> app
                              (wrap-component database))))
  (stop [this]
        (debug "Routes::stop")
        (dissoc this :app))
  )


(defn new-routes []
  (map->SwaggerRoutes {}))
