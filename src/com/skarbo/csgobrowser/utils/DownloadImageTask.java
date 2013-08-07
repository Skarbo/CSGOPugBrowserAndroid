package com.skarbo.csgobrowser.utils;

import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.skarbo.csgobrowser.handler.Handler;

public class DownloadImageTask extends AsyncTask<ImageView, Void, Bitmap> {

	private static final String TAG = DownloadImageTask.class.getSimpleName();

	private ImageView imageView = null;
	private Handler handler;

	public DownloadImageTask(Handler handler) {
		this.handler = handler;
	}

	@Override
	protected Bitmap doInBackground(ImageView... imageViews) {
		this.imageView = imageViews[0];
		return downloadImage((String) imageView.getTag());
	}

	@Override
	protected void onPostExecute(Bitmap result) {
		if (result != null)
			imageView.setImageBitmap(result);
	}

	private Bitmap downloadImage(String url) {
		Bitmap bitmapImage = null;
		try {
			Log.d(TAG, "downloadImage: " + url);
			InputStream inputStream = new java.net.URL(url).openStream();
			bitmapImage = BitmapFactory.decodeStream(inputStream);
			this.handler.addBitmapToCache(url, bitmapImage);
		} catch (Exception e) {
			Log.e(TAG, "DownloadImageTask: " + url + ", Message: " + e.getMessage());
		}

		return bitmapImage;
	}
}