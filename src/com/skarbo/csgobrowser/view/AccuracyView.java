package com.skarbo.csgobrowser.view;

import java.io.IOException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class AccuracyView extends View {

	private static final String TAG = AccuracyView.class.getSimpleName();
	private static final float CROSSHAIR_SCALE = 0.4f; // 0.29f;

	private Rect crosshairImageDimentions = new Rect(0, 0, 723, 718);
	private Rect targetImageDimentions = new Rect(0, 0, 718, 718);
	private RectF crosshairDimentions;
	private RectF targetDimentions;
	private Bitmap crosshairImage;
	private Bitmap targetImage;
	private double accuracy = 0f;
	private float scale;
	private PointF targetCenter;
	private PointF targetOutside;
	private double accuracyMoveTo;
	private CountDownTimer crosshairCountDownTimer;
	private Paint paint = new Paint();

	public AccuracyView(Context context) {
		super(context);
		doInit();
	}

	public AccuracyView(Context context, AttributeSet attrs) {
		super(context, attrs);
		doInit();
	}

	public AccuracyView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		doInit();
	}

	// ... CALCULATE

	private RectF calculateCrosshairImageSize(float width) {
		float scale = width / crosshairImageDimentions.width();
		return new RectF(0, 0, this.crosshairImageDimentions.width() * scale, this.crosshairImageDimentions.height()
				* scale);
	}

	private RectF calculateTargetImageSize(float width) {
		float scale = width / targetImageDimentions.width();
		return new RectF(0, 0, this.targetImageDimentions.width() * scale, this.targetImageDimentions.height() * scale);
	}

	private double calculateProgress(double accuracy) {
		return Math.log10((accuracy * 9) + 1);
	}

	private PointF calculateCrosshairPoint(double accuracy) {
		float progress = (float) calculateProgress(accuracy);
		PointF length = new PointF(this.targetCenter.x - this.targetOutside.x, this.targetCenter.y
				- this.targetOutside.y);
		PointF point = new PointF((length.x * progress) + this.targetOutside.x, (length.y * progress)
				+ this.targetOutside.y);
		return point;
	}

	// ... /CALCULATE

	// ... DRAW

	private void drawCrosshair(Canvas canvas) {
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		PointF point = calculateCrosshairPoint(this.accuracy);
		canvas.translate(point.x, point.y);
		canvas.drawBitmap(this.crosshairImage, null, this.crosshairDimentions, null);
		canvas.restore();
	}

	private void drawTarget(Canvas canvas) {
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		canvas.drawBitmap(this.targetImage, null, this.targetDimentions, null);
		canvas.restore();
	}

	// ... /DRAW

	// ... ON

	@SuppressLint("DrawAllocation")
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		int width = widthSize > 0 && heightSize > 0 ? Math.min(widthSize, heightSize) : Math.max(widthSize, heightSize);

		this.targetDimentions = calculateTargetImageSize(width);
		this.crosshairDimentions = calculateCrosshairImageSize(width * CROSSHAIR_SCALE);
		this.scale = 1 - (this.crosshairDimentions.width() / width);
		this.targetOutside = new PointF((this.crosshairDimentions.width() / -2), 0);
		this.targetCenter = new PointF((this.crosshairDimentions.width() / -2) + (this.targetDimentions.width() / 2),
				(this.targetDimentions.height() / 2) - (this.crosshairDimentions.height() / 2));

		int height = (int) (width * scale);

		setMeasuredDimension(width, height);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
//		paint.setColor(Color.GRAY);
//		canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
		canvas.translate(this.crosshairDimentions.width() / 2, 0);
		canvas.scale(this.scale, this.scale);
		drawTarget(canvas);
		drawCrosshair(canvas);
		canvas.restore();
	}

	// ... /ON

	// ... DO

	private void doInit() {
		try {
			this.targetImage = BitmapFactory.decodeStream(getResources().getAssets().open("accuracy/target.png"));
			this.crosshairImage = BitmapFactory.decodeStream(getResources().getAssets().open("accuracy/crosshair.png"));
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}

	public void doSetAccuracy(double accuracyValue) {
		this.accuracyMoveTo = accuracyValue; // Math.max(Math.min(accuracyValue,
												// 0), 1);

		if (this.crosshairCountDownTimer != null)
			this.crosshairCountDownTimer.cancel();

		final int timeFinish = 5000;
		final int timeInterval = 100;
		this.crosshairCountDownTimer = new CountDownTimer(timeFinish, timeInterval) {

			public void onTick(long millisUntilFinished) {
				float progress = 1 - ((float) millisUntilFinished / (float) timeFinish);
				double sum = accuracyMoveTo - accuracy;
				double move = progress * sum;
				accuracy += move;
				postInvalidate();
			}

			public void onFinish() {
				accuracyMoveTo = accuracy;
				postInvalidate();
			}
		};
		this.crosshairCountDownTimer.start();
	}

	// ... /DO

}
