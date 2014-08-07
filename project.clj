(defproject liber "0.1.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :plugins [[lein-midje "3.1.1"]
            ]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies
                   [[org.clojure/tools.namespace "0.2.5"]
                    [org.clojure/java.classpath "0.2.2"]
                    [midje "1.6.3" :exclusions [org.clojure/clojure]]
                    ]
                   :plugins [[lein-ring "0.8.10"]]}
             :uberjar {:resource-paths ["swagger-ui"]
                       :aot [liber.core]}
             :provided {:dependencies [[javax.servlet/servlet-api "2.5"]]}}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.trace "0.7.8"]

                 ;; logging
                 [org.clojure/tools.logging "0.3.0"
                  :exclusions
                  [log4j/log4j
                   commons-logging/commons-logging
                   org.slf4j/slf4j-api
                   org.slf4j/slf4j-log4j12]]
                 [ch.qos.logback/logback-classic "1.1.2"]
                 [org.slf4j/log4j-over-slf4j "1.7.7"]

                 ;; database
                 [org.clojure/java.jdbc "0.3.5"]
                 [java-jdbc/dsl "0.1.0"]
                 [honeysql "0.4.3"]
                 [org.postgresql/postgresql "9.3-1100-jdbc41"]
                 ;; com.zaxxer/HikariCP-java6 works on java7
                 [com.zaxxer/HikariCP-java6 "2.0.1"]
                 [ragtime/ragtime.core "0.3.7"]
                 [ragtime/ragtime.sql "0.3.7"]
                 ;; database test
                 [com.h2database/h2 "1.4.181"]

                 ;; web, rest
                 [org.clojure/data.json "0.2.5"]
                 [ring/ring-devel "1.3.0"]
                 [ring/ring-core "1.3.0"]
                 [ring/ring-json "0.3.1"]
                 [compojure "1.1.8"]
                 ;; 2.1.17/2.1.18 have perf problems, https://github.com/http-kit/http-kit/issues/148
                 ;; 2.1.17 and earlier have https://github.com/http-kit/http-kit/issues/127
                 [http-kit "2.1.18"]
                 [cheshire "5.3.1"]
                 [ring-cors "0.1.4"]
                 [javax.servlet/servlet-api "2.5"]

                 ;; structure
                 [com.stuartsierra/component "0.2.1"]
                 [com.redbrainlabs/system-graph "0.2.0"]

                 ;; security
                 [commons-codec/commons-codec "1.9"]
                 [crypto-password "0.1.3"]

                 ;; api
                 [org.clojars.runa/clj-schema "0.9.4"]

                 [metosin/compojure-api "0.14.0"]
                 [metosin/ring-swagger-ui "2.0.17"]

                 [prismatic/schema "0.2.6"]
                 [prismatic/plumbing "0.3.3"]

                 ;; util
                 [clj-time "0.8.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [slingshot "0.10.3"]
                 ]
  :main liber.core
  :aot [liber.core]
  :jvm-opts ["-server" "-XX:+UseConcMarkSweepGC"]
  :ring {:init liber.core/init-handler
         :handler liber.core/handler
         :destroy liber.core/destroy-handler
         }
  ;;:manifest {"Main-Class" "liber.core"}

)
