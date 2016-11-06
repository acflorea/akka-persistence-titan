package akka.persistence.titan.journal

import akka.actor.{Actor, ActorLogging}
import akka.persistence.PersistentRepr
import akka.persistence.titan.TitanCommons._
import org.apache.tinkerpop.gremlin.structure.Graph
import scala.collection.JavaConverters._

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
          entry._2 match {
            // Flatten nested structures
            case nestedMap: java.util.Map[String, Any] =>
              _flatten(entry._1, nestedMap) foreach { nested_entry =>
                detailsVertex.property(s"$PAYLOAD_KEY.${nested_entry._1}", nested_entry._2)
              }
            case _ =>
              detailsVertex.property(s"$PAYLOAD_KEY.${entry._1}", entry._2)
          }
        }

        parentVertex.addEdge(DETAILS_EDGE, detailsVertex)

        graph.tx().commit()
      } else {
        log.warning(s"Unable to retrieve parent vertex for payload $payload and parentId $parentId")
      }

  }


  def _flatten(key: String, map: java.util.Map[String, Any]): scala.collection.mutable.Map[String, Any] = {
    val flatRepr = map.asScala map { entry =>
      entry._2 match {
        // Flatten nested structures
        case nestedMap: java.util.Map[String, Any] =>
          _flatten(entry._1, nestedMap)
        case _ =>
          s"$key.${entry._1}" -> entry._2
      }
    }
    flatRepr.flatten
  }


}

case class Details(payload: PersistentRepr, parentId: AnyRef)

