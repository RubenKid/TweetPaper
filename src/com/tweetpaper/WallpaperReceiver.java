package com.tweetpaper;

import com.tweetpaper.service.TweetpaperService;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WallpaperReceiver extends BroadcastReceiver {
 
         @Override
         public void onReceive(Context context, Intent intent) {
                 WallpaperInfo wallpaperInfo = WallpaperManager.getInstance(context).getWallpaperInfo();
                 if(wallpaperInfo != null && (wallpaperInfo.getComponent().getClassName().startsWith("com.tweetpaper"))){
                                Intent myIntent = new Intent(context, TweetpaperService.class);
                                context.startService(myIntent);
                 }
         }
}