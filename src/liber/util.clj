(ns liber.util
  (:use [clojure.tools.logging :only (debug info warn error)]
        )
  )

(defn try-times*
  "Executes thunk. If an exception is thrown, will sleep and retry. At most n retries
  are done. If still some exception is thrown it is bubbled upwards in
  the call chain."
  [n sleepMsec thunk]
  (loop [n n]
    (if-let [result (try
                      [(thunk)]
                      (catch Exception e
                        (error e e)
                        (if (zero? n)
                          (throw e)
                          (Thread/sleep sleepMsec)
                          )))]
      (result 0)
      (recur (dec n)))))

(defmacro try-times
  "Executes body. If an exception is thrown, will sleep for a while and then retry. At most n retries
  are done. If still some exception is thrown it is bubbled upwards in
  the call chain."
  [n sleepMsec & body]
  `(try-times* ~n ~sleepMsec (fn [] ~@body)))

(defn remove-nils
  "Remove key-values that have nil values"
  [m]
  (into {} (filter #(not (nil? (get % 1))) m)))
