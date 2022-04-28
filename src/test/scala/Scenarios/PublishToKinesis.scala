package Scenarios

import Data.FmEventType._
import Data._
import FindMatch.{CreateEventActionBuilder, PurgeQueueActionBuilder, ReadQueueActionBuilder, FindMatchProtocol}
import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder

import scala.concurrent.duration._
import scala.language.postfixOps

class PublishToKinesis extends Simulation {

  // TODO: Read these from a config file or environment variable

  private val fmStreamName = "staging-findmatch-matchrequests"
  private val fmQueueName = "TEST3-Matches"
  private val testDuration = 1 minutes


  val fmProtocol = new FindMatchProtocol()
  val purgeSqs: ScenarioBuilder = scenario("Purge SQS Queue").exec(new PurgeQueueActionBuilder(List(fmQueueName), fmProtocol))

  val fmBridgeMatchEvents: ScenarioBuilder = scenario("Write Bridge Events for FM").exec(new CreateEventActionBuilder(FMBridgeEvent(TENANT_MATCH), fmStreamName, fmProtocol))
  val fmBridgeAgentSessionEvents: ScenarioBuilder = scenario("Write Agent session Events for FM").exec(new CreateEventActionBuilder(FMBridgeEvent(AGENT_LOGIN), fmStreamName, fmProtocol))
  val fmBridgeAgentRoutableEvents: ScenarioBuilder = scenario("Write Agent Routable Events for FM").exec(new CreateEventActionBuilder(FMBridgeEvent(AGENT_ROUTABLE), fmStreamName, fmProtocol))
  val fmBridgeContactRoutableEvents: ScenarioBuilder = scenario("Write Contact Routable Events for FM").exec(new CreateEventActionBuilder(FMBridgeEvent(CONTACT_ROUTABLE), fmStreamName, fmProtocol))
  val fmMatchResults: ScenarioBuilder = scenario("Write FM Result Events").exec(new CreateEventActionBuilder(FMMatchResult(), fmStreamName, fmProtocol))
  val fmReadSqs: ScenarioBuilder = scenario("Read FM SQS Messages").exec(new ReadQueueActionBuilder(FMMatchResult(), fmQueueName, fmProtocol))


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
      nothingFor(180 seconds),
      atOnceUsers(2)
//      rampUsers(2) during testDuration
    ).andThen(
    fmBridgeAgentRoutableEvents.inject(
      (1 to 19).flatMap(i => Seq(atOnceUsers(1), nothingFor(900 seconds)))
    )

    )
      )

    .andThen(
      // Clear the queues, calculating the processing time for each entry
      fmReadSqs.inject(
        nothingFor(5 seconds),
        atOnceUsers(3)
      )
    )
  ).protocols(fmProtocol)
}