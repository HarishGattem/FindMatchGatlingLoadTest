package FindMatch

/*
 * Copyright (C) 2016,2017 inContact Inc.
 * See the LICENSE.txt file distributed with this work for additional
 * information regarding copyright ownership.
 *
 */
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}
import java.nio.ByteBuffer
import org.apache.avro.io._
import org.apache.avro.specific.{SpecificDatumReader, SpecificDatumWriter, SpecificRecordBase}
import sun.misc.{BASE64Decoder, BASE64Encoder}

import scala.util.{Failure, Success, Try}
/**
 * Created by david.sanders on 5/24/2017.
 */

class AvroSchemaSupport {
  val MagicNumber: Int = 0x5043
  val Version = 1

  def writeMagicNumber(): ByteArrayOutputStream = {
    val bOut: ByteArrayOutputStream = new ByteArrayOutputStream()
    val dout = new DataOutputStream(bOut)
    dout.writeInt(big2little(MagicNumber))
    dout.writeInt(big2little(Version))
    return bOut
  }

  def verifyMagicNumber(bb: ByteBuffer): Boolean = {
    //this was throwing an exception for badly formatted data.
    //there is probably a cleaner way to do this.
    try {
      val magicNumber = little2big(bb.getInt())
      val version = little2big(bb.getInt())
      MagicNumber == magicNumber && Version == version
    }
    catch {
      case e: Exception => false
    }
  }

  def little2big(i: Int): Int = (i & 0xff) << 24 | (i & 0xff00) << 8 | (i & 0xff0000) >> 8 | (i >> 24) & 0xff
  def big2little(i: Int): Int = {
    val b1 = (i & 0xFF) << 24
    val b2 = (i & 0xFF00) << 8
    val b3 = (i & 0xFF0000) >> 8
    val b4 = (i & 0xFF000000) >>> 24
    (b1 | b2 | b3 | b4)
  }
}


class SchemaSerializer[A <: SpecificRecordBase](item:A) {
  val eventSchema = item.getSchema
  val reader: DatumReader[A] = new SpecificDatumReader[A](eventSchema)
  val writer: DatumWriter[A] = new SpecificDatumWriter[A](eventSchema)

  var b64decoder = new BASE64Decoder()
  var b64encoder = new BASE64Encoder()

  val decoderFactory = DecoderFactory.get()
  val encoderFactory = EncoderFactory.get()

  val avro = new AvroSchemaSupport()


  def serializeMessage(message:A): String = {
    val bOut = avro.writeMagicNumber()
    val jEncoder: JsonEncoder = EncoderFactory.get().jsonEncoder(eventSchema, bOut)
    val bEncoder = encoderFactory.binaryEncoder(bOut,null)
    writer.write(message,bEncoder)
    bEncoder.flush()
    val matchMessage:String = b64encoder.encode(bOut.toByteArray)
    bOut.close()
    matchMessage
  }

  def deserializeMessage(msgBody: String): Either[String, A] = {
    Try(b64decoder.decodeBuffer(msgBody)) match {
      case Success(msgBytes) =>
        val buffer = ByteBuffer.wrap(msgBytes)

        if(avro.verifyMagicNumber(buffer)) {
          val start = buffer.position() + buffer.arrayOffset()
          val length = buffer.limit() - buffer.position()

          val bDecoder = decoderFactory.binaryDecoder(buffer.array(), start, length, null)

          Try(reader.read(item.getClass().newInstance(), bDecoder)) match {
            case Success(event) => Right(event)
            case Failure(ex) =>    Left("Failed to deserialize message")
          }
        } else {
          Left("Failed to read message - MagicNumber/Version Failure")
        }
      case Failure(ex) =>
        Left("Failed to decode message")
    }
  }
}
