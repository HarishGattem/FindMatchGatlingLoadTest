package FindMatch

import Data.Record
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.action._

class CreateEventActionBuilder(recordType: Record, streamName: String, protocol: FindMatchProtocol) extends ActionBuilder {

  override def build(context: ScenarioContext, nextAction: Action): Action = {
    new CreateEventAction(recordType, streamName, protocol, nextAction, context.coreComponents.statsEngine)
  }
}

class ReadQueueActionBuilder(recordType: Record, queueName: String, protocol: FindMatchProtocol) extends ActionBuilder {

  override def build(context: ScenarioContext, nextAction: Action): Action = {
    new ReadQueueAction(recordType, queueName, protocol, nextAction, context.coreComponents.statsEngine)
  }
}

class PurgeQueueActionBuilder(queueNames: List[String], protocol: FindMatchProtocol) extends ActionBuilder {

  override def build(context: ScenarioContext, nextAction: Action): Action = {
    new PurgeQueueAction(queueNames, protocol, nextAction, context.coreComponents.statsEngine)
  }
}
