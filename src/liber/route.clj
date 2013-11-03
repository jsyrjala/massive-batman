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

(defn- create-routes [websocket resources]
  (routes
   ;; OPTIONS response is needed for CORS
   (OPTIONS ["*"] [] "OK")
   (ANY ["/ping"] [] (resource/ping resources))

   ;; all trackers (visible for current user)
   (ANY ["/trackers"] [] resource/trackers)
   ;; tracker
   (ANY ["/trackers/:tracker-id" :tracker-id #".+"] [tracker-id]
        (resource/tracker resources tracker-id))
   ;; events for tracker
   (ANY ["/trackers/:tracker-id/events" :tracker-id #".+"] [tracker-id]
        (resource/tracker-events resources tracker-id))
   ;; sessions for tracker
   (ANY ["/trackers/:tracker-id/sessions" :tracker-id #".+"] [tracker-id]
        (resource/tracker-sessions resources tracker-id))
   ;; get users related to tracker
   (ANY ["/trackers/:tracker-id/users" :tracker-id #".+"] [tracker-id]
        (resource/tracker-users resources tracker-id))
   ;; add tracker to group
   (ANY ["/trackers/:tracker-id/groups" :tracker-id #".+"] [tracker-id]
        (resource/tracker-groups resources tracker-id))
   ;; remove tracker from group
   (ANY ["/trackers/:tracker-id/groups/:group-id"] [tracker-id group-id]
        (resource/tracker-group resources tracker-id group-id))

   ;; add new event
   (ANY ["/events"] [] (resource/events resources))
   (ANY ["/events/:event-id"] [event-id] (resource/event resources event-id))

   (ANY ["/users"] [] resource/users)
   (ANY ["/users/:user-id"] [user-id] (resource/user resources user-id))

   ;; create auth-token (login)
   (ANY ["/auth-tokens"] [] resource/auth-tokens )
   ;; delete token (logout)
   (ANY ["/auth-tokens/:token"] [token] (resource/auth-token resources token))

   (ANY ["/groups"] [] resource/groups)
   (ANY ["/groups/:group-id"] [group-id] (resource/group resources group-id))
   (ANY ["/groups/:group-id/users"] [group-id] (resource/group-users resources group-id))
   (ANY ["/groups/:group-id/trackers"] [group-id] (resource/group-trackers resources group-id))

   (ANY ["/sessions"] [] resource/sessions)
   (ANY ["/sessions/:session-id"] [session-id] (resource/session resources session-id))
   (ANY ["/sessions/:session-id/events"] [session-id] (resource/session-events resources session-id))

   (GET ["/websocket"] [] (ws/ring-handler websocket))

   (ANY ["*"] [] (json-response {:error "Unknown route"} 404))
  ))

(def request-counter (atom 0))

(defrecord RestRoutes [websocket resources]
  Lifecycle
  Routes
  (start [this] this)
  (stop [this] this)
  (ring-handler [this]
                (let [handler (create-routes websocket resources)]
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

(defn new-rest-routes [websocket resources]
  (->RestRoutes websocket resources))
