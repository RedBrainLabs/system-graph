(defproject com.redbrainlabs/system-graph "0.3.0"
  :description "Graph + Component for large system composition"
  :url "https://github.com/redbrainlabs/system-graph"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[com.stuartsierra/component "0.3.0"]
                 [prismatic/plumbing "0.5.0"]]
  :profiles {:dev {:plugins [[lein-midje "3.1.3-RC2"]]
                   :dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  [org.clojure/clojure "1.7.0"]
                                  [midje "1.7.0"]]
                   :source-paths ["dev"]}}
  :aliases {"dumbrepl" ["trampoline" "run" "-m" "clojure.main/main"]})
