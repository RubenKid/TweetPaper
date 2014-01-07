package com.tweetpaper.utils;

import com.tweetpaper.ui.TweetpaperSettings;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;

public class TweetpaperUtils{
	
    private static String HASHTAG_DEFAULT = "nyc";
    private static int INTERVAL_DEFAULT = 60*60*1000; //1 hour as default 
    
    private Context context;
    
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
	  public String getTwitterAccessToken() {
        SharedPreferences mPrefs = context.getSharedPreferences(Constants.TWEETPAPER_PREFS, Context.MODE_PRIVATE);
	  	return mPrefs.getString(Constants.PREFS_TWITTER_OAUTH_TOKEN, null);
	  }
	  
	  /*** Get twitter Access token secret for logged in users* */
	  public String getTwitterAccessTokenSecret() {
        SharedPreferences mPrefs = context.getSharedPreferences(Constants.TWEETPAPER_PREFS, Context.MODE_PRIVATE);
	  	return mPrefs.getString(Constants.PREFS_TWITTER_OAUTH_TOKEN_SECRET, null);
	  }
	  
	  /*** Check if Twitter Logged in* */
	  public boolean isTwitterLoggedIn() {
        SharedPreferences mPrefs = context.getSharedPreferences(Constants.TWEETPAPER_PREFS, Context.MODE_PRIVATE);
	  	return mPrefs.contains(Constants.PREFS_TWITTER_OAUTH_TOKEN);
	  }
	  
	  /*** Store Twitter information after successful login* */
	  public void storeTwitterCredentials(Uri uri,Twitter twitter,RequestToken requestToken){
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
              long userID = accessToken.getUserId();
              User user = twitter.showUser(userID);
              String username = user.getName();
             
              mPrefs.edit().putString(Constants.PREFS_TWITTER_USERNAME,username).commit();
              
          } catch (Exception e) {
              // Check log for login errors
              Log.e("Twitter Login Error", "> " + e.getMessage());
          }
	  }
	  
	  public boolean isNetworkAvailable(Context ctx) {
		    ConnectivityManager connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
		}
}