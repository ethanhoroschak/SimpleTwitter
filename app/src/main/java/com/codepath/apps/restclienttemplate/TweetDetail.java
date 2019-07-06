package com.codepath.apps.restclienttemplate;

import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.codepath.apps.restclienttemplate.models.Tweet;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcels;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

import static com.codepath.apps.restclienttemplate.TweetAdapter.format;

public class TweetDetail extends AppCompatActivity {

    public ImageView ivProfileImage;
    public TextView tvUserName;
    public TextView tvBody;
    public TextView tvTimeStamp;
    public TextView tvHandle;
    public AnimationDrawable heartAnimation;
    public ImageView ivHeart;
    public ImageView ivRetweet;
    public TextView tvRetweetCount;
    public TextView tvLikeCount;
    public ImageView ivRetweeted;
    public TextView tvRetweeter;
    public ImageView ivReply;
    public ImageView ivMedia;
    public TextView tvReplying;
    public Tweet tweet;
    public TwitterClient client;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tweet_detail);
        tweet = Parcels.unwrap(getIntent().getParcelableExtra(Tweet.class.getSimpleName()));
        client = TwitterApp.getRestClient(this);
        // findbyid look ups
        ivProfileImage = findViewById(R.id.ivProfileImage);
        tvUserName = findViewById(R.id.tvUserName);
        tvBody = findViewById(R.id.tvBody);
        tvTimeStamp = findViewById(R.id.tvTimeStamp);
        tvHandle = findViewById(R.id.tvHandle);
        ivHeart = findViewById(R.id.ivHeart);
        ivRetweet = findViewById(R.id.ivRetweet);
        tvRetweetCount = findViewById(R.id.tvRetweetCount);
        tvLikeCount = findViewById(R.id.tvLikeCount);
        ivRetweeted = findViewById(R.id.ivRetweeted);
        tvRetweeter = findViewById(R.id.tvRetweeter);
        ivReply = findViewById(R.id.ivReply);
        ivMedia = findViewById(R.id.ivMedia);
        tvReplying = findViewById(R.id.tvReplying);

        // populate views according to this data
        if (tweet.retweeter != null) {
            ivRetweeted.setVisibility(View.VISIBLE);
            tvRetweeter.setVisibility(View.VISIBLE);
            tvRetweeter.setText(tweet.retweeter.name + " Retweeted");
        } else {
            ivRetweeted.setVisibility(View.GONE);
            tvRetweeter.setVisibility(View.GONE);
        }
        tvUserName.setText(tweet.user.name);
        tvBody.setText(tweet.body);
        tvHandle.setText("@" + tweet.user.screenName);
        // Set like and retweet count
        setValues(tweet);
        // Load relative timestamp
        tvTimeStamp.setText(getRelativeTimeAgo(tweet.createdAt));
        // Prep buttons
        buttonHandler(tweet);
        //setReplyListener(, tweet);
        // load image with glide
        int placeHolder = R.drawable.twitter_egg;
        Glide.with(this)
                .load(tweet.user.profileImageUrl)
                .bitmapTransform(new RoundedCornersTransformation(this, 100, 0)) // Extra: round image corners
                .placeholder(placeHolder) // Extra: placeholder for every image until load or error
                .error(placeHolder)
                .into(ivProfileImage);

        if (tweet.mediaUrl != null) {
            ivMedia.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(tweet.mediaUrl)
                    .into(ivMedia);
        } else {
            ivMedia.setVisibility(View.GONE);
        }

        if (tweet.replyTo != "null") {
            tvReplying.setVisibility(View.VISIBLE);
            SpannableStringBuilder spannable = new SpannableStringBuilder("Replying to @" + tweet.replyTo);
            spannable.setSpan(
                    new ForegroundColorSpan(ContextCompat.getColor(this, R.color.twitter_blue)),
                    12, // start
                    spannable.length(), // end
                    Spannable.SPAN_EXCLUSIVE_INCLUSIVE
            );
            tvReplying.setText(spannable);
        } else {
            tvReplying.setVisibility(View.GONE);
        }
    }

    public void buttonHandler(final Tweet tweet) {
        if (tweet.retweeted) {
            ivRetweet.setImageResource(R.drawable.ic_vector_retweet_green);
            setUnretweetListener(tweet);
        } else {
            ivRetweet.setImageResource(R.drawable.ic_vector_retweet_stroke);
            setRetweetListener(tweet);
        }
        if (tweet.liked) {
            ivHeart.setBackgroundResource(R.drawable.heart_reverse_animate);
            heartAnimation = (AnimationDrawable) ivHeart.getBackground();
            setUnlikeListener(tweet);
        } else {
            ivHeart.setBackgroundResource(R.drawable.heart_animate);
            heartAnimation = (AnimationDrawable) ivHeart.getBackground();
            setLikeListener(tweet);
        }

    }

//    // configure reply button onclick
//    public void setReplyListener(final TweetAdapter. , final Tweet tweet) {
//        ivReply.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(this, ComposeActivity.class);
//                intent.putExtra("user", tweet.user.screenName);
//                intent.putExtra("id", tweet.uid);
//                //((Activity)this).startActivityForResult(intent, REQUEST_CODE);
//            }
//        });
//    }

    public void setLikeListener(final Tweet tweet) {
        // Listener for when tweet is unliked
        ivHeart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                heartAnimation.start();
                client.favorite(tweet.uid, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                        Log.d("TwitterClient", response.toString());
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        try {
                            Log.d("TwitterClient", String.valueOf(tweet.likeCount));
                            tweet.likeCount = response.getInt("favorite_count");
                            tweet.liked = response.getBoolean("favorited");
                            setValues(tweet);
                            Log.d("TwitterClient", String.valueOf(tweet.likeCount));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        Log.d("TwitterClient", responseString);
                        throwable.printStackTrace();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                        Log.d("TwitterClient", errorResponse.toString());
                        throwable.printStackTrace();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        Log.d("TwitterClient", errorResponse.toString());
                        throwable.printStackTrace();
                    }
                });
            }
        });
    }

    public void setRetweetListener(final Tweet tweet) {
        // listener for when tweet is unretweeted
        ivRetweet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ivRetweet.setImageResource(R.drawable.ic_vector_retweet_green);
                client.retweet(tweet.uid, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                        Log.d("TwitterClient", response.toString());
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        try {
                            Log.d("TwitterClient", String.valueOf(tweet.retweeted));
                            tweet.retweetCount = response.getInt("retweet_count");
                            tweet.retweeted = response.getBoolean("retweeted");
                            setValues(tweet);
                            Log.d("TwitterClient", String.valueOf(tweet.retweeted));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        Log.d("TwitterClient", responseString);
                        throwable.printStackTrace();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                        Log.d("TwitterClient", errorResponse.toString());
                        throwable.printStackTrace();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        Log.d("TwitterClient", errorResponse.toString());
                        throwable.printStackTrace();
                    }
                });
            }
        });
    }

    public void setUnretweetListener(final Tweet tweet) {
        // listener for when tweet is retweeted
        ivRetweet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ivRetweet.setImageResource(R.drawable.ic_vector_retweet_stroke);
                client.unretweet(tweet.uid, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                        Log.d("TwitterClient", response.toString());
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        try {
                            Log.d("TwitterClient", String.valueOf(tweet.retweeted));
                            tweet.retweetCount = (response.getInt("retweet_count")) - 1;
                            tweet.retweeted = false;//response.getBoolean("retweeted");
                            setValues(tweet);
                            Log.d("TwitterClient", String.valueOf(tweet.retweeted));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        Log.d("TwitterClient", responseString);
                        throwable.printStackTrace();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                        Log.d("TwitterClient", errorResponse.toString());
                        throwable.printStackTrace();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        Log.d("TwitterClient", errorResponse.toString());
                        throwable.printStackTrace();
                    }
                });
            }
        });
    }

    public void setUnlikeListener(final Tweet tweet) {
        // Listener for when tweet is liked
        ivHeart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                heartAnimation.start();
                client.unfavorite(tweet.uid, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                        Log.d("TwitterClient", response.toString());
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        try {
                            Log.d("TwitterClient", String.valueOf(tweet.likeCount));
                            tweet.likeCount = response.getInt("favorite_count");
                            tweet.liked = response.getBoolean("favorited");
                            setValues(tweet);
                            Log.d("TwitterClient", String.valueOf(tweet.likeCount));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        Log.d("TwitterClient", responseString);
                        throwable.printStackTrace();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                        Log.d("TwitterClient", errorResponse.toString());
                        throwable.printStackTrace();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                        Log.d("TwitterClient", errorResponse.toString());
                        throwable.printStackTrace();
                    }
                });
            }
        });
    }

    public void setValues(final Tweet tweet) {
        tvLikeCount.setText(format(tweet.likeCount));
        tvRetweetCount.setText(format(tweet.retweetCount));
        buttonHandler(tweet);
    }

    // Parse twitter date into relative time stamp to display
    public String getRelativeTimeAgo(String rawJsonDate) {
        String twitterFormat = "EEE MMM dd HH:mm:ss ZZZZZ yyyy";
        SimpleDateFormat sf = new SimpleDateFormat(twitterFormat, Locale.ENGLISH);
        sf.setLenient(true);

        String relativeDate = "";
        try {
            long dateMillis = sf.parse(rawJsonDate).getTime();
            relativeDate = DateUtils.getRelativeTimeSpanString(dateMillis,
                    System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS).toString();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return relativeDate;
    }
}
