package controllers

import io.Source
import scala.Predef._
import collection.mutable.ListBuffer


object Utils {

  val wordIndx = loadStopWords()

  def loadStopWords() = {

    val src = Source.fromFile("data/stopwords.csv")
    val stopWords = src.getLines().flatMap(_.split(","))

    //for some reason it seems this collection leaves some kind of pointer to a file descriptor in the collection
    //so we copy the strings out.  The error when trying to access the collection without this is:
    //Caused by: java.io.IOException: Bad file descriptor

    val buf = new ListBuffer[String]
    for (nxt <- stopWords) {
      buf += nxt
    }

    src.close()
    buf
  }

  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try {
      op(p)
    } finally {
      p.close()
    }
  }

  def stopWordExists(word: String) = {
    wordIndx.find(_ == word) match {
      case Some(x) => true
      case _ => false
    }
  }


}
