;; The majority of this file originated from Component's example which
;; is MIT licensed by Stuart Sierra.
;; https://github.com/stuartsierra/component/blob/master/dev/examples.clj
(ns example
  (:require [com.redbrainlabs.system-graph :as system-graph]
            [com.stuartsierra.component :as component]
            [plumbing.core :refer [defnk fnk]]
            [schema
             [core :as s]
             [macros :as sm]]))

;;; Schema Configs

(def DBConfig {(s/required-key :host) String
               (s/required-key :port) Number})

(def ExampleComponentConfig {:foo s/Any :bar s/Any})

(def Config {:db DBConfig
             :example-component ExampleComponentConfig})

;;; Dummy functions to use in the examples

(defn connect-to-database [host port]
  (println ";; Opening database connection")
  (reify java.io.Closeable
    (close [_] (println ";; Closing database connection"))))

(defn execute-query [& _]
  (println ";; execute-query"))

(defn execute-insert [& _]
  (println ";; execute-insert"))

;; this constructor is a fnk so it can be used in a graph below
(defnk new-scheduler []
  (reify component/Lifecycle
    (start [this]
      (println ";; Starting scheduler")
      this)
    (stop [this]
      (println ";; Stopping scheduler")
      this)))


;;; Example database component

;; To define a component, define a Clojure record that implements the
;; `Lifecycle` protocol.

(defrecord Database [host port connection]
  ;; Implement the Lifecycle protocol
  component/Lifecycle

  (start [component]
    (println ";; Starting database")
    ;; In the 'start' method, initialize this component
    ;; and start it running. For example, connect to a
    ;; database, create thread pools, or initialize shared
    ;; state.
    (let [conn (connect-to-database host port)]
      ;; Return an updated version of the component with
      ;; the run-time state assoc'd in.
      (assoc component :connection conn)))

  (stop [component]
    (println ";; Stopping database")
    ;; In the 'stop' method, shut down the running
    ;; component and release any external resources it has
    ;; acquired.
    (.close connection)
    ;; Return the component, optionally modified.
    component))

;; Provide a constructor function that takes in the essential
;; configuration parameters of the component, leaving the
;; runtime state blank.

;; To play nicely with Graph this constructor function should
;; be a fnk. Note how we are using the nested-map destructuring
;; syntax provided by Graph. This is of course optional but
;; we've found it useful for configuration maps.

(defnk new-database [[:config db]]
  (s/validate DBConfig db)
  (map->Database db))

;; Define the functions implementing the behavior of the
;; component to take the component itself as an argument.

(defn get-user [database username]
  (execute-query (:connection database)
    "SELECT * FROM users WHERE username = ?"
    username))

(defn add-user [database username favorite-color]
  (execute-insert (:connection database)
    "INSERT INTO users (username, favorite_color)"
    username favorite-color))


;;; Second Example Component

;; Define other components in terms of the components on which they
;; depend.

(defrecord ExampleComponent [options cache database scheduler]
  component/Lifecycle

  (start [this]
    (println ";; Starting ExampleComponent")
    ;; In the 'start' method, a component may assume that its
    ;; dependencies are available and have already been started.
    (assoc this :admin (get-user database "admin")))

  (stop [this]
    (println ";; Stopping ExampleComponent")
    ;; Likewise, in the 'stop' method, a component may assume that its
    ;; dependencies will not be stopped until AFTER it is stopped.
    this))

;; Since we are using the dependency graph built up by fnk constructors
;; each constructor should have its needed deps declared in the signature.
;; In general, the constructor should not depend on other components
;; being started.

(defnk new-example-component [database scheduler [:config example-component]]
  (s/validate ExampleComponentConfig example-component)
  (map->ExampleComponent {:options example-component
                          :database database
                          :scheduler scheduler
                          :cache (atom {})}))


;;; Example System

;; Components are composed into systems. A system is a component which
;; knows how to start and stop other components.

;; Now the fun begins! Instead of specifying the dependency links manually
;; we can place all of our fnks into a single Graph and let it work out
;; the dependency graph for us.  As long as we have been consistent with
;; our naming of the components across the various fnks everything will
;; be injected for us.

(def example-graph
  {:database new-database
   :scheduler new-scheduler
   :example-component new-example-component})

;; You can optionaly compile the Graph into a fnk.
(def example-system (system-graph/eager-compile example-graph))

;; stub out a config loader
(defn load-config []
  {:config
   {:db {:host "dbhost.com" :port 123}
    :example-component {:foo 42 :bar 22}}})

;; Sample usage:
(comment

;; We can see the Schema our system returns and takes (if you're on the latest SNAPSHOT):
(s/fn-schema example-system)
;; (=> {:example-component Any, :scheduler Any, :db Any}
;;     {:config {:example-component Any, :db Any}})

;; BTW, once Schema has been fully integrated into fnk our
;; more detailed Schemas that we defined at the top of this file
;; will be able to be used giving us great documentation on what
;; a system requires for initialization.

;; Lets use the compiled fnk we made from above..
(def system (example-system (load-config)))
;;=> #'examples/system
(class system)
;;=> #'examples/system
;; com.redbrainlabs.system_graph.SystemGraph

(alter-var-root #'system component/start)
;; Starting database
;; Opening database connection
;; Starting scheduler
;; Starting ExampleComponent
;; execute-query
;;=> #com.redbrainlabs.system_graph.SystemGraph{ ... }

(alter-var-root #'system component/stop)
;; Stopping ExampleComponent
;; Stopping scheduler
;; Stopping database
;; Closing database connection
;;=> #com.redbrainlabs.system_graph.SystemGraph{ ... }
)

;; Local Variables:
;; clojure-defun-style-default-indent: t
;; End:
