package akka.persistence.titan.snapshot

import akka.persistence.snapshot.SnapshotStoreSpec
import com.typesafe.config.ConfigFactory

/**
 * Created by aflorea on 25.07.2016.
 */
class TitanSnapshotStoreSpec extends SnapshotStoreSpec(
  config = ConfigFactory.parseString(
    """
    akka.persistence.snapshot-store.plugin = "titan-snapshot-store"
    """))