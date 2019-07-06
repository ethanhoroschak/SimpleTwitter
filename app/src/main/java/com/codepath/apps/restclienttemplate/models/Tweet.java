package com.codepath.apps.restclienttemplate.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcel;

@Parcel
public class Tweet {
    // list attributes to store
    public String body;
    public long uid; // database id for tweet
    public User user;
    public String createdAt;
    public int retweetCount;
    public int likeCount;
    public boolean retweeted;
    public boolean liked;
    public User retweeter;
    public String mediaUrl;
    public String replyTo;
    //public JSONArray media;
    //public JSONObject entities;

    public Tweet() {
    }

    // deserialize the JSON
     public static Tweet fromJSON(JSONObject jsonObject) throws JSONException {
         Tweet tweet = new Tweet();

         if (jsonObject.has("retweeted_status")) {
             tweet.retweeter = User.fromJSON(jsonObject.getJSONObject("user"));
             jsonObject = jsonObject.getJSONObject("retweeted_status");
             tweet.uid = jsonObject.getLong("id");
         } else {
             tweet.uid = jsonObject.getLong("id");
         }
         // extract all values from JSON
         tweet.body = jsonObject.optString("full_text");
         if (tweet.body.isEmpty()) {
             tweet.body = jsonObject.getString("text");
         }

         tweet.createdAt = jsonObject.getString("created_at");
         tweet.user = User.fromJSON(jsonObject.getJSONObject("user"));
         tweet.retweetCount = jsonObject.getInt("retweet_count");
         tweet.likeCount = jsonObject.getInt("favorite_count");
         tweet.retweeted = jsonObject.getBoolean("retweeted");
         tweet.liked = jsonObject.getBoolean("favorited");
         //tweet.entities = jsonObject.getJSONObject("entities");
         //tweet.media = tweet.entities.getJSONArray("media");
         if (jsonObject.getJSONObject("entities").optJSONArray("media") == null) {
             tweet.mediaUrl = null;
         } else {
             tweet.mediaUrl = jsonObject.getJSONObject("entities").getJSONArray("media").getJSONObject(0).optString("media_url_https");
         }

         tweet.replyTo = jsonObject.optString("in_reply_to_screen_name");


         return tweet;
     }
}
