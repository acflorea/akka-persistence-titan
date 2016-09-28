# An Akka Persistence Plugin for Titan graph

A replicated [Akka Persistence](http://doc.akka.io/docs/akka/current/scala/persistence.html) journal backed by [Titan](http://titan.thinkaurelius.com/).

## Prerequisites

### Release

| Technology | Version                          |
| :--------: | -------------------------------- |
| Plugin     | [<img src="https://img.shields.io/maven-central/v/com.github.acflorea/akka-persistence-titan_2.11.svg?label=latest%20release%20for%202.11"/>](http://search.maven.org/#search%7cga%7c1%7cg%3a%22com.github.acflorea%22a%3a%22akka-persistence-titan_2.11%22)|
| Scala      | 2.11.7                           |
| Akka       | 2.4.0 or higher                  |
| Titan      | 1.0.0                            |

### Snapshot

| Technology | Version                          |
| :--------: | -------------------------------- |
| Plugin     | [<img src="https://img.shields.io/badge/latest%20snapshot%20for%202.11-1.0.1--SNAPSHOT-blue.svg"/>](https://oss.sonatype.org/content/repositories/snapshots/com/github/acflorea/akka-persistence-titan_2.11/1.0.1-SNAPSHOT/)
| Scala      | 2.11.7                           |
| Akka       | 2.4.0 or higher                  |
| Titan      | 1.0.0                            |

## Installation

### SBT

#### Release

```scala
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/releases"

libraryDependencies ++= Seq(
  "com.github.acflorea" %% "akka-persistence-titan"  % "1.0.0" % "compile")
```

#### Snapshot

```scala
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  "com.github.acflorea" %% "akka-persistence-titan"  % "1.0.1-SNAPSHOT" % "compile")
```

### Maven

#### Release

```XML
// Scala 2.11.7
<dependency>
    <groupId>com.github.acflorea</groupId>
    <artifactId>akka-persistence-titan_2.11</artifactId>
    <version>0.7.6</version>
</dependency>
```

#### Snapshot

```XML
// Scala 2.11.7
<dependency>
    <groupId>com.github.acflorea</groupId>
    <artifactId>akka-persistence-titan_2.11</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Build Locally

You can build and install the plugin to your local Ivy cache. This requires sbt 0.13.8 or above.

```scala
sbt publishLocal
```

<br/>It can then be included as dependency:

```scala
libraryDependencies += "com.github.acflorea" %% "akka-persistence-titan" % "1.0.1-SNAPSHOT"
```

## Configuration Example

```Roboconf

# Give enough time for embedded Cassandra to warm up
akka.test.single-expect-default = "30s"

akka.persistence.journal.plugin = "titan-journal"
titan-journal {

  # FQCN of the titan journal plugin
  class = "akka.persistence.titan.journal.TitanJournal"

  # Give enough time for embedded Cassandra to warm up
  circuit-breaker.call-timeout = "30s"
  circuit-breaker.reset-timeout = "30s"

  graph {
    # Titan
    # ~~~~~
    storage.backend = "embeddedcassandra"
    storage.conf-file = "cassandra/cassandra.yaml"
    storage.cassandra.keyspace = "akkajournal"

    # ElasticSearch
    # ~~~~~
    index.search.backend = "elasticsearch"
    index.search.elasticsearch.client-only = false
    index.search.elasticsearch.local-mode = true
    index.search.directory = "../db/es"

  }

}

akka.persistence.snapshot-store.plugin = "titan-snapshot-store"
titan-snapshot-store {

  # FQCN of the cassandra journal plugin
  class = "akka.persistence.titan.snapshot.TitanSnapshotStore"

  # Give enough time for embedded Cassandra to warm up
  circuit-breaker.call-timeout = "30s"
  circuit-breaker.reset-timeout = "30s"

  graph {
    # Titan
    # ~~~~~
    storage.backend = "embeddedcassandra"
    storage.conf-file = "cassandra/cassandra.yaml"
    storage.cassandra.keyspace = "akkasnapshot"

    # ElasticSearch
    # ~~~~~
    index.search.backend = "elasticsearch"
    index.search.elasticsearch.client-only = false
    index.search.elasticsearch.local-mode = true
    index.search.directory = "../db/es"

  }
}
```

## Status

* All operations required by the Akka Persistence [journal plugin API](http://doc.akka.io/docs/akka/current/scala/persistence.html#Journal_plugin_API) are supported.
* All operations required by the Akka Persistence [Snapshot store plugin API](http://doc.akka.io/docs/akka/current/scala/persistence.html#Snapshot_store_plugin_API) are supported.
* Tested against [Plugin TCK](http://doc.akka.io/docs/akka/current/scala/persistence.html#Plugin_TCK).

## Change Log

### 1.0.0

* First release version to Maven Central Releases.

## Author / Maintainer

* [Adrian FLOREA (@acflorea)](https://github.com/acflorea)
