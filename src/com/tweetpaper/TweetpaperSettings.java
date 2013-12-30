package com.tweetpaper;

import com.tweetpaper.utils.Constants;
import com.tweetpaper.utils.TweetpaperUtils;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class TweetpaperSettings extends Activity {

	TweetpaperUtils utils;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tweetpaper_settings);
		utils = new TweetpaperUtils(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.tweetpaper_settings, menu);
		return true;
	}
	
	@Override
	public void onResume(){
		Uri uri = getIntent().getData();
        if (uri != null && uri.toString().startsWith(Constants.TWITTER_CALLBACK_URL)) {
        	utils.storeTwitterCredentials();
        }
	}
	
	

}
