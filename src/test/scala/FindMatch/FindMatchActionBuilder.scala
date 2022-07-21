package FindMatch

import Data.Record
import Result.ResultReporter
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.action._

class CreateEventActionBuilder(recordType: Record, streamName: String, protocol: FindMatchProtocol, resultReporter: ResultReporter) extends ActionBuilder {

  override def build(context: ScenarioContext, nextAction: Action): Action = {
    new CreateEventAction(recordType, streamName, protocol, nextAction, context.coreComponents.statsEngine, resultReporter)
  }
}

class ReadQueueActionBuilder(recordType: Record, queueName: String, protocol: FindMatchProtocol, resultReporter: ResultReporter) extends ActionBuilder {

  override def build(context: ScenarioContext, nextAction: Action): Action = {
    new ReadQueueAction(recordType, queueName, protocol, nextAction, context.coreComponents.statsEngine, resultReporter)
  }
}

class PurgeQueueActionBuilder(queueNames: List[String], protocol: FindMatchProtocol, resultReporter: ResultReporter) extends ActionBuilder {

  override def build(context: ScenarioContext, nextAction: Action): Action = {
    new PurgeQueueAction(queueNames, protocol, nextAction, context.coreComponents.statsEngine, resultReporter)
  }
}
