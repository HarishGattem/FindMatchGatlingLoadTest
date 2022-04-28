package FindMatch

import Data.FmEventType._
import Data.{FMBridgeEvent, Record}
import com.incontact.datainfra.events.TenantIdentification
import com.incontact.datainfra.events.TenantIdentification
import com.incontact.datainfra.events.common._
import com.incontact.datainfra.events.findmatch.FindMatchResultContext
import com.incontact.datainfra.events.findmatchbridge._
import io.gatling.commons.stats.OK
import io.gatling.core.action._
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.{Encoder, EncoderFactory}
import org.apache.avro.specific.SpecificDatumWriter
import org.apache.tomcat.util.codec.binary.Base64

import java.io.{ByteArrayOutputStream, OutputStream}
import java.nio.{ByteBuffer, ByteOrder}
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.{BufferHasAsJava, SeqHasAsJava}

class CreateEventAction(recordType: Record, streamName: String, protocol: FindMatchProtocol, val next: Action, statsEngine: StatsEngine) extends ChainableAction with EventUtilities {

  override def name: String = "Create Event"

  val buId = getNextBusNo.toString
  val tenant = new TenantIdentification(buId)
  val tenantGuid = getRandomUUID
  val numOfSkills = 47
  var numberOfAgents = 1200
  val numberOfContactsPerSkill = 426
  var skillProficiencyList = generateSkillProficiencyList(buId, numOfSkills )
  var agentIdList = generateAgentIdList(numberOfAgents)
  val clusterName = "TEST3"


  override def execute(session: Session): Unit = {
    val partitionKey = buId
    recordType match {
      case FMBridgeEvent(fmEventType: FmEventType) if fmEventType == TENANT_MATCH => {
        val data = createTenantChannelMatchEvent(clusterName)
        println(s"Tenant data is $data")
        val start = System.currentTimeMillis()
        protocol.putKinesisRecord(streamName, partitionKey, data)
        val end = System.currentTimeMillis()
        statsEngine.logResponse("Create Tenant Events", List(recordType.name), "Create Event", start, end, OK, None, None)
      }
      case FMBridgeEvent(fmEventType: FmEventType) if fmEventType == CONTACT_ROUTABLE => {
        for (i <- 1 to numberOfContactsPerSkill) {
          for (j <- 0 to skillProficiencyList.length - 1) {
            val contactData = createContactData(clusterName, skillProficiencyList(j).getId)
            println(s"Contact data is $contactData")
            val start = System.currentTimeMillis()
            protocol.putKinesisRecord(streamName, partitionKey, contactData)
            val end = System.currentTimeMillis()
            statsEngine.logResponse("Create Contact Events", List(recordType.name), "Create Event", start, end, OK, None, None)
          }
        }
      }
      case _ => {
        for (i <- 0 to agentList.length - 1) {
          val data = createAgentData(clusterName, agentList(i))
          println(s"Agent data is $data")

          val start = System.currentTimeMillis()
          protocol.putKinesisRecord(streamName, partitionKey, data)
          val end = System.currentTimeMillis()

          statsEngine.logResponse("Create Agent Events", List(recordType.name), "Create Event", start, end, OK, None, None)
        }
      }
    }
    next ! session
  }

  def createTenantChannelMatchEvent(cluster: String): String = {
    wrap(buId, createTenantMatchEvent(tenantGuid, buId, cluster))
  }

  def createAgentData(cluster: String, agentId: String): String = {

    recordType match {
      case FMBridgeEvent(fmEventType: FmEventType) if fmEventType == AGENT_ROUTABLE => wrap(buId, createAgentRoutableEvent(cluster, agentId))
      case FMBridgeEvent(fmEventType: FmEventType) if fmEventType == AGENT_LOGIN => wrap(buId, createAgentLoginEvent(cluster, agentId))
      }
  }

  def createContactData(cluster: String, skillIdentification: SkillIdentification): String = {
    recordType match {
      case FMBridgeEvent(fmEventType: FmEventType) if fmEventType == CONTACT_ROUTABLE => wrap(buId, createContactRoutableEvent(cluster, skillIdentification))
    }
  }

  def wrap(busNo: String, event: String): String = {
    val jsonString = """
                       |{
                       | "Wrapper": {
                       |  "BusNo": $busNo,
                       |  "Data": "$event"
                       | }
                       |}
                      """.stripMargin

    jsonString
      .replace("$busNo", busNo)
      .replace("$event", event)
  }

  def createTenantMatchEvent(tenant: String, busNo: String, cluster: String): String = {
    val event = FindMatchBridgeEvent.newBuilder()
      .setTenant(TenantIdentification.newBuilder()
        .setTenantId(buId)
        .build())
      .setCluster(cluster)
      .setTimestamp(System.currentTimeMillis())
      .setContext(TenantChannelMatchContext.newBuilder()
        .setBusinessUnit(BusinessUnitIdentification.newBuilder()
          .setBuId(busNo)
          .build())
        .setChannels(List(new ChannelIdentification(0), new ChannelIdentification(1), new ChannelIdentification(2),
          new ChannelIdentification(3), new ChannelIdentification(4), new ChannelIdentification(5),
          new ChannelIdentification(6), new ChannelIdentification(7), new ChannelIdentification(8)).asJava)
        .build())
      .build()
    println(s"Tenant event is $event")
    encodeEvent(event)
  }

  def createAgentLoginEvent(cluster: String, agentId: String): String = {
    val event = FindMatchBridgeEvent.newBuilder()
      .setContext(AgentSessionContext.newBuilder()
        .setId(AgentIdentification.newBuilder()
          .setBuId(buId)
          .setAgentId(agentId)
          .setAgentSessionId(getNextAgentSessionId)
          .build()
        )
        .setSkills(skillProficiencyList.asJava)
        .setState(AgentSessionState.STARTED)
        .build())
      .setCluster(cluster)
      .setTenant(TenantIdentification.newBuilder()
        .setTenantId(buId)
        .build())
      .setTimestamp(System.currentTimeMillis())
      .build()
    println(s"Agent login event is $event")
    encodeEvent(event)
  }

  def createAgentRoutableEvent(cluster: String, agentId: String): String = {
    val event = FindMatchBridgeEvent.newBuilder()
      .setContext(AgentRoutableContext_v4.newBuilder()
        .setAgent(AgentIdentification.newBuilder()
          .setBuId(buId)
          .setAgentId(agentId)
          .setAgentSessionId(getNextAgentSessionId)
          .build()
        )
        .setAgentUuid(getRandomUUID)
        .setInterruptPriorities(new InterruptibilityPriorities("1", "3", "34", timestamp.toString))
        .setRoutingCriteria(List(new RoutabilityCriteria_v2(workitem, RoutableReason.AVAILABLE)).asJava)
        .setIgnorePersistence(true)
        .setCurrentContacts(List.empty.asJava)
      .build())
      .setCluster(cluster)
      .setTenant(TenantIdentification.newBuilder()
        .setTenantId(buId)
        .build())
      .setTimestamp(System.currentTimeMillis())
      .build()
    println(s"Agent routable event is $event")
    encodeEvent(event)
  }

  def createContactRoutableEvent(cluster: String, skillIdentification: SkillIdentification): String = {
    val event = FindMatchBridgeEvent.newBuilder()
      .setContext(ContactRoutableContext_v4.newBuilder()
        .setId(getNextContact(buId))
        .setRoutableType(ContactRoutableType.ROUTABLE)
        .setRegion("us-west-2")
        .setMaxPri(1000)
        .setSkillId(skillIdentification)
        .setAcceleration(1.0)
        .setHighProficiency(1)
        .setInitialPri(getRandomInteger(10))
        .setLowProficiency(20)
        .setTargetedAgentId(null)
        .build()
      )
      .setCluster(cluster)
      .setTenant(TenantIdentification.newBuilder()
        .setTenantId(buId)
        .build())
      .setTimestamp(System.currentTimeMillis())
      .build()
    println(s"Contact routable event is $event")
    encodeEvent(event)
  }



  protected def writeToStream(num: Int, stream: OutputStream): Unit = {
    val buf = ByteBuffer.allocate(Integer.BYTES)
    buf.order(ByteOrder.LITTLE_ENDIAN)
    buf.putInt(num)
    stream.write(buf.array)
  }

  protected def encodeEvent[T <: GenericRecord](event: T): String = {
    val stream: ByteArrayOutputStream = new ByteArrayOutputStream()
    val encoder: Encoder = EncoderFactory.get().binaryEncoder(stream, null)
    val writer = new SpecificDatumWriter[T](event.getSchema)

    writeToStream(0x5043, stream)
    writeToStream(1, stream)
    writer.write(event, encoder)

    encoder.flush()
    stream.close()

    new String(Base64.encodeBase64(stream.toByteArray))
  }
}