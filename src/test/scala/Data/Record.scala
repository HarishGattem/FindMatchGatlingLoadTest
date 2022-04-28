package Data

object FmEventType extends Enumeration {
  type FmEventType = Value
  val TENANT_MATCH, AGENT_LOGIN, AGENT_ROUTABLE, CONTACT_ROUTABLE = Value
}

import Data.FmEventType._
abstract class Record(val name: String)
case class FMBridgeEvent(service: FmEventType) extends Record("FM_BridgeEvent")
case class FMMatchResult() extends Record("FM_MatchEvent")
case class BEResult() extends Record("BE_Result")

