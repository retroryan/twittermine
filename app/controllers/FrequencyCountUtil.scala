package controllers


import scala.Predef._

import models.WordCount
import play.api.Logger

object FrequencyCountUtil {

  /**
   * For the given string, parse the string into individual words and create a map of word counts
   * Then add or update the word counts in the db
   *
   * @param line
   */
  def frequencyCountStr(line: String):Unit = {

    // ideally we want to split on sentences and word boundaries
    // valwordsArray=text.split("[ !,.]+")

    //first group the like words in map, forcing everything to lower case
    //then create a map of the word counts by taking the length of the array words
    val frequencyCountMap = line.split("[ !,.]+").groupBy(word => word.toLowerCase).mapValues(_.length)

    //add or update word counts in the database
    //exclude single letter words and stop words.

    frequencyCountMap.foreach {
      case (word, count) => {
        if ((word.length > 1) && (!Utils.stopWordExists(word))) {
          addFrequencyCountToDb(word, count)
        }
      }
    }
  }

  /**
   * Add or update the given word and count to the database
   *
   * @param word
   * @param count
   */
  def addFrequencyCountToDb(word: String, count: Int):Unit = {

    WordCount.findWord(word) match {
      case Some(wc) => {

        val wordCount = new WordCount(
          wc.id,
          word,
          wc.getCount + count
        )
        WordCount.update(wordCount)
        Logger.info("update " + wordCount.getWord + " " + wordCount.getCount)
      }

      case _ => {

        val wordCount = new WordCount(
          null,
          word,
          count
        )
        WordCount.create(wordCount)
        Logger.info("adding " + wordCount.getWord + " " + wordCount.getCount)
      }
    }

  }

}
