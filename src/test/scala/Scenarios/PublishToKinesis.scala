package Scenarios

import Configuration.Configuration
import Data.FmEventType._
import Data._
import FindMatch.{CreateEventActionBuilder, FindMatchProtocol, PurgeQueueActionBuilder, ReadQueueActionBuilder}
import Result.LogResultReporter
import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder

import scala.concurrent.duration._
import scala.language.postfixOps

class PublishToKinesis extends Simulation {

  // TODO: Read these from a config file or environment variable
  val configuration: Configuration = new Configuration()
  private val fmStreamName = configuration.streamName
  private val fmQueueName = configuration.queueName
  private val testDuration = 1 minutes


  val fmProtocol = new FindMatchProtocol()
  val reporter = new LogResultReporter()
  val purgeSqs: ScenarioBuilder = scenario("Purge SQS Queue").exec(new PurgeQueueActionBuilder(List(fmQueueName), fmProtocol, reporter))

  val fmBridgeMatchEvents: ScenarioBuilder = scenario("Write Bridge Events for FM").exec(new CreateEventActionBuilder(FMBridgeEvent(TENANT_MATCH), fmStreamName, fmProtocol, reporter))
  val fmBridgeAgentSessionEvents: ScenarioBuilder = scenario("Write Agent session Events for FM").exec(new CreateEventActionBuilder(FMBridgeEvent(AGENT_LOGIN), fmStreamName, fmProtocol, reporter))
  val fmBridgeAgentRoutableEvents: ScenarioBuilder = scenario("Write Agent Routable Events for FM").exec(new CreateEventActionBuilder(FMBridgeEvent(AGENT_ROUTABLE), fmStreamName, fmProtocol, reporter))
  val fmBridgeContactRoutableEvents: ScenarioBuilder = scenario("Write Contact Routable Events for FM").exec(new CreateEventActionBuilder(FMBridgeEvent(CONTACT_ROUTABLE), fmStreamName, fmProtocol, reporter))
  val fmMatchResults: ScenarioBuilder = scenario("Write FM Result Events").exec(new CreateEventActionBuilder(FMMatchResult(), fmStreamName, fmProtocol, reporter))
  val fmReadSqs: ScenarioBuilder = scenario("Read FM SQS Messages").exec(new ReadQueueActionBuilder(FMMatchResult(), fmQueueName, fmProtocol, reporter))


  // Injection types: https://gatling.io/docs/current/general/simulation_setup/
  setUp(
    // Clear out the queues that we need, then send the FindMatchBridgeEvents to populate the database
    purgeSqs.inject(
      atOnceUsers(1)
    ).andThen(
      fmBridgeMatchEvents.inject(
        atOnceUsers(1)
//        rampUsers(2) during testDuration
      )
    ).andThen(
    // Create BE and FM events
      fmBridgeContactRoutableEvents.inject(
        nothingFor(5 seconds),
        atOnceUsers(1)
//        rampUsers(2) during testDuration
      )
    ).andThen(
//      fmBridgeContactRoutableEvents.inject(
//        rampUsers(2) during testDuration
//      ),
    fmBridgeAgentSessionEvents.inject(
      nothingFor(5 seconds),
      atOnceUsers(1)
//      rampUsers(2) during testDuration
    ).andThen(
    fmBridgeAgentRoutableEvents.inject(
      nothingFor(5 seconds),
      atOnceUsers(1)
    )

    )
      )

    .andThen(
      // Clear the queues, calculating the processing time for each entry
      fmReadSqs.inject(
        nothingFor(30 seconds),
        atOnceUsers(1)
      )
    )
  ).protocols(fmProtocol)
}