/**
 * Created with IntelliJ IDEA.
 * User: ryan
 * Date: 11/18/12
 * Time: 8:07 PM
 * To change this template use File | Settings | File Templates.
 */


function startStream() {
    var keywords = $("#keywordsInput").val()
    console.log(keywords.length)

    var iframe = window.document.createElement("iframe")

    if (keywords.length > 0) {
        iframe.src = "../tweets/" + keywords
    }
    else {
        iframe.src = "../randomtweets"
    }
    iframe.style.display = "none"
    window.document.body.appendChild(iframe)
}

var newTweet = function (tweet) {
    if (tweet.text != undefined) {
        var tweetsListSize = $("#tweetList").children("li").length
        if (tweetsListSize > 10) {
            $("#tweetList li:last").remove()
        }
        $("#tweetList").prepend("<li><h4>" + tweet.user.name + " </h4><p>" + tweet.text + "</p></li>")
    }
}

function loadTweets() {
    $.post("../listTweets", {},
        function (data) {
            data.forEach(function (tweet) {
                $("#tweetList").prepend("<li><h4>" + tweet.user + " </h4><p>" + tweet.status + "</p></li>")
            })
        }).error(function () {
            console.log("Error loading tweets")
        })
}


function saveStatus() {
    var twitterId = $("#twitterIdInput").val()
    var status = $("#statusInput").val()
    var user = $("#userInput").val()

    if (status === "" || user === "") {
        alert("Please enter a status and user.")
        return
    }

    var statusJson = JSON.stringify({twitterId:twitterId, status: status, user: user})
    console.log("sending " + statusJson)

    jqxhr = $.ajax({
        url: "../create",
        data: statusJson,
        contentType: "application/json",
        type: "POST",
        dataType: "json"
    })

    jqxhr.done(
        function (data) {
            console.log("saved tweet:" + data)
            $("#tweets").empty()
            loadTweets()
        })

    //even thought the create tweet is working, it returns an error message
    jqxhr.fail(
        function (data, textStatus, errorThrown) {
            console.log("error:" + data)
        })
}

function loadWordCounts() {
    $.post("../listWordCount", {},
        function (data) {

            $("#loadingText").empty()

            data.forEach(function (wordCount) {
                $("#wordCounts").append("<li><h4>" + wordCount.word + " =>  " + wordCount.count + "</h4></li>")
            })
        }).error(function () {
            console.log("Error loading word counts");
        });
}
