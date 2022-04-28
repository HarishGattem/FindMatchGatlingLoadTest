package FindMatch

import com.incontact.datainfra.events.TenantIdentification
import com.incontact.datainfra.events.common.{AgentIdentification, ContactIdentification_v2, ContactType, InterruptibilityPriorities, RoutabilityCriteria_v2, RoutableReason, SkillIdentification}
import com.incontact.datainfra.events.findmatchbridge.{AgentRoutableContext, AgentRoutableContext_v5, AgentSessionContext, AgentSessionState, ChannelCount, ContactRoutableContext_v5, ContactRoutableType, FindMatchBridgeEvent, SkillProficiency}
import org.joda.time.DateTime

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.{BufferHasAsJava, SeqHasAsJava}
import java.util.{Random, UUID}
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}
import scala.collection.convert.ImplicitConversions.`collection asJava`
import scala.collection.mutable.ListBuffer

trait EventUtilities {

  val atomicInteger: AtomicInteger = new AtomicInteger(Math.abs((System.currentTimeMillis / 1000).toInt) * -1)
  val atomicLong: AtomicLong = new AtomicLong(System.currentTimeMillis * -1)
  val defaultPublishQueuePrefix = "TEST"
  val numberOfTestQueues = 8
  var timestamp: Long = _

  var agentList: ListBuffer[String] = ListBuffer.empty
  var agentSessionEventList: ListBuffer[FindMatchBridgeEvent] = ListBuffer.empty
  var agentSession: AgentSessionContext = _
  var agentRoutable: AgentRoutableContext = _
  var agentRoutableEventList: ListBuffer[FindMatchBridgeEvent] = ListBuffer.empty

  /**
   * Get next business unit id to use
   * @return current busNo + 1
   */
  def getNextBusNo: Int = {
    atomicInteger.getAndDecrement
  }
  def getNextRandomBusNo: Int = {
    new AtomicInteger(Math.abs((DateTime.now().plusHours(scala.util.Random.nextInt()).getMillis() / 1000).toInt) * -1).getAndDecrement
  }
  /**
   * Get next agent id to use
   * @return current agentId + 1
   */
  def getNextAgentId: Long = {
    atomicLong.getAndDecrement
  }

  /**
   * Get next contact id to use
   * @return current contactId + 1
   */
  def getNextContactId: Long = {
    atomicLong.getAndDecrement
  }

  /**
   * Get next agent session id to use
   * @return current agentSessionId + 1
   */
  def getNextAgentSessionId: Long = {
    atomicLong.getAndDecrement
  }

  /**
   * Get next skill id to use
   * @return current skillId + 1
   */
  def getNextSkillId: Int = {
    atomicInteger.getAndDecrement
  }

  def getNextQueueId: Int = {
    atomicInteger.getAndDecrement
  }

  def getRandomInteger(range: Int): Int = {
    new Random().nextInt(range) + 1
  }

  /**
   * Get next random UUId unit id to use for tenantIds
   *
   * @return random UUID
   */
  def getRandomUUID: String = UUID.randomUUID.toString

  def chooseTestCluster: String = {
    val cluster = defaultPublishQueuePrefix + getRandomInteger(numberOfTestQueues)
    cluster
  }

  def getNextSkill(tenant: String) : SkillIdentification = {
    var skill = new SkillIdentification(tenant, getNextSkillId.toString,workitem.toString)
    skill
  }

  def generateSkillProficiencyList(tenant: String, numOfSkills: Int): ListBuffer[SkillProficiency] = {
    var skillProficiencyList: ListBuffer[SkillProficiency] = ListBuffer.empty
    for(a <- 1 to numOfSkills){
      skillProficiencyList += new SkillProficiency(getNextSkill(tenant), getRandomInteger(20))
    }
    println(s"$numOfSkills skills added ")
    skillProficiencyList
  }

  def generateAgentIdList(numOfAgents: Int): ListBuffer[String] = {


    for (i <- 0 to numOfAgents - 1) {
      agentList +=  getNextAgentId.toString
    }
//    for (j <- 0 to numOfAgents - 1) {
//
//      timestamp = System.currentTimeMillis()
//      agentSession = new AgentSessionContext(agentList(j), AgentSessionState.STARTED, skillList.asJava)
//      agentSessionEventList += new FindMatchBridgeEvent(clusterName, agentSession, timestamp, tenant)
//    }
//    agentSessionEventList
    agentList
  }

//  def generateAgentRoutableEventList(bus: String, tenant: TenantIdentification, numOfAgents: Int, numOfSkills: Int ): ListBuffer[FindMatchBridgeEvent] = {
//    for(a <- 0 to numOfAgents -1){
//      timestamp = System.currentTimeMillis()
//      var agentRoutable = new AgentRoutableContext_v5(getRandomUUID,agentList(a), List[ChannelCount]().asJava, List(new RoutabilityCriteria_v2(email, RoutableReason.AVAILABLE)).asJava, true, new InterruptibilityPriorities("1", "1000", "1", timestamp.toString), "us-west-2" )
//      agentRoutableEventList += new FindMatchBridgeEvent(clusterName, agentRoutable, timestamp, tenant)
//    }
//    agentRoutableEventList
//  }

  def getNextContact(tenant: String) : ContactIdentification_v2 = {
    var contact = new ContactIdentification_v2(tenant,getNextContactId.toString, ContactType.Default)
    contact
  }

  def generateContacts(numOfContacts: Int, tenant: String, skill: SkillIdentification): ListBuffer[ContactRoutableContext_v5] = {
    var contactList: ListBuffer[ContactRoutableContext_v5] = ListBuffer.empty
    for (a <- 1 to numOfContacts){
      contactList += new ContactRoutableContext_v5(getNextContact(tenant), null, ContactRoutableType.ROUTABLE, skill, 1.0, 1.0, 100.0, 1, 20, "us-west-2", 0)
    }
    contactList
  }

  // media types
  val unknown = -1
  val email = 1
  val fax = 2
  val chat = 3
  val call = 4
  val voicemail = 5
  val workitem = 6
  val sms = 7
  val social = 8

  // proficiencies
  val highest = 1
  val high = 2
  val medium = 3
  val low = 4
  val lowest = 5


}
