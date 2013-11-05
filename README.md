# system-graph

'system-graph' is a Clojure library for using Prismatic's [Graph] in large system composition.

'Graph' provides a form of dependency injection which does all of the hard work. 'system-graph'
builds on top of this to allow Graphs to be compiled so that `SystemGraph`s are returned. These
`SystemGraph`s implement a `Lifecycle` protocol that enables the components of the system to be
started and shut down in a coordinated fashion. The beauty of using 'Graph' is that [the correct
order] of the components is implicitly defined by the Graph's [Fnks].

[Graph]: https://github.com/Prismatic/plumbing#graph-the-functional-swiss-army-knife
[Fnks]: https://github.com/Prismatic/plumbing#bring-on-defnk
[the correct order]: http://en.wikipedia.org/wiki/Topological_sorting

## Releases and Dependency Information

* Releases are published to [Clojars]

* Latest stable release is [0.1.0](https://clojars.org/com.redbrainlabs/system-graph/versions/0.1.0)

* [All releases](https://clojars.org/com.redbrainlabs/system-graph/versions)

[Leiningen] dependency information:

    [com.redbrainlabs/system-graph "0.1.0"]

[Maven] dependency information:

    <dependency>
      <groupId>com.redbrainlabs</groupId>
      <artifactId>system-graph</artifactId>
      <version>0.1.0</version>
    </dependency>

[Component]: https://github.com/stuartsierra/component
[Clojars]: http://clojars.org/
[Leiningen]: http://leiningen.org/
[Maven]: http://maven.apache.org/


## Introduction

While dependency injection and containers that manage `Lifecycle` is [nothing][DI] [new][pico] this
approach has been gaining traction in the Clojure community recently.  Rather than adding [yet][jig]
[another][teuta] `Lifecycle` protocol to the Clojure ecosystem 'system-graph' uses Stuart Sierra's
[Component] library. Please *[read the documentation for Component][Component docs]* for an overview of this
approach as well as some great advice on applying it within a Clojure application.

[DI]: http://www.martinfowler.com/articles/injection.html
[pico]: http://picocontainer.codehaus.org/
[jig]: https://github.com/juxt/jig#components
[teuta]: https://github.com/vmarcinko/teuta#component-lifecycle
[Component docs]: https://github.com/stuartsierra/component/blob/master/README.md#introduction

## Usage

For a detailed walkthrough see the [example system].  Below is a quick walkthrough for people already familiar with
[Graph] and [Component].

[example system]: https://github.com/RedBrainLabs/system-graph/blob/master/dev/example.clj

```clojure
(ns your-app
  (:require [com.redbrainlabs.system-graph :as system-graph]
            [com.stuartsierra.component :refer [Lifecycle] :as lifecycle]
            [plumbing.core :refer [defnk fnk]]))

```

### Create Components with fnk constructors

```clojure
(defrecord ComponentA [foo]
  Lifecycle
  (start [this]
    (println "Starting Component A")
    this)
  (stop [this]
    (println "Stopping Component A")
    this))

(defnk component-A [foo]
  (->ComponentA foo))

(defrecord ComponentB [component-a bar]
  Lifecycle
  (start [this]
    ;; By this time ComponentA will have already started
    (println "Starting Component B")
    this)
  (stop [this]
    ;; This will be called before ComponentA is stopped
    (println "Stopping Component B")
    this))

(defnk component-B [component-a bar]
  (map->ComponentB {:component-a component-a :bar bar}))

(defrecord ComponentC [component-a component-b baz]
  Lifecycle
  (start [this]
    (println "Starting Component C")
    this)
  (stop [this]
    (println "Stopping Component C")
    this))

(defnk component-c [component-a component-b baz]
  (map->ComponentB {:component-a component-a
                    :component-b component-b
                    :baz baz}))
```

### Create your system-graph

```clojure

;; notice how the keys of the Graph need to be the same as the
;; named parameters of the fnks used withing the graph.
(def system-graph
  {:component-a component-a
   :component-b component-b
   :component-c component-c})

;; You can optionally compile your system-graph into a fnk.
(def init-system (system-graph/eager-compile system-graph))
```

### Create a System from the system-graph

``` clojure
(def system (init-system {:foo 42 :bar 23 :baz 90}))
;; #com.redbrainlabs.system_graph.SystemGraph{:component-a #foo.ComponentA{:foo 42}, ...}

;; Alternatively, you can skip the compilation step and use system-graph/init-system:
(def system (system-graph/init-system system-graph {:foo 42 :bar 23 :baz 90}))
;; #com.redbrainlabs.system_graph.SystemGraph{:component-a #foo.ComponentA{:foo 42}, ...}
```

### Start and stop the System

```clojure
(alter-var-root #'system lifecycle/start)
;; Starting Component A
;; Starting Component B
;; Starting Component C
;; #com.redbrainlabs.system_graph.SystemGraph{:component-a #foo.ComponentA{:foo 42}, ...}

(alter-var-root #'system lifecycle/stop)
;; Stopping Component C
;; Stopping Component B
;; Stopping Component A
;; #com.redbrainlabs.system_graph.SystemGraph{:component-a #foo.ComponentA{:foo 42}, ...}

```

Again, please see the [example system] for a more detailed explanation.

## Tradeoffs

The nice thing about using Graph is that the dependency graph is computed automatically
using the fnks. This requires that you place all of a component's dependencies in the
fnk constructor.  It also requires that the names of your components be consistent across
Graphs and fnks.  Contrast this to 'Component' where you have to explicitly list your
component's dependencies out like so:

```clojure
   (component/using
     (example-component config-options)
     {:database  :db
      :scheduler :scheduler})
;;     ^          ^
;;     |          |
;;     |          \- Keys in the ExampleSystem record
;;     |
;;     \- Keys in the ExampleComponent record
```

While this may seem onerous it does allow you to list dependencies that aren't needed by
your component but need to start before it can start (e.g. components that require that
the database be put in a certain state). It also allows you to have context-specific names
within each component.

'system-graph' could provide an API that would allow for these use cases but for now it
does not.  It may in the future if we see enough need for those features.

## References / More Information

* [Graph: Abstractions for Structured Computation](http:blog.getprismatic.com/blog/2013/2/1/graph-abstractions-for-structured-computation)
* [Graph: composable production systems in Clojure](http://www.infoq.com/presentations/Graph-Clojure-Prismatic) (video)
 * [Slides from talk](https://github.com/strangeloop/strangeloop2012/raw/master/slides/sessions/Wolfe-Graph.pdf) (PDF)
* [Prismatic's "Graph" at Strange Loop](http://blog.getprismatic.com/blog/2012/10/1/prismatics-graph-at-strange-loop.html)
* [Component's Documentation][Component docs]
* [Clojure in the Large](http://www.infoq.com/presentations/Clojure-Large-scale-patterns-techniques) (video)
* [My Clojure Workflow, Reloaded](http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded)
* [reloaded](https://github.com/stuartsierra/reloaded) Leiningen template


## Change Log

* Version 0.1.1-SNAPSHOT (current Git master branch)
* Version [0.1.0] released on November 4, 2013

[0.1.0]: https://github.com/redbrianlabs/system-graph/tree/system-graph-0.1.0


## Copyright and License

Copyright © 2013 Ben Mabey and Red Brain Labs, All rights reserved.

The use and distribution terms for this software are covered by the
[Eclipse Public License 1.0] which can be found in the file
epl-v10.html at the root of this distribution. By using this software
in any fashion, you are agreeing to be bound by the terms of this
license. You must not remove this notice, or any other, from this
software.

[Eclipse Public License 1.0]: http://opensource.org/licenses/eclipse-1.0.php
