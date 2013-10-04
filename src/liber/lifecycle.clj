(ns liber.lifecycle
  (:use [clojure.tools.logging :only (trace debug info warn error)])
  )

(defprotocol Lifecycle
  (start [this]
         "Begins operation of this component. Synchronous, does not return
         until the component is started. Returns an updated version of this
         component.")
  (stop [this]
        "Ceases operation of this component. Synchronous, does not return
        until the component is stopped. Returns an updated version of this
        component.")
  (started? [this]
            "Returns true if app is started")
  )
