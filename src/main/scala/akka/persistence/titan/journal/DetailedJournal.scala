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
        _flatten(PAYLOAD_KEY, getCCParams(payload.payload)) foreach { entry =>
          detailsVertex.property(s"${entry._1}", entry._2)
        }

        parentVertex.addEdge(DETAILS_EDGE, detailsVertex)

        graph.tx().commit()
      } else {
        log.warning(s"Unable to retrieve parent vertex for payload $payload and parentId $parentId")
      }

  }


  /**
    * Coverts a map of maps to its flatten representation
    *
    * @param key - key to be concatenated with map entries
    * @param map - map of properties candidates
    * @return - a flatten representation of the map
    */
  def _flatten(key: String, map: java.util.Map[String, Any]): Map[String, Any] = {
    val flatRepr = map.asScala map { entry =>
      val inner = entry._2 match {
        // Flatten nested structures
        case nestedMap: java.util.Map[String, Any] =>
          _flatten(s"$key.${entry._1}", nestedMap)
        case _ =>
          Map(s"$key.${entry._1}" -> entry._2)
      }
      inner
    }
    flatRepr.flatten.toMap
  }


}

case class Details(payload: PersistentRepr, parentId: AnyRef)

