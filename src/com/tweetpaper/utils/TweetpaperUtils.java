package com.tweetpaper.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class TweetpaperUtils{
	
    private static String HASHTAG_DEFAULT = "nyc";
    private static int INTERVAL_DEFAULT = 60*60*1000; //1 hour as default
    
	  public static String getHashTag(Context context) {  
          SharedPreferences prefs = context.getSharedPreferences(Constants.TWEETPAPER_PREFS, Context.MODE_PRIVATE);
          return prefs.getString(Constants.PREFS_HASHTAG, HASHTAG_DEFAULT);
      }
	  
	  public static int getInterval(Context context) {  
		  SharedPreferences mPrefs = context.getSharedPreferences(Constants.TWEETPAPER_PREFS, Context.MODE_PRIVATE);
          return mPrefs.getInt(Constants.PREFS_INTERVAL, INTERVAL_DEFAULT);
      }
}