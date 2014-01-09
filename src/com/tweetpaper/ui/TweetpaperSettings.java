package com.tweetpaper.ui;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import com.tweetpaper.R;
import com.tweetpaper.R.id;
import com.tweetpaper.R.layout;
import com.tweetpaper.R.menu;
import com.tweetpaper.R.string;
import com.tweetpaper.service.TweetpaperService;
import com.tweetpaper.utils.Constants;
import com.tweetpaper.utils.TweetpaperUtils;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

public class TweetpaperSettings extends Activity {

	TweetpaperUtils utils;
    private Twitter twitter;
    public RequestToken requestToken;
    EditText hashtagEditText;
    
    private int[] intervals = {Constants.INTERVAL_5M,Constants.INTERVAL_15M,Constants.INTERVAL_30M,Constants.INTERVAL_1H,Constants.INTERVAL_24H};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tweetpaper_settings);
		utils = new TweetpaperUtils(this);
		
		ConfigurationBuilder builder = new ConfigurationBuilder();
		builder.setOAuthConsumerKey(Constants.TWITTER_CONSUMER_KEY);
		builder.setOAuthConsumerSecret(Constants.TWITTER_CONSUMER_SECRET);
		Configuration configuration = builder.build();
		  
		TwitterFactory twitterFactory = new TwitterFactory(configuration);
		twitter = twitterFactory.getInstance();
		
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		hashtagEditText = (EditText)findViewById(R.id.hashtag_edit);
		hashtagEditText.setOnKeyListener(new OnKeyListener(){

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
					onClickSave(null);
					return true;
				}
				return false;
			}
			
		});
	}

	public void onClickTwitterLogin(View v){
			if(!utils.isNetworkAvailable()){
				Toast.makeText(this.getApplicationContext(), getString(R.string.no_connection), Toast.LENGTH_LONG).show();
				return;
			}
			showLoader();
			new showTwitterLoginPopup().execute();		
	}
	
	@SuppressLint("InlinedApi")
	public void onClickSave(View v){
		if(hashtagEditText.getText().toString().equals("")){
			Toast.makeText(this, getString(R.string.error_empty_hashtag), Toast.LENGTH_LONG).show();
		}
		Spinner interval = (Spinner)findViewById(R.id.interval_selector);
		utils.setHashTag(hashtagEditText.getText().toString());
		utils.setInterval(intervals[interval.getSelectedItemPosition()]);
		
		if(!utils.isTweetpaperSet()){
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
                Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,new ComponentName(this, TweetpaperService.class));
            startActivity(intent);
            Toast.makeText(this,getString(R.string.saved_ok), Toast.LENGTH_LONG).show();
            }else{
                    Intent intent = new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
                    startActivity(intent);
                    Toast.makeText(this,getString(R.string.saved_ok), Toast.LENGTH_LONG).show();
            }
		}
		finish();
	}
	
	public void onControlClicked(View v){
		ImageView playPause = (ImageView)findViewById(R.id.control_play_pause);
		ImageView back = (ImageView)findViewById(R.id.control_back);
		ImageView forward = (ImageView)findViewById(R.id.control_forward);
		if(v == playPause){	
			if(!utils.isPaused()){
				utils.setPaused(true);
				playPause.setImageResource(R.drawable.play_icon);
			}else{
				utils.setPaused(false);
				playPause.setImageResource(R.drawable.pause_icon);
			}
		}else if(v == back){
			utils.setBack(true);
			back.setVisibility(View.INVISIBLE);
		}else if(v == forward){
			utils.setForward(true);
			forward.setVisibility(View.INVISIBLE);
		}
	}
	
	private void updateControls(){
		ImageView playPause = (ImageView)findViewById(R.id.control_play_pause);
		ImageView back = (ImageView)findViewById(R.id.control_back);
		ImageView forward = (ImageView)findViewById(R.id.control_forward);
		
		playPause.setVisibility(View.VISIBLE);
		back.setVisibility(View.VISIBLE);
		forward.setVisibility(View.VISIBLE);
		if(utils.isPaused())
			playPause.setImageResource(R.drawable.play_icon);
		else
			playPause.setImageResource(R.drawable.pause_icon);
	}
	/*
	public void showSetWallpaperDialog() {
	    AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    builder.setTitle(getString(R.string.set_wallpaper_title));
	    builder.setMessage(getString(R.string.set_wallpaper_text));
	    final Activity activity = this;
	    builder.setPositiveButton(getString(R.string.yes), new OnClickListener(){
	    
			@SuppressLint("InlinedApi")
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
			}
	    	
	    });
	    builder.setNegativeButton(getString(R.string.no), new OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
	    	
	    });
	    builder.show();
	}
	*/
	private class showTwitterLoginPopup extends AsyncTask<Void, Void, RequestToken> {

        @Override
        protected RequestToken doInBackground(Void... params) {
        	RequestToken rt = null;
        	try {
        		rt = twitter.getOAuthRequestToken(Constants.TWITTER_CALLBACK_URL);	
        	 } catch (TwitterException e) {
   			  e.printStackTrace();
   		  	}
            return rt;
        }

        @Override
        protected void onPostExecute(RequestToken rt) {
        	if(rt != null){
	        	requestToken = rt;
	        	showTwitterLoginDialog(rt.getAuthenticationURL());
        	}
        }
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.tweetpaper_settings, menu);
		return true;
	}
	
	@Override
	public void onResume(){
		/*Uri uri = getIntent().getData();
        if (uri != null && uri.toString().startsWith(Constants.TWITTER_CALLBACK_URL)) {
        	utils.storeTwitterCredentials(uri,twitter,requestToken);
        	displayLoggedInScreen();
        }*/
		hashtagEditText.setText(utils.getHashTag());
        if(utils.isTwitterLoggedIn()){
        	displayLoggedInScreen();
        	updateInterval();
        	updateControls();
        }
        
        if(utils.isTweetpaperSet())
        	findViewById(R.id.controls).setVisibility(View.VISIBLE);
        else
        	findViewById(R.id.controls).setVisibility(View.GONE);
        
        super.onResume();
	}
	
	private void displayLoggedInScreen(){
		findViewById(R.id.twitter_login).setVisibility(View.GONE);
		findViewById(R.id.settings_screen).setVisibility(View.VISIBLE);
	}
	
	private void updateInterval(){
		Spinner intervalDropDown = (Spinner)findViewById(R.id.interval_selector);
		switch(utils.getInterval()){
			case Constants.INTERVAL_5M:
				intervalDropDown.setSelection(0);
				break;
			case Constants.INTERVAL_15M:
				intervalDropDown.setSelection(1);
				break;
			case Constants.INTERVAL_30M:
				intervalDropDown.setSelection(2);
				break;
			case Constants.INTERVAL_1H:
				intervalDropDown.setSelection(3);
				break;
			case Constants.INTERVAL_24H:
				intervalDropDown.setSelection(4);
				break;
			default:
				intervalDropDown.setSelection(0);
				break;
		}
	}
	
	private void showTwitterLoginDialog(String url){
		final Dialog dialog = new Dialog(this); 
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.activity_webview);
		dialog.setCancelable(true);
		dialog.setOnDismissListener(new OnDismissListener(){

			@Override
			public void onDismiss(DialogInterface arg0) {
				 if(utils.isTwitterLoggedIn())
			        	displayLoggedInScreen();
			}
			
		});
		
		WebView wb = (WebView) dialog.findViewById(R.id.webView);
		wb.setWebViewClient(new WebViewClient(){
			@Override
	        public boolean shouldOverrideUrlLoading(WebView view, String url) {
	            return (false);
	        }
			@Override
	        public void onPageFinished(WebView view, String url) {
				if(!url.startsWith(Constants.TWITTER_CALLBACK_URL)){
					dialog.show();
		        	hideLoader();
				}
	        }
			
			@Override
	        public void onLoadResource(WebView view, String url) {
				if(url.startsWith(Constants.TWITTER_CALLBACK_URL)){
					utils.storeTwitterCredentials(Uri.parse(url),twitter,requestToken);
					dialog.dismiss();
				}
			}
		});
        wb.loadUrl(url);
	}
	
	private void showLoader(){
		findViewById(R.id.loading).setVisibility(View.VISIBLE);
	}
	
	private void hideLoader(){
		findViewById(R.id.loading).setVisibility(View.GONE);
	}

}
