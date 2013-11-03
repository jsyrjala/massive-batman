(ns liber.util-test
  (:use liber.util)
  (:use midje.sweet))

;; remove-nil-values
(fact "remove-nils returns nil as nil"
      (remove-nils nil) => nil)

(fact "remove-nils removes nils and empty values from top level"
      (remove-nils
       {nil :x :a false :b nil :c 2 :d {} :e [] :f [1 2] :g {:a1 1 :b2 {} }})
      => {nil :x :a false :c 2 :f [1 2] :g {:a1 1 :b2 {} }})


;; dissoc-in
(fact (dissoc-in nil [:a :b]) => nil
      (dissoc-in {:a 1} nil) => {:a 1}
      (dissoc-in {:a 1} []) => {:a 1}
      (dissoc-in {} [:a :b]) => {}
      (dissoc-in {:a {:b 1}} [:a :b]) => {}
      (dissoc-in {:a {:b 1 :c 2}} [:a :b]) => {:a {:c 2}}
      (dissoc-in {:a {:b 1 :c 2}} [:a :d]) => {:a {:b 1 :c 2}})

;; convert
(def data {:a {:b 1 :c 42 :d 7}})
(def converters {[:a :b] inc
                 [:a :c] dec
                 [:a :x] dec})

(fact (convert nil nil) => nil
      (convert data nil) => data
      (convert nil converters) => nil
      (convert data converters) => {:a {:b 2 :c 41 :d 7}})
