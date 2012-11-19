package controllers


import views._
import models.{WordCount, Tweet}

import play.api.mvc._
import play.api.libs.concurrent.Promise
import play.api.Logger
import play.api.libs.json.Json._
import play.api.libs.json._
import play.api.libs.oauth.{OAuthCalculator, RequestToken}
import play.api.libs.ws.{WS, ResponseHeaders}
import play.api.libs.iteratee.{Enumerator, Enumeratee, Iteratee}
import play.api.libs.ws.WS.WSRequestHolder
import play.api.libs.Comet

object TwitterController extends Controller {


  def index = Action {
    request =>
      Ok(html.index())
  }

  /**
   * Create a new tweet in mongo db
   * Eventually this should add the tweet to twitter as well
   * But this app doesn't have twitter write access
   *
   * @return
   */
  def create = Action(parse.json) {
    request =>
      val tweet = request.body.as[Tweet]
      Tweet.create(tweet)

      //add a count of the words in each tweet to the database
      FrequencyCountUtil.frequencyCountStr(tweet.getStatus)

      Ok(toJson("created tweet " + tweet.getStatus))
  }

  /**
   * List all of the current tweets in the mongo db
   * @return
   */
  def listTweets = Action {
    implicit request =>

      val tweetList = Tweet.findByOwner("ryan")

      Utils.saveTweetsToFile(tweetList)

      // this uses the implicit converter in Tweet
      Ok(toJson(tweetList))

    // instead of an implicit converter we could just do this
    //and this also works with the java util array returned from the mongo db
    // val json = Json.generate(tweetList)
    // Ok(json).as("application/json")
  }

  /**
   * List all of the current tweets in the mongo db
   * @return
   */
  def listWordCount = Action {
    implicit request =>

      //todo the minimum count should be a parameter passed in from the client
      val wordCountList = WordCount.findAll().filter(_.getCount > 1).sortWith(_.getCount > _.getCount)

      // this uses the implicit converter in Tweet
      Ok(toJson(wordCountList))

  }

  /**
   * Asynchronously connect to twitter and get the users timeline
   * For initial prototyping this is just testing with my token
   * In the future this needs to ask the user for their token and use that instead
   *
   * @return
   */
  def loadtimeline = Action {
    implicit request =>

      val promiseOfTimeline: Promise[Unit] = TwitterUtils.readTwitterTimeline {
        processTimeline(_)
      }

      promiseOfTimeline.map {
        result =>
          Logger.info("finished reading twitter timeline " + result)
      }

      Ok("Processing Timeline")
  }

  /**
   * From the twitter timeline json, parse the json into a list
   * and save the idnividual tweets
   *
   * @param timelineJson
   */
  def processTimeline(timelineJson: JsValue):Unit = {

    //  The following gets the text of the tweets, but text is also used
    //  at multiple levels, so this picks up garbage
    //  I need a way to get text from just one level down?
    //  val tweetSeq: Seq[JsValue] = (timelineJson \\ "text")


    //this is not the right way to parse json
    //the JsValue is a JsArray, but JsArray doesn't have a size or way to iterate
    //I am missing something, there should be a better way

    //we cast this to a list and get the size
    //we then iterate through the list and parse each tweet
    val tweetList: List[JsValue] = timelineJson.as[List[JsValue]]
    (0 to tweetList.size - 1).map {
      indx =>
        parseAndSaveTweet(timelineJson.apply(indx), "ryan", true)
    }
  }


  /**
   * From a twitter json entry of a tweet, parse the json, create a Tweet
   * and save it in the database if it is a new tweet
   *
   * To minimize the amount of data saved in the db, if  saveTweetInDb is false
   * only the twitter id will be saved and the tweet will be set to an empty string
   *
   * @param json
   * @param owner
   * @param saveTweetInDb
   */
  def parseAndSaveTweet(json: JsValue, owner:String, saveTweetInDb:Boolean):Unit = {
    val tweet = Tweet.createTweetFromTwitterJson(json, owner)

    //first see if this tweet is already saved in the db.
    //if not save it to the db
    Tweet.findByTwitterId(tweet.getTwitterId) match {
      case Some(t) => Logger.debug("tweet already exists, not saving")
      case _ => {
        Logger.info("Adding tweet " + tweet)
        Tweet.create(tweet)
        //add a count of the words in each tweet to the database
        FrequencyCountUtil.frequencyCountStr(tweet.getStatus)
      }
    }
  }

  def tweetsStream[A](token: RequestToken)(terms: String)(consumer: ResponseHeaders => Iteratee[Array[Byte], A]) = {

    var encodedTerms = java.net.URLEncoder.encode(terms, "UTF-8")
    val requestUrl = "https://stream.twitter.com/1/statuses/filter.json?track=" + encodedTerms
    Logger.info("connecting to " + requestUrl)

    WS.url(requestUrl)
       .sign(OAuthCalculator(TwitterUtils.KEY, token))
      .get(consumer)
  }


  /**
   * Stream tweets from the Twitter Streaming API
   * @param keywords Terms to track
   */
  def tweets(keywords: String) = Action {

    implicit request =>

      Logger.info("starting stream processing")

      val rawTweet = new Enumerator[Array[Byte]] {
        def apply[A](iteratee: Iteratee[Array[Byte], A]) = {
          tweetsStream(TwitterUtils.TOKEN)(keywords) {
            _ => iteratee
          }
        }
      }

      val jsonTweet = Enumeratee.map[Array[Byte]] {
        message =>
        //println("js: " + new String(message))
          val json = Json.parse(new String(message))
          parseAndSaveTweet(json, owner = "", saveTweetInDb = true)
          json
      }

      /* val streamEnum = rawTweet &> jsonTweet

      val parseAndSaveTweetIteratee = Iteratee.foreach[JsValue] {
        json =>
          parseAndSaveTweet(json, true)
      }

      streamEnum(parseAndSaveTweetIteratee)*/

      Ok.stream(rawTweet &> jsonTweet &> Comet(callback = "parent.newTweet"))
  }

}
