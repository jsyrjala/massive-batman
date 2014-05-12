(ns liber.domain
  (:require [schema.core :refer [optional-key enum both]]
            [ring.swagger.schema :refer [defmodel field]]
            [liber.parse :as parse]
            [liber.util :as util]
            )
  (:import [org.joda.time DateTime])
)


(def tracker_code? (field (both String #"^[a-zA-Z0-9]+{4,30}$"){:description "TODO"}))
(def tracker_name? (field (both String #"^.{1,256}$")
                          {:description "TODO"}))

(defmodel Tracker
  {:id (field Long {:description "TODO"})
   :tracker_code tracker_code?
   (optional-key :name) tracker_name?
   (optional-key :latest_activity) (field DateTime {:description "Timestamp when this tracker last sent an event"})
   (optional-key :description) (field String {:description "Short description of tracker"})
   (optional-key :created_on) (field DateTime {:description "Time when tracker was created"})
   })

(defmodel NewTracker
  {:tracker_code tracker_code?
   :name tracker_name?
   :shared_secret (field String {:description "TODO"})
   :password (field String {:description "TODO"})
   (optional-key :description) (field String {:description "Short description of tracker"})
   })

(defmodel Session
  {:id (field Long {:description "TODO"})
   :tracker_id (field Long {:description "TODO"})
   :session_code (field String {:description "TODO"})
   :first_event_time (field DateTime {:description "Time of first activity related to session"})
   :latest_event_time (field DateTime {:description "Time of latest activity related to session"})
   })

;; these are in random order
;; needed because multilevel structures dont support (field)
(defmodel EventLocation
  {(optional-key :latitude) (field Double {:description "TODO"})
   (optional-key :longitude) (field Double {:description "TODO"})
   (optional-key :accuracy) (field Double {:description "TODO"})
   (optional-key :vertical_accuracy) (field Double {:description "TODO"})
   (optional-key :heading) (field Double {:description "Compass heading in degrees."})
   (optional-key :satellite_count) (field Long {:description "TODO"})
   (optional-key :battery) (field Double {:description "TODO"})
   (optional-key :speed) (field Double {:description "Current speed of tracker in m/s"})
   (optional-key :altitude) (field Double {:description "Altitude in meters from sea level."})
   (optional-key :temperature) (field Double {:description "In Celcius"})
   (optional-key :annotation) (field String {:description "Free form text that describes the event."})
   })

(defmodel Event
  {:id (field Long {:description "TODO"})
   :tracker_id (field Long {:description "TODO"})
   (optional-key :event_session_id) (field Long {:description "TODO"})
   :event_time (field DateTime {:description "TODO"})
   :store_time (field DateTime {:description "TODO"})
   ;;(optional-key :location) EventLocation
   (optional-key :location) EventLocation
   })

(def mac? (field (both String #"^[a-fA-F0-9]{40}$")
                 {:description ""}))

(defmodel NewEvent
  {:version (field (enum "1") {:description "Version number of Tracker API. Currently constant 1."})
   :tracker_code tracker_code?
   (optional-key :session_code) (field String {:description "Session identifier. Same for events that belong to same session. Typically something timestamp related."})
   (optional-key :time) (field String {:description "TODO"})
   (optional-key :nonce) (field String {:description "TODO"})
   ;; TODO support also decimal
   (optional-key :latitude) (field String {:description "TODO"})
   (optional-key :longitude) (field String {:description "TODO"})
   (optional-key :accuracy) (field Double {:description "TODO"})
   (optional-key :vertical_accuracy) (field Double {:description "TODO"})
   (optional-key :heading) (field Double {:description "TODO"})
   (optional-key :satellite_count) (field Long {:description "TODO"})
   (optional-key :battery) (field Double {:description "TODO"})
   (optional-key :speed) (field Double {:description "TODO"})
   (optional-key :altitude) (field Double {:description "TODO"})
   (optional-key :temperature) (field Double {:description "TODO"})
   (optional-key :annotation) (field String {:description "TODO"})
   (optional-key :mac) mac?
   }
  )

(defmodel User
  {:id (field Long {:description "TODO"})
   :name (field String {:description "TODO"})})

(defmodel NewUser
  {:name (field String {:description "TODO"})
   :username (field String {:description "TODO"})
   :password (field String {:description "TODO"})
   (optional-key :email) (field String {:description "TODO"})})

(defmodel UserLogin
  {:username (field String {:description "Username"})
   :password (field String {:description "Password"})})

(def group-name? (field String {:description "TODO"}))
(defmodel Group
  {:name group-name?
   :owner_id (field Long {:description "TODO"})})

(defmodel NewGroup
  {:name group-name?})

(def new-event-conversion
  {[:time] parse/parse-timestamp
   [:latitude] parse/parse-coordinate
   [:longitude] parse/parse-coordinate
   })

(defn new-event->domain
  "Convert various timestamp and lat/lon format in incoming new-event to standard format"
  [new-event]
  (util/convert new-event new-event-conversion))
