package com.tweetpaper.service;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;

import com.tweetpaper.utils.Constants;
import com.tweetpaper.utils.TweetpaperUtils;

public class TweetpaperService extends WallpaperService {

        
        Context context;
  @Override
  public Engine onCreateEngine() {
	context = this;
    return new PixableWallpaperEngine();
  }

  private class PixableWallpaperEngine extends Engine {
    private final Handler handler = new Handler();
    private final Runnable drawRunner = new Runnable() {
      @Override
      public void run() {
        String hashtag = TweetpaperUtils.getHashTag(context);
        draw();
      }

    };
    private boolean visible = true;
    private int screenX;
    private int screenY;
    private int interval;

    public PixableWallpaperEngine() {
      DisplayMetrics metrics = getApplicationContext().getResources().getDisplayMetrics();
      screenX = metrics.widthPixels;
      screenY = metrics.heightPixels;
        
      interval = TweetpaperUtils.getInterval(context);
        
      handler.post(drawRunner);
    }

    @Override
    public void onVisibilityChanged(boolean visible) {
      this.visible = visible;
      if (visible) {
        handler.post(drawRunner);
      } else {
        handler.removeCallbacks(drawRunner);
      }
    }
    @Override
    public void onSurfaceDestroyed(SurfaceHolder holder) {
      super.onSurfaceDestroyed(holder);
      this.visible = false;
      handler.removeCallbacks(drawRunner);
    }

    @Override
    public void onSurfaceChanged(SurfaceHolder holder, int format,
        int width, int height) {
      screenX = width;
      screenY = height;
      super.onSurfaceChanged(holder, format, width, height);
    }

    private void draw() {
      new loadBitmapFromUrl().execute("http://upload.wikimedia.org/wikipedia/en/archive/b/be/20130511175240!Real_Madrid_Baloncesto.jpg");
      handler.removeCallbacks(drawRunner);
      if (visible) {
        handler.postDelayed(drawRunner, interval);
      }
    }
    
    class loadBitmapFromUrl extends AsyncTask<String, Void, Bitmap> {
            protected Bitmap doInBackground(String... urls) {
                    try {
                        URL url = new URL(urls[0]);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream input = connection.getInputStream();
                        Bitmap myBitmap = BitmapFactory.decodeStream(input);
                        if(myBitmap == null)
                                return null;
                        Bitmap scaledBitmap = ThumbnailUtils.extractThumbnail(myBitmap, screenX, screenY);
                        myBitmap.recycle();
                        if (scaledBitmap != null){
                                SurfaceHolder holder = getSurfaceHolder();
                                Canvas canvas = holder.lockCanvas();
                                if(canvas != null){
                                        Paint paint = new Paint();
                                        canvas.drawBitmap(scaledBitmap, 0, 0, paint);
                                        holder.unlockCanvasAndPost(canvas);
                                }
                        }
                        return scaledBitmap;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
            }
            
            protected void onPostExecute(Bitmap b) {
                    b.recycle();
            }
    }
  }
} 