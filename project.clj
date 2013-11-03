(defproject liber "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :plugins [[lein-midje "3.1.1"]
            ]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies
                   [[org.clojure/tools.namespace "0.2.4"]
                    [org.clojure/java.classpath "0.2.1"]
                    [midje "1.5.1" :exclusions [org.clojure/clojure]]
                    ]}
             :provided {:dependencies [[javax.servlet/servlet-api "2.5"]]}}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.trace "0.7.6"]

                 ;; logging
                 [org.clojure/tools.logging "0.2.6"
                  :exclusions
                  [log4j/log4j
                   commons-logging/commons-logging
                   org.slf4j/slf4j-api
                   org.slf4j/slf4j-log4j12]]
                 [ch.qos.logback/logback-classic "1.0.13"]
                 [org.slf4j/log4j-over-slf4j "1.7.5"]

                 ;; database
                 [org.clojure/java.jdbc "0.3.0-alpha5"]
                 [honeysql "0.4.2"]
                 [postgresql/postgresql "9.1-901.jdbc4"]
                 [com.jolbox/bonecp "0.7.1.RELEASE"]
                 [ragtime/ragtime.core "0.3.4"]
                 [ragtime/ragtime.sql "0.3.4"]
                 ;; database test
                 [com.h2database/h2 "1.3.174"]


                 ;; web, rest
                 [org.clojure/data.json "0.2.3"]
                 [ring/ring-devel "1.2.1"]
                 [ring/ring-core "1.2.1"]
                 [ring/ring-json "0.2.0"]
                 [compojure "1.1.6"]
                 [liberator "0.9.0"]
                 [http-kit "2.1.12"]
                 [cheshire "5.2.0"]
                 [ring-cors "0.1.0"]

                 ;; security
                 [commons-codec/commons-codec "1.7"]

                 ;; api
                 [org.clojars.runa/clj-schema "0.9.4"]

                 ;; util
                 [clj-time "0.6.0"]
                 [org.clojure/tools.cli "0.2.4"]

                 ]
  :main liber.core
  :jvm-opts ["-server" "-XX:+UseConcMarkSweepGC"]
)
