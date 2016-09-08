package akka.persistence.titan

import scala.reflect.io.Path
import scala.util.Try

/**
 * Created by aflorea on 08.09.2016.
 * Data folder removal
 */
object DataPurger {

  /**
   * Recursively tries to purge the specified folder
   * @param path - defaults to "db"
   * @return - Try[operation result]
   */
  def purge(path: String = "db"): Try[Boolean]= {
    Try {
      Path(path).deleteRecursively()
    }
  }

}
