(ns liber.route
  (:use [compojure.core :only [defroutes routes ANY GET OPTIONS]]
        [compojure.handler :only [api]]
        [clojure.tools.logging :only (trace debug info warn error)]
        liber.lifecycle
        )
  (:require [liber.resource :as resource]
            [ring.middleware.reload :as reload]
            [liber.websocket :as ws]
            [ring.middleware.cors :as cors]
            [ring.middleware.json :as ring-json]
            [cheshire.core :as json]
            [liber.middleware :as middleware]
            [liberator.dev :as liber-dev]
            )
  )

(defprotocol Routes
  (ring-handler [this]))

(defn json-response [body & [status]]
  {:status (or status 404) :body body})

(defn- create-routes [websocket pubsub-service]
  (routes
   ;; OPTIONS response is needed for CORS
   (OPTIONS ["*"] [] "OK")
   (ANY ["/ping"] [] resource/ping)

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
   (ANY ["/events"] [] (resource/events pubsub-service))
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

   (ANY ["*"] [] (json-response {:error "Unknown route"} 404))
  ))

(def request-counter (atom 0))

(defrecord RestRoutes [websocket pubsub-service]
  Lifecycle
  Routes
  (start [this] this)
  (stop [this] this)
  (ring-handler [this]
                (let [handler (create-routes websocket pubsub-service)]
                  (-> handler
                      (cors/wrap-cors :access-control-allow-origin #".*"
                                      :access-control-allow-headers "X-Requested-With, Content-Type, Origin, Referer, User-Agent, Accept"
                                      :access-control-allow-methods "OPTIONS, GET, POST, PUT, DELETE")
                      (liber-dev/wrap-trace :header :ui)
                      (ring-json/wrap-json-body {:keywords? true})
                      (ring-json/wrap-json-response {:pretty true})
                      (middleware/wrap-x-forwarded-for)
                      (middleware/wrap-exception-logging)
                      (middleware/wrap-request-logger request-counter)
                      ))
                ))

(defn new-rest-routes [websocket pubsub-service]
  (->RestRoutes websocket pubsub-service))
