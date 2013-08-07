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

public class GaugeView extends View {

	private static final int SPLITTER_COLOR = Color.parseColor("#CCCCCC");
	private static final String TAG = GaugeView.class.getSimpleName();
	private static final float SPLITTER_WIDTH = 0.005f;
	private static final int ROTATION_OFFSET = 0;
	private static final float NEEDLE_OFFSET = 0.041f;
	private static final float GAUGE_OFFSET = 0.027f;
	private static final int ANGLE_MIN = -90;
	private static final int ANGLE_MAX = 90;
	private static final int SPAN = 180;
	private static final int SPAN_START = -90;

	Paint paint = new Paint();
	private Rect needleImageDimentions = new Rect(0, 0, 112, 960);
	private Rect gaugeImageDimentions = new Rect(0, 0, 936, 469);
	private Bitmap needleImage;
	private Bitmap gaugeImage;
	private RectF needleDimentions;
	private RectF gaugeDimentions;
	private PointF rotatePoint;

	private float needleValue = 0f;
	private float needleMoveToValue = 0f;
	private float minValue = 0f;
	private float maxValue = 2f;
	private float splitterValue = 1f;
	private CountDownTimer needleCountDownTimer;

	public GaugeView(Context context) {
		super(context);
		doInit();
	}

	public GaugeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		doInit();
	}

	public GaugeView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		doInit();
	}

	// ... SET

	public void setMinValue(float valueMin) {
		this.minValue = valueMin;
	}

	public void setMaxValue(float valueMax) {
		this.maxValue = valueMax;
	}

	public void setSplitterValue(float valueSplitter) {
		this.splitterValue = valueSplitter;
	}

	public void setValue(float value) {
		this.needleValue = Math.max(Math.min(value, this.maxValue), this.minValue);
	}

	// ... /SET

	// ... CALCULATE

	private float calculateNeedleImageWidth(float height) {
		float scale = height / needleImageDimentions.height();
		return needleImageDimentions.width() * scale;
	}

	private float calculateGaugeImageHeight(float width) {
		float scale = width / gaugeImageDimentions.width();
		return gaugeImageDimentions.height() * scale;
	}

	private int calculateAngle(float value) {
		float sum = Math.abs(this.maxValue - this.minValue);
		float procent = value / sum;
		return Math.min(Math.max((int) ((procent * SPAN) + SPAN_START), ANGLE_MIN), ANGLE_MAX);
	}

	private int calculateNeedleAngle() {
		return calculateAngle(this.needleValue);
	}

	private int calculateSplitterAngle() {
		return calculateAngle(this.splitterValue);
	}

	// ... /CALCULATE

	// ... ON

	@SuppressLint("DrawAllocation")
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		int width = widthSize > 0 && heightSize > 0 ? Math.min(widthSize, heightSize) : Math.max(widthSize, heightSize);

		float gagueOffset = GAUGE_OFFSET * width;
		this.gaugeDimentions = new RectF(0, gagueOffset, width, calculateGaugeImageHeight(width));
		float needleHeight = this.gaugeDimentions.height() + (NEEDLE_OFFSET * width) + gagueOffset;
		this.needleDimentions = new RectF(0, 0, calculateNeedleImageWidth(needleHeight), needleHeight);
		this.rotatePoint = new PointF(this.gaugeDimentions.width() / 2, this.gaugeDimentions.height() + ROTATION_OFFSET
				+ gagueOffset);

		int height = (int) (this.needleDimentions.bottom);
		Log.d(TAG, "onMeasure: " + widthSize + ", " + heightSize + ", " + width + ", " + height);
		setMeasuredDimension(width, height);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		// paint.setColor(Color.GRAY);
		// canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
		drawGauge(canvas);
		drawSplitter(canvas);
		drawNeedle(canvas);
		canvas.restore();
	}

	// ... /ON

	// ... DRAW

	private void drawGauge(Canvas canvas) {
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		canvas.drawBitmap(this.gaugeImage, null, this.gaugeDimentions, null);
		canvas.restore();
	}

	private void drawSplitter(Canvas canvas) {
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		paint.setColor(SPLITTER_COLOR);
		canvas.rotate(calculateSplitterAngle(), this.rotatePoint.x, this.rotatePoint.y);
		canvas.drawRect(this.rotatePoint.x - (SPLITTER_WIDTH * getWidth()), 0, this.rotatePoint.x
				+ (SPLITTER_WIDTH * getWidth()), this.rotatePoint.y, paint);
		canvas.restore();
	}

	private void drawNeedle(Canvas canvas) {
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		canvas.rotate(calculateNeedleAngle(), this.rotatePoint.x, this.rotatePoint.y);
		canvas.translate(this.rotatePoint.x - (this.needleDimentions.width() / 2), 0);
		canvas.drawBitmap(needleImage, null, this.needleDimentions, null);
		canvas.restore();

		// canvas.save(Canvas.MATRIX_SAVE_FLAG);
		// paint.setColor(Color.RED);
		// canvas.drawCircle(this.rotatePoint.x, this.rotatePoint.y, 10, paint);
		// canvas.restore();
	}

	// ... /DRAW

	// ... DO

	private void doInit() {
		try {
			this.needleImage = BitmapFactory.decodeStream(getResources().getAssets().open("gauge/needle.png"));
			this.gaugeImage = BitmapFactory.decodeStream(getResources().getAssets().open("gauge/gauge.png"));			
		} catch (IOException e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}

	public void doSetValue(float needleValueSet) {
		this.needleMoveToValue = Math.max(Math.min(needleValueSet, this.maxValue), this.minValue);

		if (this.needleCountDownTimer != null)
			this.needleCountDownTimer.cancel();

		final int timeFinish = 2000;
		final int timeInterval = 10;
		this.needleCountDownTimer = new CountDownTimer(timeFinish, timeInterval) {

			public void onTick(long millisUntilFinished) {
				float progress = 1 - ((float) millisUntilFinished / (float) timeFinish);
				float sum = needleMoveToValue - needleValue;
				float move = progress * sum;
				needleValue += move;
				postInvalidate();
			}

			public void onFinish() {
				needleMoveToValue = needleValue;
				postInvalidate();
			}
		};
		this.needleCountDownTimer.start();
	}

	// ... /DO

}
