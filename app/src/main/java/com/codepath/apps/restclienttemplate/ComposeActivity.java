package com.codepath.apps.restclienttemplate;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.codepath.apps.restclienttemplate.models.Tweet;
import com.codepath.apps.restclienttemplate.models.User;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcels;

import cz.msebera.android.httpclient.Header;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

public class ComposeActivity extends AppCompatActivity {

    private TwitterClient client;
    private EditText etTweet;
    private TextView tvTextCount;
    private User user;
    private ImageView ivProfileImage;
    private TextView tvReply;
    private String reply;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);
        client = TwitterApp.getRestClient(this);
        etTweet = findViewById(R.id.etTweet);
        tvTextCount = findViewById(R.id.tvTextCount);
        etTweet.addTextChangedListener(mTextEditorWatcher);
        ivProfileImage = findViewById(R.id.ivProfileImage);
        tvReply = findViewById(R.id.tvReply);
        // get user profile image
        getUser();
        String replyName = getIntent().getStringExtra("user");
        if (replyName == null) {
            tvReply.setVisibility(View.GONE);
            reply = "";
        } else {
            reply = "@" + replyName;
            SpannableStringBuilder spannable = new SpannableStringBuilder("Replying to " + reply);
            spannable.setSpan(
                    new ForegroundColorSpan(ContextCompat.getColor(this, R.color.twitter_blue)),
                    12, // start
                    spannable.length(), // end
                    Spannable.SPAN_EXCLUSIVE_INCLUSIVE
            );
            tvReply.setText(spannable);
        }
    }

    private final TextWatcher mTextEditorWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            //This sets a textview to the current length
            tvTextCount.setText(String.valueOf(280 - s.length()));
        }

        public void afterTextChanged(Editable s) {
        }
    };

    public void onClickTweet(View v) {
        String tweetText = reply + " " + etTweet.getText().toString();
        // make sure only 280 characters
        if (tweetText.length() > 280) {
            Toast.makeText(this, "Exceeded character limit.", Toast.LENGTH_SHORT).show();
        } else if (tweetText.length() == 0) {
            Toast.makeText(this, "No characters entered.", Toast.LENGTH_SHORT).show();
        } else {
            client.sendTweet(tweetText, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                    Log.d("TwitterClient", response.toString());
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    //Log.d("TwitterClient", response.toString());
                    try {
                        // get new tweet data and send back to timeline activity
                        Tweet tweet = Tweet.fromJSON(response);
                        onSubmit(tweet);
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
    }

    // finish activity and send back tweet data
    private void onSubmit(Tweet tweet) {
        // Prepare data to send
        Intent data = new Intent();
        // Wrap data and set result
        data.putExtra(Tweet.class.getSimpleName(), Parcels.wrap(tweet));
        setResult(RESULT_OK, data);
        finish(); // finish the activity
    }

    // If click exit
    public void onClickExit(View v) {
        setResult(RESULT_CANCELED);
        finish();
    }

    public void getUser() {
        client.getUserData(new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.d("TwitterClient", response.toString());
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                //Log.d("TwitterClient", response.toString());
                try {
                    user = User.fromJSON(response);
                    // load profile image
                    int placeHolder = R.drawable.twitter_egg;
                    Glide.with(ComposeActivity.this)
                            .load(user.profileImageUrl)
                            .bitmapTransform(new RoundedCornersTransformation(ComposeActivity.this, 100, 0)) // Extra: round image corners
                            .placeholder(placeHolder) // Extra: placeholder for every image until load or error
                            .error(placeHolder)
                            .into(ivProfileImage);
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
}
