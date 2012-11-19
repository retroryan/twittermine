package models

import net.vz.mongodb.jackson.{Id, ObjectId}
import reflect.BeanProperty
import org.codehaus.jackson.annotate.JsonProperty
import play.modules.mongodb.jackson.MongoDB
import collection.immutable
import play.api.libs.json.{JsValue, Reads, Json, Writes}

import scala.collection.JavaConversions._

import play.api.Play.current

class WordCount(@ObjectId @Id val id: String,
                     @BeanProperty @JsonProperty("word") word: String,
                     @BeanProperty @JsonProperty("count") count: Int) {
  @ObjectId
  @Id
  def getId = id

  def getCount = count
  def getWord = word
}

object WordCount {
  private lazy val db = MongoDB.collection("wordCount", classOf[WordCount], classOf[String])

  def create(wordCount: WordCount):Unit = {
    db.save(wordCount)
  }

  def update(wordCount: WordCount):Unit = {
    db.updateById(wordCount.id, wordCount)
  }

  def findAll() = {
    //the db find returns a java util array and not a scala array
    //so we have to iterate through the list and produce a scala list
    val array = db.find().toArray.toBuffer

    val builder = immutable.List.newBuilder[WordCount]
    for (x <- array)
      builder += x

    builder.result
  }

  def findWord(word: String) = {
    val dbCursor = db.find().is("word", word)
    if (dbCursor.hasNext) {
      Some(dbCursor.next())
    }
    else
      None
  }

  def wordCountToMap(wordCount: WordCount) = (
    Map(
      "word" -> wordCount.getWord,
      "count" -> wordCount.getCount.toString
    )
    )

  implicit object WordCountListWrites extends Writes[List[WordCount]] {
    def writes(wordCountList: List[WordCount]) = Json.toJson(
      wordCountList.map(wordCountToMap(_))
    )
  }


  implicit object WordCountReads extends Reads[WordCount] {
    def reads(json: JsValue) = new WordCount(
      null,
      (json \ "word").as[String],
      (json \ "count").as[String].toInt
    )
  }

}