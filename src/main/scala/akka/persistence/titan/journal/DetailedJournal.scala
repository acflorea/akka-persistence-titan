package akka.persistence.titan.journal

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging}
import akka.persistence.PersistentRepr
import akka.persistence.titan.TitanCommons._
import org.apache.tinkerpop.gremlin.structure.{Graph, Vertex}

/**
  * Actor in charge with storing Journal events details
  * Created by acflorea on 28/09/2016.
  */
class DetailedJournal(config: TitanJournalConfig) extends Actor with ActorLogging {

  import config._

  override def receive: Receive = {

    case Details(payload, parentId) =>
      val parentIterator = graph.vertices(parentId)

      if (parentIterator.hasNext) {
        log.debug(s"Parent vertex ($parentId) retrieved. Storing details")
        val parentVertex = graph.vertices(parentId).next()

        val detailsVertex = graph.asInstanceOf[Graph].addVertex(DETAILS_VERTEX_LABEL: String)

        // Properties
        getCCParams(payload.payload) foreach { entry =>
          detailsVertex.property(s"$PAYLOAD_KEY.${entry._1}", entry._2)
        }

        parentVertex.addEdge(DETAILS_EDGE, detailsVertex)

        graph.tx().commit()
      } else {
        log.warning(s"Unable to retrieve parent vertex for payload $payload and parentId $parentId")
      }

  }

}

case class Details(payload: PersistentRepr, parentId: AnyRef)

