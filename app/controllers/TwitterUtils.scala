package controllers

import play.api.libs.oauth._
import play.api.Play._
import play.api.libs.oauth.ConsumerKey
import play.api.libs.oauth.RequestToken
import play.api.libs.oauth.OAuth
import play.api.libs.json.JsValue
import play.api.libs.ws.WS

object TwitterUtils {


  //the consumer key takes the twitter Consumer Key and Consumer Secret
  //these are the values from twitter developer when you register your application
  val KEY = ConsumerKey(
    current.configuration.getString("twitter.key").getOrElse(""),
    current.configuration.getString("twitter.secret").getOrElse("")
  )

  //the request token takes the twitter token and token Secret
  //this normally comes from the end user when they authorize the application.
  //in this case because we are also doing server to server we store our own token
  val TOKEN = RequestToken(
    current.configuration.getString("twitter.token").getOrElse(""),
    current.configuration.getString("twitter.tokensecret").getOrElse("")
  )

  val TWITTER = OAuth(ServiceInfo(
    "https://api.twitter.com/oauth/request_token",
    "https://api.twitter.com/oauth/access_token",
    "https://api.twitter.com/oauth/authorize", KEY),
    true)

  def readTwitterTimeline(procFunc:JsValue => Unit) = {
    WS.url("https://api.twitter.com/1.1/statuses/home_timeline.json")
      .sign(OAuthCalculator(TwitterUtils.KEY, TwitterUtils.TOKEN))
      .get
      .map {
      result =>
        procFunc(result.json)
    }
  }

}
