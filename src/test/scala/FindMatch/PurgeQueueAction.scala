package FindMatch

import io.gatling.commons.stats.OK
import io.gatling.core.action.{Action, ChainableAction}
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine

class PurgeQueueAction(queueNames: List[String], protocol: FindMatchProtocol, val next: Action, statsEngine: StatsEngine) extends ChainableAction {
  override def name: String = "Purge SQS Queue"

  override protected def execute(session: Session): Unit = {
    queueNames.foreach(queueName => {
      val start = System.currentTimeMillis()
      protocol.purgeSqsQueue(queueName)
      val end = System.currentTimeMillis()

      statsEngine.logResponse("Purge SQS Queue", List(queueName), "Purge Queue", start, end, OK, None, None)
    })

    next ! session
  }
}
