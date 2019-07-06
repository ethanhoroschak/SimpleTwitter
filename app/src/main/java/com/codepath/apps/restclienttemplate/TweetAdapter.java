package com.codepath.apps.restclienttemplate;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.codepath.apps.restclienttemplate.models.Tweet;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcels;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import cz.msebera.android.httpclient.Header;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

public class TweetAdapter extends RecyclerView.Adapter<TweetAdapter.ViewHolder>{

    private List<Tweet> mTweets;
    private Context context;
    private TwitterClient client;
    // pass in the tweets array in the constructor
    public TweetAdapter(List<Tweet> tweets) {
         mTweets = tweets;
    }
    private final int REQUEST_CODE = 20;

    // for each row inflate the layout and cache references in ViewHolder class
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        client = TwitterApp.getRestClient(viewGroup.getContext());
        context = viewGroup.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View tweetView = inflater.inflate(R.layout.item_tweet, viewGroup, false);
        ViewHolder viewHolder = new ViewHolder(tweetView);
        return viewHolder;
    }

    // bind the elements based on the position of the element


    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int position) {
        // get data according to position
        final Tweet tweet = mTweets.get(position);
        // populate views according to this data
        if (tweet != null) {
            if (tweet.retweeter != null) {
                viewHolder.ivRetweeted.setVisibility(View.VISIBLE);
                viewHolder.tvRetweeter.setVisibility(View.VISIBLE);
                viewHolder.tvRetweeter.setText(tweet.retweeter.name + " Retweeted");
            } else {
                viewHolder.ivRetweeted.setVisibility(View.GONE);
                viewHolder.tvRetweeter.setVisibility(View.GONE);
            }

            viewHolder.root.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Create intent for new activity
                    Intent intent = new Intent(context, TweetDetail.class);
                    // serialize the movie using parceler, use short name as key
                    intent.putExtra(Tweet.class.getSimpleName(), Parcels.wrap(tweet));
                    // start activity
                    context.startActivity(intent);
                }
            });
            viewHolder.tvUserName.setText(tweet.user.name);
            viewHolder.tvBody.setText(tweet.body);
            viewHolder.tvHandle.setText("@" + tweet.user.screenName);
            // Set like and retweet count
            setValues(viewHolder, tweet);
            // Load relative timestamp
            viewHolder.tvTimeStamp.setText(getRelativeTimeAgo(tweet.createdAt));
            // Prep buttons
            buttonHandler(viewHolder, tweet);
            setReplyListener(viewHolder, tweet);
            // load image with glide
            int placeHolder = R.drawable.twitter_egg;
            Glide.with(context)
                    .load(tweet.user.profileImageUrl)
                    .bitmapTransform(new RoundedCornersTransformation(context, 100, 0)) // Extra: round image corners
                    .placeholder(placeHolder) // Extra: placeholder for every image until load or error
                    .error(placeHolder)
                    .into(viewHolder.ivProfileImage);

            if (tweet.mediaUrl != null) {
                viewHolder.ivMedia.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(tweet.mediaUrl)
                        .into(viewHolder.ivMedia);
            } else {
                viewHolder.ivMedia.setVisibility(View.GONE);
            }

            if (tweet.replyTo != "null") {
                viewHolder.tvReplying.setVisibility(View.VISIBLE);
                SpannableStringBuilder spannable = new SpannableStringBuilder("Replying to @" + tweet.replyTo);
                spannable.setSpan(
                        new ForegroundColorSpan(ContextCompat.getColor(context, R.color.twitter_blue)),
                        12, // start
                        spannable.length(), // end
                        Spannable.SPAN_EXCLUSIVE_INCLUSIVE
                );
                viewHolder.tvReplying.setText(spannable);
            } else {
                viewHolder.tvReplying.setVisibility(View.GONE);
            }
        }
    }

    // set up button animations and appropriate onclicklisteners
    public void buttonHandler(final ViewHolder viewHolder, final Tweet tweet) {
        if (tweet.retweeted) {
            viewHolder.ivRetweet.setImageResource(R.drawable.ic_vector_retweet_green);
            setUnretweetListener(viewHolder, tweet);
        } else {
            viewHolder.ivRetweet.setImageResource(R.drawable.ic_vector_retweet_stroke);
            setRetweetListener(viewHolder, tweet);
        }
        if (tweet.liked) {
            viewHolder.ivHeart.setBackgroundResource(R.drawable.heart_reverse_animate);
            viewHolder.heartAnimation = (AnimationDrawable) viewHolder.ivHeart.getBackground();
            setUnlikeListener(viewHolder, tweet);
        } else {
            viewHolder.ivHeart.setBackgroundResource(R.drawable.heart_animate);
            viewHolder.heartAnimation = (AnimationDrawable) viewHolder.ivHeart.getBackground();
            setLikeListener(viewHolder, tweet);
        }

    }

    // configure reply button onclick
    public void setReplyListener(final ViewHolder viewHolder, final Tweet tweet) {
        viewHolder.ivReply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ComposeActivity.class);
                intent.putExtra("user", tweet.user.screenName);
                intent.putExtra("id", tweet.uid);
                ((Activity)context).startActivityForResult(intent, REQUEST_CODE);
            }
        });
    }

    public void setLikeListener(final ViewHolder viewHolder, final Tweet tweet) {
        // Listener for when tweet is unliked
        viewHolder.ivHeart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                viewHolder.heartAnimation.start();
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
                            setValues(viewHolder, tweet);
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

    public void setRetweetListener(final ViewHolder viewHolder, final Tweet tweet) {
        // listener for when tweet is unretweeted
        viewHolder.ivRetweet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewHolder.ivRetweet.setImageResource(R.drawable.ic_vector_retweet_green);
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
                            setValues(viewHolder, tweet);
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

    public void setUnretweetListener(final ViewHolder viewHolder, final Tweet tweet) {
        // listener for when tweet is retweeted
        viewHolder.ivRetweet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewHolder.ivRetweet.setImageResource(R.drawable.ic_vector_retweet_stroke);
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
                            setValues(viewHolder, tweet);
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

    public void setUnlikeListener(final ViewHolder viewHolder, final Tweet tweet) {
        // Listener for when tweet is liked
        viewHolder.ivHeart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                viewHolder.heartAnimation.start();
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
                            setValues(viewHolder, tweet);
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

    public void setValues(final ViewHolder viewHolder, final Tweet tweet) {
        viewHolder.tvLikeCount.setText(format(tweet.likeCount));
        viewHolder.tvRetweetCount.setText(format(tweet.retweetCount));
        buttonHandler(viewHolder, tweet);
    }

    @Override
    public int getItemCount() {
        return mTweets.size();
    }

    // create ViewHolder class
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
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
         public ConstraintLayout root;

         public ViewHolder(View itemView) {
             super(itemView);

             // findbyid look ups
             ivProfileImage = itemView.findViewById(R.id.ivProfileImage);
             tvUserName = itemView.findViewById(R.id.tvUserName);
             tvBody = itemView.findViewById(R.id.tvBody);
             tvTimeStamp = itemView.findViewById(R.id.tvTimeStamp);
             tvHandle = itemView.findViewById(R.id.tvHandle);
             ivHeart = itemView.findViewById(R.id.ivHeart);
             ivRetweet = itemView.findViewById(R.id.ivRetweet);
             tvRetweetCount = itemView.findViewById(R.id.tvRetweetCount);
             tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
             ivRetweeted = itemView.findViewById(R.id.ivRetweeted);
             tvRetweeter = itemView.findViewById(R.id.tvRetweeter);
             ivReply = itemView.findViewById(R.id.ivReply);
             ivMedia = itemView.findViewById(R.id.ivMedia);
             tvReplying = itemView.findViewById(R.id.tvReplying);
             root = itemView.findViewById(R.id.root);
         }

        @Override
        public void onClick(View v) {
             Log.e("Click", "REEEEEEEE");
            Toast.makeText(context, "REEE", Toast.LENGTH_SHORT).show();
            int position = getAdapterPosition();
            // ensure the position is in the row
            if (position != RecyclerView.NO_POSITION) {
                // get movie at position, wont work if static
                Tweet tweet = mTweets.get(position);
                // Create intent for new activity
                Intent intent = new Intent(context, TweetDetail.class);
                // serialize the movie using parceler, use short name as key
                intent.putExtra(Tweet.class.getSimpleName(), Parcels.wrap(tweet));
                // start activity
                context.startActivity(intent);
            }
        }
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

    // Clean all elements of the recycler
    public void clear() {
        mTweets.clear();
        notifyDataSetChanged();
    }

    // Add a list of items -- change to type used
    public void addAll(List<Tweet> list) {
        mTweets.addAll(list);
        notifyDataSetChanged();
    }

    // For formatting integers
    private static final NavigableMap<Long, String> suffixes = new TreeMap<>();
    static {
        suffixes.put(1_000L, "k");
        suffixes.put(1_000_000L, "M");
        suffixes.put(1_000_000_000L, "G");
        suffixes.put(1_000_000_000_000L, "T");
        suffixes.put(1_000_000_000_000_000L, "P");
        suffixes.put(1_000_000_000_000_000_000L, "E");
    }
    // Format like and retweet count
    public static String format(long value) {
        //Long.MIN_VALUE == -Long.MIN_VALUE so we need an adjustment here
        if (value == Long.MIN_VALUE) return format(Long.MIN_VALUE + 1);
        if (value < 0) return "-" + format(-value);
        if (value < 1000) return Long.toString(value); //deal with easy case

        Map.Entry<Long, String> e = suffixes.floorEntry(value);
        Long divideBy = e.getKey();
        String suffix = e.getValue();

        long truncated = value / (divideBy / 10); //the number part of the output times 10
        boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);
        return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
    }
}
