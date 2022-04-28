package FindMatch

import io.gatling.core.protocol._
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kinesis.KinesisClient
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.{DeleteMessageRequest, GetQueueUrlRequest, Message, PurgeQueueRequest, QueueAttributeName, ReceiveMessageRequest}
import scala.jdk.CollectionConverters.CollectionHasAsScala

object FindMatchProtocol {
  protected var queueUrls: Map[String, String] = Map[String, String]()
}

class FindMatchProtocol extends Protocol {

  import FindMatchProtocol._

  protected val kinesisClient: KinesisClient = KinesisClient.builder().build()
  protected val sqsClient: SqsClient = SqsClient.builder().build()

  def putKinesisRecord(streamName: String, partitionKey: String, data: String): Unit = {
    val req = PutRecordRequest.builder()
      .streamName(streamName)
      .data(SdkBytes.fromUtf8String(data))
      .partitionKey(partitionKey)
      .build()

    kinesisClient.putRecord(req)
  }

  def getSqsMessages(queueName: String, numMessages: Int = 1): Iterable[Message] = {
    val req = ReceiveMessageRequest.builder()
      .queueUrl(getQueueUrl(queueName))
      .maxNumberOfMessages(numMessages)
      .attributeNames(QueueAttributeName.ALL)
      .build()

    sqsClient.receiveMessage(req).messages().asScala
  }

  def deleteSqsMessage(msg: Message, queueName: String): Unit = {
    val req = DeleteMessageRequest.builder()
      .queueUrl(getQueueUrl(queueName))
      .receiptHandle(msg.receiptHandle())
      .build()

    sqsClient.deleteMessage(req)
  }

  def purgeSqsQueue(queueName: String): Unit = {
    val req = PurgeQueueRequest.builder()
      .queueUrl(getQueueUrl(queueName))
      .build()

    sqsClient.purgeQueue(req)
  }

  private def getQueueUrl(queueName: String): String = {
    queueUrls.getOrElse(queueName, {
      val getQueueUrlResponse = sqsClient.getQueueUrl(GetQueueUrlRequest.builder.queueName(queueName).build)
      queueUrls += (queueName -> getQueueUrlResponse.queueUrl)
      getQueueUrlResponse.queueUrl
    })
  }
}
