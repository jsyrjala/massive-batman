(ns liber.middleware
  "Compojure middlewares"
  (:require [cheshire.core :as json]
            [clojure.string :as string]
            [clojure.tools.logging :refer [error info]]))

(defn wrap-exception-logging
  "Logs uncaught exceptions"
  [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e
        (error e "Handling request " (str req) " failed")
        {:status 500
         :body (json/generate-string {:error "Internal error"}
                                     {:prettyPrint true})
         :headers {"Content-Type" "application/json;charset=UTF-8"}
         }))))

(defn wrap-x-forwarded-for
  "Replace value of remote-addr -header with value of X-Forwarded-for -header if available."
  [handler]
  (fn [request]
    (if-let [xff (get-in request [:headers "x-forwarded-for"])]
      (handler (assoc request :remote-addr (last (string/split xff #"\s*,\s*"))))
      (handler request))))

(defn wrap-request-logger
  "Logs each incoming request"
  [app request-counter]
  (fn [request]
    (let [counter (swap! request-counter inc)
          request-method (:request-method request)
          uri (:uri request)
          query-params (:query-params request)
          start (System/currentTimeMillis)
          remote-addr (:remote-addr request)
          query (if (not (empty? query-params))
                  (str ":query-params "  query-params)
                  "") ]
      (info (str "REQUEST:" counter)
            remote-addr request-method uri query)
      (let [response (app request)
            duration (- (System/currentTimeMillis) start)
            status (:status response)]
        (info (str "RESPONSE:" counter)
              remote-addr
              status
              duration "msec")
        response) )))
