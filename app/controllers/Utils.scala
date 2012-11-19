package controllers

import io.Source
import scala.Predef._
import collection.mutable.ListBuffer
import models.Tweet
import play.api.libs.json.Json
import collection.mutable


object Utils {

  val wordIndx = loadStopWords()


  def loadStopWords() = {

    val fileSource = Source.fromFile("data/stopwords.csv")
    readOperation(fileSource) {
         file => {
           file.getLines().flatMap(_.split(","))
         }
    }


   /* val src = Source.fromFile("data/stopwords.csv")
    val stopWords = src.getLines().flatMap(_.split(","))

    //for some reason it seems this collection leaves some kind of pointer to a file descriptor in the collection
    //so we copy the strings out.  The error when trying to access the collection without this is:
    //Caused by: java.io.IOException: Bad file descriptor

    val buf = new ListBuffer[String]
    for (nxt <- stopWords) {
      buf += nxt
    }

    src.close()*/

    //val buf = new ListBuffer[String]
  }

  //define a structural type Closable that represents and resource that has a close
  type Closable = { def close(): Unit }


  /**
   * This excecutes a read file operation on an resource of type Closeable, ensuring at the end that the resource is closed.
   *
   * @param resource
   * @param readOperation
   * @tparam A
   * @return
   */
  def readOperation[A <: Closable](resource : A)(readOperation: A => Iterator[String]):ListBuffer[String] = {
    try {
      val data = readOperation(resource)
      val buf = new ListBuffer[String]
      data.foreach(buf += _)
      buf
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
  def writeOpertaion[A <: Closable](resource : A)(writeOperation: A => Any):Unit = {
    try {
      writeOperation(resource)
    } finally {
      resource.close()
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
  def saveTweetsToFile(tweetList: List[Tweet]):Unit = {

    import java.io._

    val tweetStrings: List[String] = tweetList.map {
      tweet =>
        Json.toJson(Tweet.tweetToMap(tweet)).toString()
    }

    val printWriter = new java.io.PrintWriter(new File("data/testdata.json"))
    writeOpertaion(printWriter)(file => {
      file.print(tweetStrings.mkString("[", ", ", "]"))
    })
  }

}
