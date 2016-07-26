package akka.persistence.titan.snapshot

import akka.actor.ActorLogging
import akka.persistence.serialization.Snapshot
import akka.persistence.snapshot.SnapshotStore
import akka.persistence.titan.TitanCommons._
import akka.persistence.{SelectedSnapshot, SnapshotMetadata, SnapshotSelectionCriteria}
import akka.serialization.{Serialization, SerializationExtension}
import com.thinkaurelius.titan.core.attribute.Cmp
import com.typesafe.config.Config
import org.apache.tinkerpop.gremlin.process.traversal.Order

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by aflorea on 18.07.2016.
 */
class TitanSnapshotStore(cfg: Config) extends SnapshotStore with ActorLogging {

  private lazy val serialization: Serialization = SerializationExtension(context.system)

  val config = new TitanSnapshotStoreConfig(cfg)

  import config._

  override def loadAsync(
                          persistenceId: String,
                          criteria: SnapshotSelectionCriteria
                        ): Future[Option[SelectedSnapshot]] = {

    val snapshotVertex = graph.query()
      .has(PERSISTENCE_ID_KEY, persistenceId)
      .has(SEQUENCE_NR_KEY, Cmp.GREATER_THAN_EQUAL, criteria.minSequenceNr)
      .has(SEQUENCE_NR_KEY, Cmp.LESS_THAN_EQUAL, criteria.maxSequenceNr)
      .has(TIMESTAMP_KEY, Cmp.GREATER_THAN_EQUAL, criteria.minTimestamp)
      .has(TIMESTAMP_KEY, Cmp.LESS_THAN_EQUAL, criteria.maxTimestamp)
      .orderBy(TIMESTAMP_KEY, Order.decr)
      .orderBy(SEQUENCE_NR_KEY, Order.decr)
      .vertices().asScala.headOption

    Future {
      snapshotVertex map {
        vertex =>
          val snapshotMetadata = SnapshotMetadata(
            vertex.property[String](PERSISTENCE_ID_KEY).value(),
            vertex.property[Long](SEQUENCE_NR_KEY).value(),
            vertex.property[Long](TIMESTAMP_KEY).value()
          )

          val snapshot = serialization.
            deserialize[Snapshot](
            vertex.property[Array[Byte]](PAYLOAD_KEY).value(),
            classOf[Snapshot]
          ).get

          SelectedSnapshot(snapshotMetadata, snapshot.data)
      }
    }
  }

  override def saveAsync(
                          metadata: SnapshotMetadata,
                          snapshot: Any
                        ): Future[Unit] = {

    Future {

      val vertex = graph.addVertex()
      // Keys
      vertex.property(PERSISTENCE_ID_KEY, metadata.persistenceId)
      vertex.property(SEQUENCE_NR_KEY, metadata.sequenceNr)
      vertex.property(TIMESTAMP_KEY, metadata.timestamp)

      serialization.serialize(Snapshot(snapshot)) map {
        vertex.property(PAYLOAD_KEY, _)
      }

      graph.tx().commit()
      log.debug(s"$snapshot persisted OK!")

    }
  }

  override def deleteAsync(
                            metadata: SnapshotMetadata
                          ): Future[Unit] = {


    val snapshotVertex = graph.query()
      .has(PERSISTENCE_ID_KEY, metadata.persistenceId)
      .has(SEQUENCE_NR_KEY, metadata.sequenceNr)
      .vertices().asScala.headOption

    Future {
      snapshotVertex.map { vertex =>
        vertex.remove()
      }
    }

  }

  override def deleteAsync(
                            persistenceId: String, criteria: SnapshotSelectionCriteria
                          ): Future[Unit] = {

    val snapshotVertices = graph.query()
      .has(PERSISTENCE_ID_KEY, persistenceId)
      .has(SEQUENCE_NR_KEY, Cmp.GREATER_THAN_EQUAL, criteria.minSequenceNr)
      .has(SEQUENCE_NR_KEY, Cmp.LESS_THAN_EQUAL, criteria.maxSequenceNr)
      .has(TIMESTAMP_KEY, Cmp.GREATER_THAN_EQUAL, criteria.minTimestamp)
      .has(TIMESTAMP_KEY, Cmp.LESS_THAN_EQUAL, criteria.maxTimestamp)
      .vertices().asScala

    Future {
      snapshotVertices.map(_.remove())
    }
  }

  override def postStop(): Unit = {
    graph.close()
  }
}

