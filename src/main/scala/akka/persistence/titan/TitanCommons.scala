package akka.persistence.titan

import com.google.gson.{Gson, JsonPrimitive}

/**
 * Created by aflorea on 20.07.2016.
 */
object TitanCommons {

  val PERSISTENCE_ID_KEY = "persistenceId"
  val SEQUENCE_NR_KEY = "sequenceNr"
  val TIMESTAMP_KEY = "timestamp"
  val PAYLOAD_KEY = "payload"

  val DELETED_KEY = "deleted"

  val DETAILS_EDGE = "has_details"
  val DETAILS_VERTEX_LABEL = "payload_details"

  val gson = new Gson()

  /**
   * Class to Map serializer
   *
   * @param o - the object to serialize
   * @return - a Map
   */
  def getCCParams(o: Any): Map[String, Any] = {
    val jsoned = gson.toJsonTree(o)
    jsoned match {
      case primitive: JsonPrimitive =>
        Map("_raw" -> o)
      case _ =>
        gson.fromJson[Map[String, Any]](jsoned, Map.empty[String, Any].getClass)
    }
  }

}
