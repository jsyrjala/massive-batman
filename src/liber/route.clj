(ns liber.route
  (:use [compojure.core :only [defroutes routes ANY GET]]
        [compojure.handler :only [api]]
        [clojure.tools.logging :only (trace debug info warn error)]
        liber.lifecycle
        )
  (:require [liber.resource :as resource]
            [ring.middleware.reload :as reload]
            [liber.websocket :as ws]
            )
  )

(defprotocol Routes
  (ring-handler [this]))

(defrecord RestRoutes [websocket]
  Lifecycle
  Routes
  (start [this] this)
  (stop [this] this)
  (ring-handler [this]
                (routes (ANY ["/ping"] [] resource/ping)

                        ;; all trackers (visible for current user)
                        (ANY ["/trackers"] [] resource/trackers)
                        ;; tracker
                        (ANY ["/trackers/:tracker-id" :tracker-id #".+"] [tracker-id] (resource/tracker tracker-id))
                        ;; events for tracker
                        (ANY ["/trackers/:tracker-id/events" :tracker-id #".+"] [tracker-id] (resource/tracker-events tracker-id))
                        ;; sessions for tracker
                        (ANY ["/trackers/:tracker-id/sessions" :tracker-id #".+"] [tracker-id] (resource/tracker-sessions tracker-id))
                        ;; get users related to tracker
                        (ANY ["/trackers/:tracker-id/users" :tracker-id #".+"] [tracker-id] (resource/tracker-users tracker-id))
                        ;; add tracker to group
                        (ANY ["/trackers/:tracker-id/groups" :tracker-id #".+"] [tracker-id] (resource/tracker-groups tracker-id))
                        ;; remove tracker from group
                        (ANY ["/trackers/:tracker-id/groups/:group-id"] [tracker-id group-id] (resource/tracker-group tracker-id group-id))

                        ;; add new event
                        (ANY ["/events"] [] (resource/events))
                        (ANY ["/events/:event-id"] [event-id] (resource/event event-id))

                        (ANY ["/users"] [] resource/users)
                        (ANY ["/users/:user-id"] [user-id] (resource/user user-id))

                        ;; create auth-token (login)
                        (ANY ["/auth-tokens"] [] resource/auth-tokens )
                        ;; delete token (logout)
                        (ANY ["/auth-tokens/:token"] [token] (resource/auth-token token))

                        (ANY ["/groups"] [] resource/groups)
                        (ANY ["/groups/:group-id"] [group-id] (resource/group group-id))
                        (ANY ["/groups/:group-id/users"] [group-id] (resource/group-users group-id))
                        (ANY ["/groups/:group-id/trackers"] [group-id] (resource/group-trackers group-id))

                        (ANY ["/sessions"] [] resource/sessions)
                        (ANY ["/sessions/:session-id"] [session-id] (resource/session [session-id]))
                        (ANY ["/sessions/:session-id/events"] [session-id] (resource/session-events [session-id]))

                        (GET ["/websocket"] [] (ws/ring-handler websocket))
                        (ANY ["*"] [] identity)
                        )))


(defn new-rest-routes [websocket]
  (->RestRoutes websocket))
