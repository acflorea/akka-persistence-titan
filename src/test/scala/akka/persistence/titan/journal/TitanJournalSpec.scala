package akka.persistence.titan.journal

import akka.persistence.CapabilityFlag
import akka.persistence.journal.JournalSpec
import akka.persistence.titan.DataPurger
import akka.persistence.titan.TitanCommons._
import akka.testkit.TestProbe
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._


object TitanJournalConfiguration {
  lazy val config = ConfigFactory.parseString(
    """
      |akka.persistence.journal.plugin = "titan-journal"
      |akka.persistence.snapshot-store.plugin = "titan-snapshot-store"
      |titan-journal.class = "akka.persistence.titan.journal.TitanJournal"
      |titan-journal.circuit-breaker.call-timeout = "30s"
      |titan-journal.circuit-breaker.reset-timeout = "30s"
      |graph.storage.backend = "embeddedcassandra"
      |graph.storage.conf-file = "cassandra/cassandra.yaml"
      |graph.storage.cassandra.keyspace = "akkajournal"
      |graph.index.search.backend = "elasticsearch"
      |graph.index.search.elasticsearch.client-only = false
      |graph.index.search.elasticsearch.local-mode = true
      |index.search.directory = "db/es"
    """.stripMargin)
}


/**
 * Created by aflorea on 25.07.2016.
 */
class TitanJournalSpec extends JournalSpec(
  config = TitanJournalConfiguration.config) {

  val titanConfig = new TitanJournalConfig(config)

  import titanConfig._

  override def supportsRejectingNonSerializableObjects: CapabilityFlag =
    false // or CapabilityFlag.off

  private var myProbe: TestProbe = _

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    myProbe = TestProbe()
  }

  override def afterAll() {
    super.afterAll()
    // Purge data folder
    DataPurger.purge()
  }

  "My journal" must {
    "replay all messages" in {
      val vertices = graph.query().vertices().asScala

      val payloadInfo = vertices map { vertex =>
        val payloadProps = vertex.properties[String]().asScala.filter { p =>
          p.label().startsWith(s"$PAYLOAD_KEY.")
        }
        vertex.property[String](SEQUENCE_NR_KEY).value() ->
          payloadProps.map(p => vertex.property[String](p.label()).value()).toSeq
      }

      logger.info(payloadInfo.toString())
    }
  }
}