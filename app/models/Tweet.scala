package models

import net.vz.mongodb.jackson.{Id, ObjectId}
import reflect.BeanProperty
import org.codehaus.jackson.annotate.JsonProperty
import play.modules.mongodb.jackson.MongoDB
import collection.immutable
import play.api.libs.json.{JsValue, Reads, Json, Writes}

import scala.collection.JavaConversions._

import play.api.Play.current

/**
 * The Class that is stored in the database can not be a case class.  The problem is case classes are immutable
 * and so the id doesn't get properly updated with the mongo db id.  Instead it just adds another field for the id.
 *
 * @param id
 * @param twitterId
 * @param status
 * @param user
 */
class Tweet(@ObjectId @Id id: String,
                 @BeanProperty @JsonProperty("twitterId") twitterId: String,
                 @BeanProperty @JsonProperty("status") status: String,
                 @BeanProperty @JsonProperty("user") user: String,
                 @BeanProperty @JsonProperty("owner") owner: String) {
  @ObjectId
  @Id
  def getId = id

  def getTwitterId = twitterId
  def getStatus = status
  def getUser = user
  def getOwner = owner


}

object Tweet {
  private lazy val db = MongoDB.collection("statuses", classOf[Tweet], classOf[String])

  def create(status: Tweet):Unit = {
    db.save(status)
  }

  def findAll() = {
    //the db find returns a java util array and not a scala array
    //so we have to iterate through the list and produce a scala list
    val array = db.find().toArray.toBuffer

    val builder = immutable.List.newBuilder[Tweet]
    for (x <- array)
      builder += x

    builder.result
  }

  def findByOwner(owner: String) = {
    val dbCursor = db.find().is("owner", owner)

    val builder = immutable.List.newBuilder[Tweet]
    while (dbCursor.hasNext) {
      builder += dbCursor.next()
    }

    builder.result
  }

  def findByTwitterId(twitterId: String) = {
    val dbCursor = db.find().is("twitterId", twitterId)
    if (dbCursor.hasNext)
      Some(dbCursor.next())
    else
      None
  }

  def tweetToMap(tweet: Tweet) = (
    Map(
      "status" -> tweet.getStatus,
      "user" -> tweet.getUser,
      "twitterId" -> tweet.getTwitterId,
      "owner" -> tweet.getOwner
    )
    )

  def createTweetFromTwitterJson(json: JsValue, owner:String) = {
    new Tweet(
      null,
      (json \ "id_str").as[String],
      (json \ "text").as[String],
      (json \ "user" \ "name").as[String],
      owner
    )
  }

  implicit object TweetListWrites extends Writes[List[Tweet]] {
    def writes(tweetList: List[Tweet]) = Json.toJson(
      tweetList.map(tweetToMap(_))
    )
  }


  implicit object TweetReads extends Reads[Tweet] {
    def reads(json: JsValue) = {
      new Tweet(
        null,
        (json \ "twitterId").as[String],
        (json \ "status").as[String],
        (json \ "user").as[String],
        ""
      )
    }
  }

}
