package akka.persistence.titan.snapshot

import akka.persistence.snapshot.SnapshotStoreSpec
import akka.persistence.titan.DataPurger
import akka.persistence.titan.TitanCommons._
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._

object TitanSnapshotConfiguration {
  lazy val config = ConfigFactory.parseString(
    """
      |akka.persistence.snapshot-store.plugin = "titan-snapshot-store"
      |titan-snapshot-store.class = "akka.persistence.titan.snapshot.TitanSnapshotStore"
      |titan-snapshot-store.circuit-breaker.call-timeout = "30s"
      |titan-snapshot-store.circuit-breaker.reset-timeout = "30s"
      |graph.storage.backend = "embeddedcassandra"
      |graph.storage.conf-file = "cassandra/cassandra.yaml"
      |graph.storage.cassandra.keyspace = "akkasnapshot"
      |graph.index.search.backend = "elasticsearch"
      |graph.index.search.elasticsearch.client-only = false
      |graph.index.search.elasticsearch.local-mode = true
      |index.search.directory = "db/es"
    """.stripMargin)
}

/**
  * Created by aflorea on 25.07.2016.
  */
class TitanSnapshotStoreSpec extends SnapshotStoreSpec(TitanSnapshotConfiguration.config) {

  val titanConfig = new TitanSnapshotStoreConfig(config)

  import titanConfig._

  override def afterAll() {
    super.afterAll()
    // Purge data folder
    DataPurger.purge()
  }

  "A snapshot store" must {
    "store payload details" in {
      val vertices = graph.traversal().V().hasLabel("vertex").toList.asScala

      val payloadInfo = vertices foreach { vertex =>

        val detailsVertex = graph.traversal().V(vertex.id()).out(DETAILS_EDGE).limit(1).toList.asScala.headOption

        // Check that the details are present
        detailsVertex shouldBe defined

        logger.info(detailsVertex.get.properties().asScala.toList.mkString(", "))
      }

    }
  }


}