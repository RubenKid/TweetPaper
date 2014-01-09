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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class TweetpaperSettings extends Activity {

	TweetpaperUtils utils;
    private Twitter twitter;
    public RequestToken requestToken;
    EditText hashtagEditText;
    
    private int[] intervals = {5,15,30,60,60*24};

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
	}

	public void onClickTwitterLogin(View v){
			if(!utils.isNetworkAvailable(this.getApplicationContext())){
				Toast.makeText(this.getApplicationContext(), getString(R.string.no_connection), Toast.LENGTH_LONG).show();
				return;
			}
			showLoader();
			new showTwitterLoginPopup().execute();		
	}
	
	public void onClickSave(View v){
		Spinner interval = (Spinner)findViewById(R.id.interval_selector);
		utils.setHashTag(hashtagEditText.getText().toString());
		utils.setInterval(intervals[interval.getSelectedItemPosition()]);
		Toast.makeText(this,getString(R.string.saved_ok), Toast.LENGTH_LONG).show();
		WallpaperInfo wallpaperInfo = WallpaperManager.getInstance(this).getWallpaperInfo();
        if(wallpaperInfo == null || !(wallpaperInfo.getComponent().getClassName().startsWith("com.tweetpaper")))
        	showSetWallpaperDialog();
		
	}
	
	public void showSetWallpaperDialog() {
	    AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    builder.setTitle(getString(R.string.set_wallpaper_title));
	    builder.setMessage(getString(R.string.set_wallpaper_text));
	    final Activity activity = this;
	    builder.setPositiveButton(getString(R.string.yes), new OnClickListener(){
	    
			@SuppressLint("InlinedApi")
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
	                Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
	            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,new ComponentName(activity, TweetpaperService.class));
	            startActivity(intent);
	            }else{
	                    Intent intent = new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
	                    startActivity(intent);
	            }
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
        if(utils.isTwitterLoggedIn())
        	displayLoggedInScreen();
        super.onResume();
	}
	
	private void displayLoggedInScreen(){
		findViewById(R.id.twitter_login).setVisibility(View.GONE);
		findViewById(R.id.settings_screen).setVisibility(View.VISIBLE);
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
