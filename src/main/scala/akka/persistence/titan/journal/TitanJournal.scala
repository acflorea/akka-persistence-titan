package akka.persistence.titan.journal

import akka.actor.{ActorLogging, Props}
import akka.persistence.journal.AsyncWriteJournal
import akka.persistence.titan.TitanCommons._
import akka.persistence.{AtomicWrite, PersistentRepr}
import akka.serialization.{Serialization, SerializationExtension}
import com.thinkaurelius.titan.core.attribute.Cmp
import com.typesafe.config.Config
import org.apache.tinkerpop.gremlin.process.traversal.Order

import scala.collection.JavaConverters._
import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try


/**
  * Created by aflorea on 18.07.2016.
  */
class TitanJournal(conf: Config) extends AsyncWriteJournal with ActorLogging {


  private lazy val serialization: Serialization = SerializationExtension(context.system)

  val config = new TitanJournalConfig(conf)

  import config._

  lazy val detailsActor = context.actorOf(Props(classOf[DetailedJournal], config), "journal-details-writer")

  override def asyncWriteMessages(
                                   messages: Seq[AtomicWrite]
                                 ): Future[Seq[Try[Unit]]] = {

    val verticesFuture = Future {

      messages map { message =>

        val tryBlock = Try {
          message.payload map { payload =>

            val vertex = graph.addVertex()
            // Keys
            vertex.property(TIMESTAMP_KEY, System.currentTimeMillis())
            vertex.property(PERSISTENCE_ID_KEY, payload.persistenceId)
            vertex.property(SEQUENCE_NR_KEY, payload.sequenceNr)
            // Deleted ?
            vertex.property(DELETED_KEY, payload.deleted)
            serialization.serialize(payload) map {
              vertex.property(PAYLOAD_KEY, _)
            }
            log.debug(s"$payload persisted OK!")

            (vertex, payload)
          }
        }

        graph.tx.commit()

        tryBlock.map {
          _ map {
            vp =>
              val vertex = vp._1
              val payload = vp._2
              // Persist details - do it in a fire & forget manner
              detailsActor ! Details(payload, vertex.id())
              vertex
          }
        }

      }

    }

    verticesFuture.map(vertices => vertices map (vertex => vertex map { _ => {} }))

  }

  override def asyncDeleteMessagesTo(
                                      persistenceId: String,
                                      toSequenceNr: Long
                                    ): Future[Unit] = {

    val journalVertices = graph.query()
      .has(PERSISTENCE_ID_KEY, persistenceId)
      .has(SEQUENCE_NR_KEY, Cmp.LESS_THAN_EQUAL, toSequenceNr)
      .vertices().asScala

    Future {
      journalVertices.map(_.property(DELETED_KEY, "Y"))
    }
  }

  override def asyncReplayMessages(
                                    persistenceId: String,
                                    fromSequenceNr: Long,
                                    toSequenceNr: Long,
                                    max: Long
                                  )
                                  (
                                    recoveryCallback: (PersistentRepr) => Unit
                                  ): Future[Unit] = {

    val limit: Int =
      if (max <= Int.MaxValue) max.toInt
      else Int.MaxValue

    val journalVertices = graph.query()
      .has(PERSISTENCE_ID_KEY, persistenceId)
      .has(SEQUENCE_NR_KEY, Cmp.GREATER_THAN_EQUAL, fromSequenceNr)
      .has(SEQUENCE_NR_KEY, Cmp.LESS_THAN_EQUAL, toSequenceNr)
      .hasNot(DELETED_KEY, "Y")
      .orderBy(TIMESTAMP_KEY, Order.incr)
      .orderBy(SEQUENCE_NR_KEY, Order.incr)
      .limit(limit)
      .vertices().asScala

    Future {
      journalVertices map { vertex =>

        serialization.
          deserialize[PersistentRepr](
          vertex.property[Array[Byte]](PAYLOAD_KEY).value(),
          classOf[PersistentRepr]
        ).get

      } foreach recoveryCallback
    }

  }

  override def asyncReadHighestSequenceNr(
                                           persistenceId: String,
                                           fromSequenceNr: Long
                                         ): Future[Long] = {

    val journalVertex = graph.query()
      .has(PERSISTENCE_ID_KEY, persistenceId)
      .has(SEQUENCE_NR_KEY, Cmp.GREATER_THAN_EQUAL, fromSequenceNr)
      .orderBy(TIMESTAMP_KEY, Order.decr)
      .orderBy(SEQUENCE_NR_KEY, Order.decr)
      .vertices().asScala.headOption

    Future {
      journalVertex match {
        case Some(vertex) => vertex.property[Long](SEQUENCE_NR_KEY).value()
        case _ => 0L
      }
    }

  }

  override def postStop(): Unit = {
    graph.close()
  }

}
