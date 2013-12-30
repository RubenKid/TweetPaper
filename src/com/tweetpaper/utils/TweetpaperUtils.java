package com.tweetpaper.utils;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;

public class TweetpaperUtils{
	
    private static String HASHTAG_DEFAULT = "nyc";
    private static int INTERVAL_DEFAULT = 60*60*1000; //1 hour as default 
    
    private Context context;
    private Twitter twitter;
    private RequestToken requestToken;
    
      public TweetpaperUtils(Context ctx) {
    	  context = ctx;
      }
    
	  public String getHashTag() {  
          SharedPreferences prefs = context.getSharedPreferences(Constants.TWEETPAPER_PREFS, Context.MODE_PRIVATE);
          return prefs.getString(Constants.PREFS_HASHTAG, HASHTAG_DEFAULT);
      }
	  
	  public int getInterval() {  
		  SharedPreferences mPrefs = context.getSharedPreferences(Constants.TWEETPAPER_PREFS, Context.MODE_PRIVATE);
          return mPrefs.getInt(Constants.PREFS_INTERVAL, INTERVAL_DEFAULT);
      }
	  
	  /*** Get twitter token for logged in users* */
	  private String getTwitterToken() {
        SharedPreferences mPrefs = context.getSharedPreferences(Constants.TWEETPAPER_PREFS, Context.MODE_PRIVATE);
	  	return mPrefs.getString(Constants.PREFS_TWITTER_OAUTH_TOKEN, null);
	  }
	  
	  /*** Check if Twitter Logged in* */
	  private boolean isTwitterLoggedIn() {
        SharedPreferences mPrefs = context.getSharedPreferences(Constants.TWEETPAPER_PREFS, Context.MODE_PRIVATE);
	  	return mPrefs.contains(Constants.PREFS_TWITTER_OAUTH_TOKEN);
	  }
	  
	  /*** Function to login twitter* */
	  public void loginToTwitter() {
		  ConfigurationBuilder builder = new ConfigurationBuilder();
		  builder.setOAuthConsumerKey(Constants.TWITTER_CONSUMER_KEY);
		  builder.setOAuthConsumerSecret(Constants.TWITTER_CONSUMER_SECRET);
		  Configuration configuration = builder.build();
		  
		  TwitterFactory factory = new TwitterFactory(configuration);
		  twitter = factory.getInstance();
		  try {
			  	requestToken = twitter.getOAuthRequestToken(Constants.TWITTER_CALLBACK_URL);
			  	context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthenticationURL())));
		  } catch (TwitterException e) {
			  e.printStackTrace();
		  }
	  }
	  
	  /*** Store Twitter information after successful login* */
	  public void storeTwitterCredentials(Uri uri){
          String verifier = uri.getQueryParameter(Constants.URL_TWITTER_OAUTH_VERIFIER);

          try {
              // Get the access token
              AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verifier);

              // Shared Preferences
              SharedPreferences mPrefs = context.getSharedPreferences(Constants.TWEETPAPER_PREFS, Context.MODE_PRIVATE);

              // After getting access token, access token secret
              // store them in application preferences
              mPrefs.edit().putString(Constants.PREFS_TWITTER_OAUTH_TOKEN, accessToken.getToken()).commit();
              mPrefs.edit().putString(Constants.PREFS_TWITTER_OAUTH_TOKEN_SECRET,accessToken.getTokenSecret()).commit();

              
              // Getting user details from twitter
              // For now i am getting his name only
              long userID = accessToken.getUserId();
              User user = twitter.showUser(userID);
              String username = user.getName();
               
              // Displaying in xml ui
              lblUserName.setText(Html.fromHtml("<b>Welcome " + username + "</b>"));
          } catch (Exception e) {
              // Check log for login errors
              Log.e("Twitter Login Error", "> " + e.getMessage());
          }
	  }
	  
	  private Twitter getTwitter(){
		  return twitter;
	  }
}