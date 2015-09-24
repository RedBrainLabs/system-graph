(defproject com.redbrainlabs/system-graph "0.2.1"
  :description "Graph + Component for large system composition"
  :url "https://github.com/redbrainlabs/system-graph"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[com.stuartsierra/component "0.2.1"]
                 [prismatic/plumbing "0.5.0"]]
  :profiles {:dev {:plugins [[lein-midje "3.1.3-RC2"]]
                   :dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  [org.clojure/clojure "1.5.1"]
                                  [midje "1.6-beta1"]]
                   :source-paths ["dev"]}}
  :aliases {"dumbrepl" ["trampoline" "run" "-m" "clojure.main/main"]})
