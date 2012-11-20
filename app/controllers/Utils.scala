package controllers

import io.Source
import scala.Predef._
import collection.mutable.ListBuffer
import models.Tweet
import play.api.libs.json.Json
import collection.mutable


object Utils {

  val wordIndx = loadStopWords()

  //define a structural type Closable that represents and resource that has a close
  type Closable = {def close(): Unit}


  /**
   * This executes a read file operation on an resource of type Closeable, ensuring at the end that the resource is closed.
   *
   * For some reason it seems this collection leaves some kind of pointer to a file descriptor in the collection
   * so we copy the strings out.  The error when trying to access the collection without this is:
   * Caused by: java.io.IOException: Bad file descriptor

   * @param resource
   * @param readOperation
   * @tparam A
   * @return
   */
  def readListOperation[A <: Closable](resource: A)(readOperation: A => mutable.Buffer[String]): mutable.Buffer[String] = {
    try {
      readOperation(resource)
    } finally {
      resource.close()
    }
  }

  /**
   * This excecutes a write file operation on an resource of type Closeable, ensuring at the end that the resource is closed.
   *
   * @param resource
   * @param writeOperation
   * @tparam A
   * @return
   */
  def writeOpertaion[A <: Closable](resource: A)(writeOperation: A => Any): Unit = {
    try {
      writeOperation(resource)
    } finally {
      resource.close()
    }
  }

  def loadStopWords() = {
    val fileSource = Source.fromFile("data/stopwords.csv")
    readListOperation(fileSource) {
      file => {
        var buf = file.getLines().toBuffer[String].flatMap(_.split(","))
        buf
      }
    }
  }

  def stopWordExists(word: String) = {
    wordIndx.find(_ == word) match {
      case Some(x) => true
      case _ => false
    }
  }

  /**
   * This is a one time use function to write a list of tweets to file for testing purposes
   * @param tweetList
   */
  def saveTweetsToFile(tweetList: List[Tweet]): Unit = {

    import java.io._

    val tweetStrings = tweetList.map {
      tweet =>
        Json.toJson(Tweet.tweetToMap(tweet)).toString()
    }

    val printWriter = new java.io.PrintWriter(new File("data/testdata.json"))
    writeOpertaion(printWriter)(file => {
      file.print(tweetStrings.mkString("[", ", ", "]"))
    })
  }
}
