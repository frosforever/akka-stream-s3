package com.github.frosforever

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

object Upload extends App {
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()(system)
  implicit val ec: ExecutionContext = materializer.executionContext


  //TODO: Put whatever test file path here
  val inputFilePath = ""

  val inputFile = new File(inputFilePath)

  //TODO: Put upload bucket name
  val bucket = """
  val key = inputFile.getName


  val stream = FileIO
    .fromPath(inputFile.toPath)
    .map(_.toArray)
    .toMat(S3Stage.sink(bucket, key)){
      case (fio, fs3) =>
        for {
          io <- fio
          s3 <- fs3
        } yield (io, s3)
    }

  val f = stream.run()

  val res = Await.result(f, 30.seconds)

  println(res)

  sys.exit()


}
