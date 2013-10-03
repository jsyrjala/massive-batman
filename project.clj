(defproject liber "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [http-kit "2.1.10"]
                 [liberator "0.9.0"]
                 [org.clojure/tools.trace "0.7.3"]
                 [org.clojure/tools.logging "0.2.6"
                  :exclusions
                  [log4j/log4j
                   commons-logging/commons-logging
                   org.slf4j/slf4j-api
                   org.slf4j/slf4j-log4j12]]
                 [ch.qos.logback/logback-classic "1.0.13"]
                 [org.slf4j/log4j-over-slf4j "1.7.5"]

                 [org.clojure/data.json "0.2.3"]
                 [ring/ring-devel "1.2.0"]
                 [ring/ring-core "1.2.0"]
                 [ring/ring-json "0.2.0"]
                 [compojure "1.1.5"]]
  :main liber.core
)
