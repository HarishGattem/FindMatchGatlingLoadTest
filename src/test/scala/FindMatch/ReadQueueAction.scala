package FindMatch

import Data.{BEResult, FMBridgeEvent, FMMatchResult, Record}
import FindMatch.FindMatchProtocol
import Result.{LogResultReporter, ResultReporter}
import com.incontact.datainfra.events.findmatch.{DigitalContactContext, FindMatchResultContext, FindMatchResultContext_v3, FindMatchResultEvent}
import com.incontact.datainfra.events.findmatchbridge.FindMatchBridgeEvent
import io.gatling.commons.stats.{KO, OK}
import io.gatling.core.action.{Action, ChainableAction}
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import org.apache.avro.Schema
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.apache.commons.lang3.StringEscapeUtils
import org.apache.tomcat.util.codec.binary.Base64
import play.api.libs.json.{JsValue, Json}
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName

import scala.util.{Failure, Success, Try}

class ReadQueueAction(recordType: Record, queueName: String, protocol: FindMatchProtocol, val next: Action, statsEngine: StatsEngine, reporter: ResultReporter) extends ChainableAction {
  override def name: String = "Read Queue"

  override protected def execute(session: Session): Unit = {
    val start = System.currentTimeMillis()

    // Read from the queue until it is consistently empty
    var missedCount = 0
    while (missedCount < 10) {
      val messageList = protocol.getSqsMessages(queueName, 10)

      if (messageList.isEmpty) {
        println("MESSAGE EMPTY")
        missedCount += 1
      }
      else {
        missedCount = 0
        messageList.foreach(msg => {
          var status: io.gatling.commons.stats.Status = OK
          var returnMessage: Option[String] = None

          val body = msg.body()
          println("MESSAGE READ: " + body)
          val event_start: Long = getStartTime(body, reporter) match {
            case Success(v: Long) => v
            case Failure(ex: Throwable) =>
              status = KO
              returnMessage = Some(ex.getMessage)
              start
          }
          var event_end: Long = msg.attributes().getOrDefault(MessageSystemAttributeName.SENT_TIMESTAMP, "0").toLong
          if (event_end == 0) {
            status = KO
            returnMessage = Some("Unable to determine end time")
            event_end = System.currentTimeMillis()
          }
          statsEngine.logResponse("Read SQS Queue", List(recordType.name), "Process Event", event_start, event_end, status, None, returnMessage)

          protocol.deleteSqsMessage(msg, queueName)
        })
      }
    }

    val end = System.currentTimeMillis()
    statsEngine.logResponse("Read SQS Queue", List(queueName), "Read Queue", start, end, OK, None, None)

    next ! session
  }

  def getStartTime(body: String, reporter: ResultReporter): Try[Long] = {
    try {
      recordType match {
        case FMBridgeEvent(_) =>
          val event: FindMatchBridgeEvent = deserializeString(body, FindMatchBridgeEvent.getClassSchema)
          println("BRIDGE EVENT")
          Success(event.getTimestamp)

        case FMMatchResult() =>
          val event: FindMatchResultEvent = deserializeString(body, FindMatchResultEvent.getClassSchema)
          val context: FindMatchResultContext_v3 = event.getContext().asInstanceOf[FindMatchResultContext_v3]
          reporter.reportMatch(context.getSkill().getBuId().toString(), context.getAgent().getAgentId().toString(), context.getContact().getContactId().toString())
          Success(context.getOriginalQueueTime.getMillis)

        case BEResult() =>
          val decoded = new String(Base64.decodeBase64(body))
          val wrapped = parseJson(decoded)
          println("BERESULT")
          wrapped match {
            case Success(json: JsValue) =>
              val data = (json \ "Wrapper" \ "Data").as[String]
              val event: DigitalContactContext = deserializeString(data, DigitalContactContext.getClassSchema)
              Success(event.getPreviousAgentId.toString.toLong) // I put the start time in this field so that it would make it to this point
            case Failure(ex: Throwable) =>
              Failure(ex)
          }
      }
    }
    catch {
      case ex: Throwable => Failure(new RuntimeException(s"Unable to get StartTime: ${ex.getMessage}"))
    }

  }

  def deserializeString[T](str: String, schema: Schema): T = {
    val decoded = Base64.decodeBase64(StringEscapeUtils.unescapeJava(str))
    val data = readFromArray(decoded, Integer.BYTES * 2)
    val reader = new SpecificDatumReader[T](schema)
    val decoder = DecoderFactory.get.binaryDecoder(data, null)
    reader.read(null.asInstanceOf[T], decoder)
  }

  protected def readFromArray(array: Array[Byte], index: Int): Array[Byte] = {
    val len = array.length - index
    val arr = new Array[Byte](len)
    System.arraycopy(array, index, arr, 0, len)
    arr
  }

  def parseJson(data: String): Try[JsValue] = {
    try {
      val json = Json.parse(data)
      Success(json)
    }
    catch {
      case e: Throwable => Failure(throw new RuntimeException(s"Unable to parse BE Json: ${e.getMessage}"))
    }
  }
}