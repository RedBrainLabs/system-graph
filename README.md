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

No releases have been made yet.  A release will be made once [Component] has made one.

* Releases will be published to [Clojars]

* Latest stable release is TODO_LINK

* All released versions TODO_LINK

[Leiningen] dependency information:

    [com.redbrainlabs/system-graph "0.1.0-SNAPSHOT"]

[Maven] dependency information:

    <dependency>
      <groupId>com.redbrainlabs</groupId>
      <artifactId>system-graph</artifactId>
      <version>0.1.0-SNAPSHOT</version>
    </dependency>

[Component]: https://github.com/stuartsierra/component
[Clojars]: http://clojars.org/
[Leiningen]: http://leiningen.org/
[Maven]: http://maven.apache.org/


## Introduction

While dependency injection and containers that manage `Lifecycle` is [nothing][DI] [new][pico] this
approach has been gaining traction in the Clojure community recently.  Rather than adding [yet][jig]
[another][teuta] `Lifecycle` protocol to the Clojure ecosystem 'system-graph' uses Stuart Siearra's
[Component] library. Please *[read the documentation for Component][Component docs]* for an overview of this
approach as well as some great advice on applying it within a Clojure application.


[DI]: http://www.martinfowler.com/articles/injection.html
[pico]: http://picocontainer.codehaus.org/
[jig]: https://github.com/juxt/jig#components
[teuta]: https://github.com/vmarcinko/teuta#component-lifecycle
[Component docs]: https://github.com/stuartsierra/component/blob/master/README.md#introduction

## Usage

TODO

## Change Log

* Version 0.1.0-SNAPSHOT



## Copyright and License

Copyright © 2013 Ben Mabey and Red Brain Labs, All rights reserved.

The use and distribution terms for this software are covered by the
[Eclipse Public License 1.0] which can be found in the file
epl-v10.html at the root of this distribution. By using this software
in any fashion, you are agreeing to be bound by the terms of this
license. You must not remove this notice, or any other, from this
software.

[Eclipse Public License 1.0]: http://opensource.org/licenses/eclipse-1.0.php
