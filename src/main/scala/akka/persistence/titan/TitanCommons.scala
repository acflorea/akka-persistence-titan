package akka.persistence.titan

import java.util

import com.google.gson.reflect.TypeToken
import com.google.gson.{Gson, JsonPrimitive}
import scala.collection.JavaConverters._


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
  def getCCParams(o: Any): scala.collection.mutable.Map[String, Any] = {
    val jsoned = gson.toJsonTree(o)
    jsoned match {
      case primitive: JsonPrimitive =>
        scala.collection.mutable.Map("_raw" -> o)
      case _ =>
        val stringStringMap = new TypeToken[util.HashMap[String, Any]]() {}.getType
        gson.fromJson(jsoned, stringStringMap).asInstanceOf[util.HashMap[String, Any]].asScala
    }
  }

}
