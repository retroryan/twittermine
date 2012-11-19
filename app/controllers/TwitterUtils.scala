package controllers

import play.api.libs.oauth._
import play.api.libs.oauth.ConsumerKey
import play.api.libs.oauth.RequestToken
import play.api.libs.oauth.OAuth
import play.api.libs.json.JsValue
import play.api.libs.ws.WS
import io.Source
import collection.mutable
import play.api.Logger

object TwitterUtils {

  /**
   * Read the twitter timeline of the user
   * @param procFunc
   * @return
   */
  def readTwitterTimeline(procFunc: JsValue => Unit) = {
    WS.url("https://api.twitter.com/1.1/statuses/home_timeline.json")
      .sign(OAuthCalculator(TwitterUtils.KEY, TwitterUtils.TOKEN))
      .get
      .map {
      result =>
        procFunc(result.json)
    }
  }

  ///This is the wrong way to read a config file, there is the much fancier config file
  //operations built-into the play framework
  //The reason for this is 2 fold.  One was to understand regex in scala and how they work
  //and the other was to put the twitter keys in a separate file so they didn't get checked in.
  val keys = {
    val fileSource = Source.fromFile("data/twitterkeys.conf")
    val pattern = """(\w+.\w+) = (\S+)""".r

    var keymap = new mutable.HashMap[String, String]()
    fileSource.getLines().foreach {
      line =>
        line match {
          case pattern(key, value) => keymap(key) = value
        }
    }

    fileSource.close()

    keymap
  }

  //the consumer key takes the twitter Consumer Key and Consumer Secret
  //these are the values from twitter developer when you register your application
  val KEY = ConsumerKey(
    keys.get("twitter.key").getOrElse(""),
    keys.get("twitter.secret").getOrElse("")
  )

  //the request token takes the twitter token and token Secret
  //this normally comes from the end user when they authorize the application.
  //in this case because we are also doing server to server we store our own token
  val TOKEN = RequestToken(
    keys.get("twitter.token").getOrElse(""),
    keys.get("twitter.tokensecret").getOrElse("")
  )

  val TWITTER = OAuth(ServiceInfo(
    "https://api.twitter.com/oauth/request_token",
    "https://api.twitter.com/oauth/access_token",
    "https://api.twitter.com/oauth/authorize", KEY),
    true)




}
