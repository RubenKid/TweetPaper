	package com.tweetpaper.service;
	
	import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import twitter4j.FilterQuery;
import twitter4j.MediaEntity;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.TextView;

import com.tweetpaper.R;
import com.tweetpaper.utils.Constants;
import com.tweetpaper.utils.TweetpaperUtils;
	
	public class TweetpaperService extends WallpaperService {
	
	    private static int INTERVAL_MINUTES_DEFAULT = 60; //60 minutes as default
	    private static int RETRY_TIMEOUT = 1000;
	    
	    private static int MAX_ITEMS = 500;
	    
	    private Bitmap currentBitmap;
	    TweetpaperUtils utils;
		Twitter twitter;
		TwitterStream twitterStream;
		String hashtag;
	    
	private int nextPositionToDisplay = 0;
	ArrayList<String> itemsArray = new ArrayList<String>();
	private TweetpaperEngine tweetpaperEngine;
	
	@Override
	public Engine onCreateEngine() {
		utils = new TweetpaperUtils(this);
		initTwitter();
	    tweetpaperEngine = new TweetpaperEngine();
	    tweetpaperEngine.setContext(this.getApplicationContext());
	    return tweetpaperEngine;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	  return START_NOT_STICKY;
	}
	
	private void initTwitter(){
		String twitterAccessToken = utils.getTwitterAccessToken();
	    String twitterAccessTokenSecret = utils.getTwitterAccessTokenSecret();
	    hashtag = utils.getHashTag();
	       
		ConfigurationBuilder builder = new ConfigurationBuilder();
		builder.setOAuthConsumerKey(Constants.TWITTER_CONSUMER_KEY);
		builder.setOAuthConsumerSecret(Constants.TWITTER_CONSUMER_SECRET);
		builder.setOAuthAccessToken(twitterAccessToken);
		builder.setOAuthAccessTokenSecret(twitterAccessTokenSecret);
		Configuration configuration = builder.build();
		  
		TwitterFactory twitterFactory = new TwitterFactory(configuration);
		twitter = twitterFactory.getInstance();
		twitterStream = new TwitterStreamFactory(configuration).getInstance();
	}
	
	private class TweetpaperEngine extends Engine {
	private final Handler handler = new Handler();
	//private final Handler clearHandler = new Handler();
	
	private final Runnable drawRunner = new Runnable() {
	  @Override
	  public void run() {
	          if((!visible || utils.isPaused()) && !forceUpdate){
	        	  Log.i("tweetpaper","Wallpaper not visible or paused");
	                  handler.postDelayed(drawRunner, interval);
	                  return;
	          }
	          if(!utils.isNetworkAvailable() || (utils.isOnlyOnWifiEnabled() && !utils.isWifiConnected())){
	        	  Log.i("tweetpaper","No network");
	        	  Bitmap bitmapCache = getBitmapInCache();
	              if(bitmapCache != null)
	                  drawBitmapInCanvas(bitmapCache);
	              else
	            	  loadDefaultWallpaper(getString(R.string.default_text_no_internet));
	             handler.postDelayed(drawRunner, RETRY_TIMEOUT);
	          }else{
	    	      	if(!twitterInitialized)
	    	      		initTwitterSearch();
	                draw();
	          }
	                          
	  }
	
	};
	private boolean visible = true;
	private long lastchange;
	private int screenX;
	private int screenY;
	private int interval;
	private Context context;
	private boolean twitterInitialized = false;
	boolean forceUpdate = false;

	public TweetpaperEngine() {
	        //Log.d(LOG_TAG,"PixableWallpaperEngine");
	  DisplayMetrics metrics = getApplicationContext().getResources().getDisplayMetrics();
	  screenX = metrics.widthPixels;
	  screenY = metrics.heightPixels;
	    
	  handler.removeCallbacks(drawRunner);
	  updateInterval();
	  updateHashtag();
	  
	  getSurfaceHolder().addCallback(surfaceCallback);
	  
	      handler.post(drawRunner);   
	}
	
	private void initTwitterSearch(){
	     if(hashtag != null && !hashtag.equals("") && utils.getTwitterAccessToken() != null && utils.getTwitterAccessTokenSecret() != null){
		    //Enable Twitter Streaming API
			FilterQuery fq = new FilterQuery(); 
	        String keywords[] = {hashtag};
	        fq.track(keywords);
	        twitterStream.addListener(statusListener);
	        twitterStream.filter(fq); 
	        
	        //Search tweets
	        new getTweetsByHashtag().execute(new Query(hashtag));
	        twitterInitialized = true;
	      }
	}
	
	SurfaceHolder.Callback surfaceCallback =  new SurfaceHolder.Callback(){
	
	            @Override
	            public void surfaceChanged(SurfaceHolder holder, int format, int width,
	                            int height) {
	            }
	
	            @Override
	            public void surfaceCreated(SurfaceHolder holder) {
	                     Bitmap bitmapCache = getBitmapInCache();
	                 if(bitmapCache != null){ 
	                          drawBitmapInCanvas(bitmapCache);
	                 }
	            }
	
	            @Override
	            public void surfaceDestroyed(SurfaceHolder holder) {
	            }
	
	};
	
	public void setContext(Context ctx){
	        context = ctx;
	}
	
	public boolean updateInterval(){          
	        SharedPreferences mPrefs = getSharedPreferences(Constants.TWEETPAPER_PREFS, Context.MODE_PRIVATE);
	        int newInterval = mPrefs.getInt(Constants.PREFS_INTERVAL, INTERVAL_MINUTES_DEFAULT);     
	        newInterval = newInterval * 60 * 1000; //Minutes to Millis	 
	        	        
	        boolean intervalChanged = newInterval != interval;
	        interval = newInterval;
	        return intervalChanged;
	}
	
	public boolean updateHashtag(){   
        SharedPreferences mPrefs = getSharedPreferences(Constants.TWEETPAPER_PREFS, Context.MODE_PRIVATE);
        String newHashtag = mPrefs.getString(Constants.PREFS_HASHTAG, TweetpaperUtils.HASHTAG_DEFAULT);    
        if(!newHashtag.equals(hashtag)){
        	hashtag = newHashtag;
        	initTwitterSearch();
        	nextPositionToDisplay = 0;
        	itemsArray = new ArrayList<String>();
        	return true;
        }else
        	return false;
	}

	@Override
	public void onVisibilityChanged(boolean visible) {
	  this.visible = visible;
	  updateInterval();
	  forceUpdate = updateHashtag();
	  
	  if(utils.isBackPressed() && nextPositionToDisplay > 1 && !utils.isForwardPressed()){
		  nextPositionToDisplay = nextPositionToDisplay-2;
		  forceUpdate = true;
		  utils.setBack(false);
	  }
	  if(utils.isForwardPressed() && !utils.isBackPressed()){
		  utils.setForward(false);
		  forceUpdate = true;
	  }		  
	  
	  //If background is visible or no items or we changed interval, we should refresh
	  if ((visible && (System.currentTimeMillis() - lastchange) > interval) || itemsArray.size() == 0 || forceUpdate) {
	    handler.post(drawRunner);
	  } else if(!visible){
	    //handler.removeCallbacks(drawRunner);
	  }
	}
	@Override
	public void onSurfaceDestroyed(SurfaceHolder holder) {
	  super.onSurfaceDestroyed(holder);
	  this.visible = false;
	  //handler.removeCallbacks(drawRunner);
	}

	@Override
	public void onSurfaceChanged(SurfaceHolder holder, int format,
	    int width, int height) {
	  screenX = width;
	  screenY = height;
	  
	  if(currentBitmap != null)
	          drawBitmapInCanvas(null);
	  super.onSurfaceChanged(holder, format, width, height);
	}
	
	private void draw() {
	  String url;
	  if(itemsArray.size() > 0 && nextPositionToDisplay >= itemsArray.size())
	          nextPositionToDisplay = 0;
	  if(nextPositionToDisplay < itemsArray.size()){
	          url = itemsArray.get(nextPositionToDisplay);
	  }else{
		  	Log.i("tweetpaper","No images Available");
	          loadDefaultWallpaper(getString(R.string.default_text_no_images,hashtag));
	          handler.postDelayed(drawRunner, RETRY_TIMEOUT);
	          return;
	  }
	  Log.i("tweetpaper","Show next item ("+nextPositionToDisplay+" of "+itemsArray.size()+")");
	  new loadBitmapFromUrl().execute(url);
	  handler.removeCallbacks(drawRunner);
	  if (visible) {
	    handler.postDelayed(drawRunner, interval);
	  }
	}

	class loadBitmapFromUrl extends AsyncTask<String, Void, Void> {
	        protected Void doInBackground(String... items) {
	                try {
	                        String sUrl = items[0];
	                    URL url = new URL(sUrl);
	                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	                    connection.setDoInput(true);
	                    connection.connect();
	                    InputStream input = connection.getInputStream();
	                    currentBitmap = BitmapFactory.decodeStream(input);
	                    if(currentBitmap == null)
	                            return null;
	                    
	                    drawBitmapInCanvas(null);
	                   
	                    return null;
	                } catch (Exception e) {
	                    e.printStackTrace();
	                    return null;
	                }
	        }
	        
	        protected void onPostExecute() {
	        }
	}

	private void drawBitmapInCanvas(Bitmap b){
	        Bitmap scaledBitmap;
	        if(b == null){
	                scaledBitmap = getResizedBitmap(currentBitmap,screenX,screenY);
	                storeWallpaperCacheImage(scaledBitmap);
	        }else
	                scaledBitmap = b;
	    //currentBitmap.recycle();
	    if (scaledBitmap != null){
	            SurfaceHolder holder = getSurfaceHolder();
	            Canvas canvas = holder.lockCanvas();
	            if(canvas != null){
	                    Paint paint = new Paint();
	                    int[] positions = getBitmapPositions(scaledBitmap,screenX, screenY);
	                    canvas.drawColor(Color.BLACK);
	                    canvas.drawBitmap(scaledBitmap,positions[0],positions[1], paint);
	                    holder.unlockCanvasAndPost(canvas);
	                    nextPositionToDisplay++;
	                    lastchange = System.currentTimeMillis();
	            }
	    }
	    if(scaledBitmap != null)
	            scaledBitmap.recycle();
	    return;
	}

	

		private void loadDefaultWallpaper(String text){
		        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		        View layout = inflater.inflate(R.layout.wallpaper_default, null);
		        TextView message = (TextView)layout.findViewById(R.id.wallpaper_default_text);
		        message.setText(text);
		        layout.setDrawingCacheEnabled(true);
		        
		        
		        SurfaceHolder holder = getSurfaceHolder();
		        Canvas canvas = holder.lockCanvas();
		        if(canvas != null){
		            layout.measure(MeasureSpec.makeMeasureSpec(canvas.getWidth(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(canvas.getHeight(), MeasureSpec.EXACTLY));
		            layout.layout(0, 0, layout.getMeasuredWidth(), layout.getMeasuredHeight());
		            canvas.drawBitmap(layout.getDrawingCache(), 0, 0, new Paint());
		                holder.unlockCanvasAndPost(canvas);
		        }
		    }
		
		 StatusListener statusListener = new StatusListener() {

			@Override
			public void onException(Exception arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onDeletionNotice(StatusDeletionNotice arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onScrubGeo(long arg0, long arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStallWarning(StallWarning arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStatus(Status status) {
				//Log.i("Tweetpaper","Status received: "+status.getText());
				if(status.getMediaEntities() != null){
        			for(MediaEntity media : status.getMediaEntities()){
        				if(media.getType().equals("photo")){
        					itemsArray.add(media.getMediaURL());
        					if(itemsArray.size() >= MAX_ITEMS)
        						itemsArray.remove(0);
        						
        				}
        			}
        		}
				
			}

			@Override
			public void onTrackLimitationNotice(int arg0) {
				// TODO Auto-generated method stub
				
			}
	        };
		
		
		class getTweetsByHashtag extends AsyncTask<Query, Void, ArrayList<String>> {
	        protected ArrayList<String> doInBackground(Query... query) {
	        	QueryResult qr;
	        	ArrayList<String> urlsArray = new ArrayList<String>();
	                try {
	                	qr = twitter.search(query[0]);
	                	if(qr != null){
		                	ArrayList<twitter4j.Status> qrTweets = (ArrayList<twitter4j.Status>)qr.getTweets(); 
		                	for(twitter4j.Status status : qrTweets){
		                		if(status.getMediaEntities() != null){
		                			for(MediaEntity media : status.getMediaEntities()){
		                				if(media.getType().equals("photo"))
		                					urlsArray.add(media.getMediaURL());
		                			}
		                		}
		                	}
		                	if(itemsArray.size() < 100 && qr.nextQuery() != null)
		                		new getTweetsByHashtag().execute(qr.nextQuery());
		                	else
		                		handler.postDelayed(new Runnable() {
			                		  @Override
			                		  public void run() {
			                			 new getTweetsByHashtag().execute(new Query(hashtag));
			                		  }
			                		}, interval);
		                		
	                	}else{
	                		handler.postDelayed(new Runnable() {
	                		  @Override
	                		  public void run() {
	                			 new getTweetsByHashtag().execute(new Query(hashtag));
	                		  }
	                		}, interval);
	                	}
	                    return urlsArray;
	                } catch (Exception e) {
	                	e.printStackTrace();
	                	
	                	handler.postDelayed(new Runnable() {
                		  @Override
                		  public void run() {
                			 new getTweetsByHashtag().execute(new Query(hashtag));
                		  }
                		}, interval);
	                    return null;
	                }
	        }
	        
	        protected void onPostExecute(ArrayList<String> urlsArray) {
	        	if(urlsArray != null && urlsArray.size() > 0){
	        		//Log.i("tweetpaper","Added "+urlsArray.size() + " by searching");
	        		itemsArray.addAll(urlsArray);
	        		if(itemsArray.size() >= MAX_ITEMS){
	        			for(int i=0;i<urlsArray.size();i++)
	        				itemsArray.remove(0);
	        		}
	        	}
	        	
	        }
	}
            
            public int[] getBitmapPositions(Bitmap bm,int screenX,int screenY){
                int[]positions = new int[2];
                
                if(bm.getWidth() > bm.getHeight()){
                        positions[0] = 0;
                        positions[1] = (screenY-bm.getHeight())/2;
                }else{
                        positions[0] = (screenX-bm.getWidth())/2;
                        positions[1] = 0;
                }
                return positions;
                        
        }
        public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
            int width = bm.getWidth();
            int height = bm.getHeight();
            float scaleWidth = ((float) newWidth) / width;
            float scaleHeight = ((float) newHeight) / height;

            if(width > height)
                    scaleHeight = scaleWidth;
            else
                    scaleWidth = scaleHeight;
            
            // Create a matrix for the manipulation
            Matrix matrix = new Matrix();

            // Resize the bit map
            matrix.postScale(scaleWidth, scaleHeight);

            // Recreate the new Bitmap
            Bitmap resizedBitmap = null;
            try{
                    resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
            }catch(OutOfMemoryError e){
                    System.gc();
            }

            return resizedBitmap;
        }
        
        private Bitmap getBitmapInCache(){
                Bitmap bitmap;
                try{
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                        bitmap = BitmapFactory.decodeFile(getWallpaperCachePath(), options);        
                }catch (Exception e){
                        return null;
                }catch (OutOfMemoryError e){
                        return null;
                }
                return bitmap;
        }
        
        private String getWallpaperCachePath(){
                return android.os.Environment.getExternalStorageDirectory() + "/" + Constants.TWEETPAPER_PREFS + "/wallpaperCache.jpg";
        }
        
        private void storeWallpaperCacheImage(Bitmap bitmap){
                try {
                ///Get Dir
                File tweetpaperDir = new File(android.os.Environment.getExternalStorageDirectory(), Constants.TWEETPAPER_PREFS);
                if (!tweetpaperDir.exists())
                	tweetpaperDir.mkdirs();
                
                String imagePath = getWallpaperCachePath();
                File fDest = new File(imagePath);
                FileOutputStream out = new FileOutputStream(fDest);
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                //Log.d(LOG_TAG,"Saved to cache: "+imagePath);
                }catch (Exception e) {
                    e.printStackTrace();
            } catch (OutOfMemoryError e) {
                    e.printStackTrace();
            }
        }
	}
} 