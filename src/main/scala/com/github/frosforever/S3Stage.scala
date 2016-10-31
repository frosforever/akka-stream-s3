package com.github.frosforever

import java.io.ByteArrayInputStream

import akka.stream._
import akka.stream.scaladsl.Sink
import akka.stream.stage._
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model._

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.concurrent.{Future, Promise}

object S3Stage {
  def sink(bucketName: String, keyName: String): Sink[Array[Byte], Future[CompleteMultipartUploadResult]] =
    Sink.fromGraph(new S3Stage(bucketName, keyName))
}

class S3Stage(existingBucketName: String, keyName: String) extends GraphStageWithMaterializedValue[SinkShape[Array[Byte]], Future[CompleteMultipartUploadResult]] {
  val minimumPartSize = 5 * 1024 * 1024
  val maxPartNumber = 10000

  val in: Inlet[Array[Byte]] = Inlet("s3.in")

  override def shape: SinkShape[Array[Byte]] = SinkShape.of(in)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[CompleteMultipartUploadResult]) = {

    val p: Promise[CompleteMultipartUploadResult] = Promise()

    (new GraphStageLogic(shape) {
      private var initResponse: InitiateMultipartUploadResult = null
      private val partETags = ListBuffer[PartETag]()

      /** must be between 1 and 10000 (inclusive) */
      private var partNumber = 1

      val s3Client = new AmazonS3Client(new DefaultAWSCredentialsProviderChain)


      def andThen(t: Array[Byte]): Unit = {
        val uploadRequest = new UploadPartRequest()
          .withBucketName(existingBucketName)
          .withKey(keyName)
          .withUploadId(initResponse.getUploadId)
          .withPartNumber(partNumber)
          .withPartSize(t.length.toLong)
          .withInputStream(new ByteArrayInputStream(t))

        val partETag = s3Client
          .uploadPart(uploadRequest)
          .getPartETag

        partETags += partETag
        partNumber += 1
      }

      def completeUpload(): Unit = {
        val comp = new CompleteMultipartUploadRequest(
          existingBucketName,
          keyName,
          initResponse.getUploadId,
          partETags.result().asJava
        )

        val res = s3Client.completeMultipartUpload(comp)
        p.success(res)
      }

      def abort(): Unit = {
        s3Client
          .abortMultipartUpload(
            new AbortMultipartUploadRequest(
              existingBucketName,
              keyName,
              initResponse.getUploadId
            )
          )
      }

      override def preStart(): Unit = {
        val initRequest = new InitiateMultipartUploadRequest(existingBucketName, keyName)
        initResponse = s3Client.initiateMultipartUpload(initRequest)
        pull(in)
      }

      var buffer = Array.empty[Byte]

      setHandler(in, new InHandler {
        //TODO: Have to handle the last element differently?
        override def onPush(): Unit = {
          val t = grab(in)

          val updated = buffer ++ t

          if (updated.lengthCompare(minimumPartSize) >= 0) {
            /* Might not be needed if it's just a lower bound but it can always be more.
             Perhaps just send up what you have so long as it's greater than `defaultPartSize` (min)
            val (head, rest) = updated.splitAt(minimumPartSize)

            // Write head
            andThen(head)

            // Update buffer
            buffer = rest
            */

            if (partNumber <= maxPartNumber) {
              andThen(updated)
              buffer = Array.empty[Byte]
            } else {
              // Not sure what do here
            }


          } else {
            buffer = updated
          }
          pull(in)
        }

        override def onUpstreamFinish(): Unit = {
          //Write the last one that might be smaller than `defaultPartSize`
          if (buffer.nonEmpty) {
            andThen(buffer)
          }
          completeUpload()
          completeStage()
        }

        override def onUpstreamFailure(ex: Throwable): Unit = {
          abort()
          failStage(ex)
        }
      })
    }, p.future)
  }

}
