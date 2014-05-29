(defproject liber "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :plugins [[lein-midje "3.1.1"]
            ]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies
                   [[org.clojure/tools.namespace "0.2.4"]
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
                 [org.clojure/tools.logging "0.2.6"
                  :exclusions
                  [log4j/log4j
                   commons-logging/commons-logging
                   org.slf4j/slf4j-api
                   org.slf4j/slf4j-log4j12]]
                 [ch.qos.logback/logback-classic "1.1.2"]
                 [org.slf4j/log4j-over-slf4j "1.7.7"]

                 ;; database
                 [org.clojure/java.jdbc "0.3.3"]
                 [java-jdbc/dsl "0.1.0"]
                 [honeysql "0.4.3"]
                 [org.postgresql/postgresql "9.3-1100-jdbc41"]
                 [com.zaxxer/HikariCP "1.3.8"]
                 [ragtime/ragtime.core "0.3.7"]
                 [ragtime/ragtime.sql "0.3.7"]
                 ;; database test
                 [com.h2database/h2 "1.4.178"]

                 ;; web, rest
                 [org.clojure/data.json "0.2.4"]
                 [ring/ring-devel "1.2.2"]
                 [ring/ring-core "1.2.2"]
                 [ring/ring-json "0.3.1"]
                 [compojure "1.1.8"]
                 ;; 2.1.18 seems to have some issues
                 [http-kit "2.1.16"]
                 [cheshire "5.3.1"]
                 [ring-cors "0.1.2"]
                 [javax.servlet/servlet-api "2.5"]

                 ;; structure
                 [com.stuartsierra/component "0.2.1"]
                 [com.redbrainlabs/system-graph "0.2.0"]

                 ;; security
                 [commons-codec/commons-codec "1.9"]
                 [crypto-password "0.1.3"]

                 ;; api
                 [org.clojars.runa/clj-schema "0.9.4"]

                 [metosin/compojure-api "0.11.4"]
                 [metosin/ring-swagger-ui "2.0.16-2"]

                 [prismatic/schema "0.2.2"]
                 [prismatic/plumbing "0.3.0"]

                 ;; util
                 [clj-time "0.7.0"]
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
