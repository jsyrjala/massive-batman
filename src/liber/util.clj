(ns liber.util
  (:require [clojure.tools.logging :refer [debug info warn error]]
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
  [data-map]
  (let [data (into {}
                   (filter
                    (fn [item]
                      (if item
                        (let [value (item 1)]
                          (cond (and (coll? value) (empty? value)) false
                                (= value nil) false
                                :else true))
                        nil)
                      ) data-map))]
    (if (empty? data)
      nil
      data)))


(defn dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(defn- get-func [func]
  (if (ifn? func)
    func
    (fn [_] func)))

(defn- updater [data key converters]
  (let [value (get-in data key)
        converter (get converters key ::notfound)]
    (if (or (nil? value) (= converter ::nofound))
      data
      (update-in data key (get-func converter)))))

(defn convert
  "Convert values in data map using functions in converters map.
Values in converters map are assumed to be either constants of 1-arity functions.
Example:
(def data {:a {:b 1
               :c 10
               :d 20
               :e {:i 42}}
           :x {:t 2}})

(def converters { {[:a :b] inc
                   [:a :c] identity
                   [:a :e :i] \"aa\"
                   [:a :d] dec}})

(convert data converters)
=> {:x {:t 2}, :a {:c 10, :b 2, :d 19, :e {:i \"aa\"}}}
"
  [data converters]
  (reduce #(updater %1 %2 converters) data (keys converters) ))
