package akka.persistence.titan.journal

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging}
import akka.persistence.PersistentRepr
import akka.persistence.titan.TitanCommons._

/**
  * Actor in charge with storing Journal events details
  * Created by acflorea on 28/09/2016.
  */
class DetailedJournal(config: TitanJournalConfig) extends Actor with ActorLogging {

  import config._

  override def receive: Receive = {

    case Details(payload, parentId) =>
      val vertex = graph.vertices(parentId).next()

      // Properties
      getCCParams(payload.payload) map { entry =>
        vertex.property(s"$PAYLOAD_KEY.${entry._1}", entry._2)
      }

      log.debug(s"Hello from the other side!!! $payload, $parentId")

  }

}

case class Details(payload: PersistentRepr, parentId: AnyRef)

