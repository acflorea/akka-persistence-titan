package akka.persistence.titan.journal

import akka.persistence.CapabilityFlag
import akka.persistence.journal.JournalSpec
import com.typesafe.config.ConfigFactory

/**
 * Created by aflorea on 25.07.2016.
 */
class TitanJournalSpec extends JournalSpec(
  config = ConfigFactory.parseString(
    """akka.persistence.journal.plugin = "titan-journal"""")) {

  override def supportsRejectingNonSerializableObjects: CapabilityFlag =
    false // or CapabilityFlag.off
}