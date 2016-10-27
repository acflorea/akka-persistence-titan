package akka.persistence.titan.journal

import akka.actor.{Actor, ActorRef}
import akka.persistence.{AtomicWrite, CapabilityFlag, PersistentImpl, PersistentRepr}
import akka.persistence.JournalProtocol.{ReplayedMessage, WriteMessageSuccess, WriteMessages, WriteMessagesSuccessful}
import akka.persistence.journal.JournalSpec
import akka.persistence.titan.DataPurger
import akka.persistence.titan.TitanCommons._
import akka.testkit.TestProbe
import com.typesafe.config.ConfigFactory
import org.apache.tinkerpop.gremlin.structure.Direction

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq


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

  override def replayedMessage(snr: Long, deleted: Boolean = false, confirms: Seq[String] = Nil): ReplayedMessage =
    ReplayedMessage(PersistentImpl(ComplexMessage(s"a-$snr"), snr, pid, "", deleted, Actor.noSender, writerUuid))

  override def writeMessages(fromSnr: Int, toSnr: Int, pid: String, sender: ActorRef, writerUuid: String): Unit = {

    def persistentRepr(sequenceNr: Long) = PersistentRepr(
      payload = ComplexMessage(s"a-$sequenceNr"), sequenceNr = sequenceNr, persistenceId = pid,
      sender = sender, writerUuid = writerUuid)

    val msgs =
      if (supportsAtomicPersistAllOfSeveralEvents) {
        (fromSnr until toSnr).map { i =>
          if (i == toSnr - 1)
            AtomicWrite(List(persistentRepr(i), persistentRepr(i + 1)))
          else
            AtomicWrite(persistentRepr(i))
        }
      } else {
        (fromSnr to toSnr).map { i ⇒
          AtomicWrite(persistentRepr(i))
        }
      }

    val probe = TestProbe()

    journal ! WriteMessages(msgs, probe.ref, actorInstanceId)

    probe.expectMsg(WriteMessagesSuccessful)
    fromSnr to toSnr foreach { i =>
      probe.expectMsgPF() {
        case WriteMessageSuccess(PersistentImpl(payload, `i`, `pid`, _, _, `sender`, `writerUuid`), _) ⇒
          payload should be(ComplexMessage(s"a-$i"))
      }
    }
  }

  "My journal" must {
    "replay all messages" in {
      val vertices = graph.traversal().V().hasLabel("vertex").toList.asScala

      val payloadInfo = vertices map { vertex =>

        val detailsVertex = graph.traversal().V(vertex.id()).out(DETAILS_EDGE).limit(1).toList.asScala.headOption

        // Check that the details are present
        detailsVertex shouldBe defined
      }

      logger.info(payloadInfo.toString())
    }
  }
}

/**
  * A dumb message body
  *
  * @param aStringFiled
  * @param aDoubleFiled - can only by 42.42
  */
case class ComplexBody(aStringFiled: String = "some random value", aDoubleFiled: Double = 42.42)

/**
  * A dumb message - the idea is to have something more complex than a single string
  *
  * @param payload
  * @param additionalInfo
  * @param aNumber - 42 obviously
  * @param aNestedCaseClass
  */
case class ComplexMessage(payload: String,
                          additionalInfo: String = "some additional info",
                          aNumber: Integer = 42,
                          aNestedCaseClass: ComplexBody = ComplexBody())