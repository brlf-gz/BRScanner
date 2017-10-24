/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.brlf.brscanner;

import com.google.zxing.ResultPoint;
import com.brlf.brscanner.camera.CameraManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

  private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
  private static final long ANIMATION_DELAY = 80L;
  private static final int CURRENT_POINT_OPACITY = 0xA0;
  private static final int MAX_RESULT_POINTS = 20;
  private static final int POINT_SIZE = 6;

  private CameraManager cameraManager;
  private final Paint paint;
  private Bitmap resultBitmap;
  private final int maskColor;
  private final int resultColor;
  private final int laserColor;
  private final int resultPointColor;
  private int scannerAlpha;
  private List<ResultPoint> possibleResultPoints;
  private List<ResultPoint> lastPossibleResultPoints;

  /////////////////////////////
  private Context mContext;
  private int scanLineTop = 0;
  private Bitmap scanLight;
  private boolean scanDirection;
  private int topHeight;
  private String mPromptText = "请将条码置于取景框内扫描";
  private int mTextMargin = 45;
  private Paint mTextPaint = new Paint();
  private float mTextSize = 20;

  // This constructor is used when the class is built from an XML resource.
  public ViewfinderView(Context context, AttributeSet attrs) {
    super(context, attrs);

    // Initialize these once for performance rather than calling them every time in onDraw().
    paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Resources resources = getResources();
    maskColor = resources.getColor(R.color.viewfinder_mask);
    resultColor = resources.getColor(R.color.result_view);
    laserColor = resources.getColor(R.color.viewfinder_laser);
    resultPointColor = resources.getColor(R.color.possible_result_points);
    scannerAlpha = 0;
    possibleResultPoints = new ArrayList<>(5);
    lastPossibleResultPoints = null;

    ////////////////////////////////////////////////////////////
    mContext = context;
    mTextPaint.setColor(Color.WHITE);
  }

  public void setCameraManager(CameraManager cameraManager) {
    this.cameraManager = cameraManager;
  }

  @SuppressLint("DrawAllocation")
  @Override
  public void onDraw(Canvas canvas) {

    //获取cameraManager中的Rectf对象来画扫码框和阴影
    if (cameraManager == null) {
      return; // not ready yet, early draw before done configuring
    }
    Rect frame = cameraManager.getFramingRect();
    Rect previewFrame = cameraManager.getFramingRectInPreview();    
    if (frame == null || previewFrame == null) {
      return;
    }
    int width = canvas.getWidth();
    int height = canvas.getHeight();

    // Draw the exterior (i.e. outside the framing rect) darkened
    //画扫码框周围的阴影，分成四部分来画
//    paint.setColor(resultBitmap != null ? resultColor : maskColor);
//    canvas.drawRect(0, 0, width, frame.top, paint);
//    canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
//    canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
//    canvas.drawRect(0, frame.bottom + 1, width, height, paint);

    DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
    Rect tempRect = new Rect();
    int realWidth = (int) (metrics.widthPixels * (365.f/720.f));
    int realHeight = realWidth;
    int leftOffset = (width - realWidth) / 2;
    int topOffset = (int) (metrics.heightPixels * (342.f/1280.f));
    tempRect.set(leftOffset, topOffset, leftOffset+realWidth, topOffset+realHeight);

    paint.setColor(resultBitmap != null ? resultColor : maskColor);
    canvas.drawRect(0, topHeight, width, tempRect.top, paint);
    canvas.drawRect(0, tempRect.top, tempRect.left, tempRect.bottom + 1, paint);
    canvas.drawRect(tempRect.right + 1, tempRect.top, width, tempRect.bottom + 1, paint);
    canvas.drawRect(0, tempRect.bottom + 1, width, height, paint);

    /***********************画四个角**********************/

    float mLineRate = 0.08767F;  //边角线的长度比例
    float mLineDepth =  tempRect.width() * 0.01096f;
    int mLineColor = Color.parseColor("#DA000F");  //边角线颜色

    //绘制4个角
    paint.setColor(mLineColor); // 定义画笔的颜色
    canvas.drawRect(tempRect.left, tempRect.top, tempRect.left + tempRect.width() * mLineRate, tempRect.top + mLineDepth, paint);
    canvas.drawRect(tempRect.left, tempRect.top, tempRect.left + mLineDepth, tempRect.top + tempRect.height() * mLineRate, paint);

    canvas.drawRect(tempRect.right - tempRect.width() * mLineRate, tempRect.top, tempRect.right, tempRect.top + mLineDepth, paint);
    canvas.drawRect(tempRect.right - mLineDepth, tempRect.top, tempRect.right, tempRect.top + tempRect.height() * mLineRate, paint);

    canvas.drawRect(tempRect.left, tempRect.bottom - mLineDepth, tempRect.left + tempRect.width() * mLineRate, tempRect.bottom, paint);
    canvas.drawRect(tempRect.left, tempRect.bottom - tempRect.height() * mLineRate, tempRect.left + mLineDepth, tempRect.bottom, paint);

    canvas.drawRect(tempRect.right - tempRect.width() * mLineRate, tempRect.bottom - mLineDepth, tempRect.right, tempRect.bottom, paint);
    canvas.drawRect(tempRect.right - mLineDepth, tempRect.bottom - tempRect.height() * mLineRate, tempRect.right, tempRect.bottom, paint);


    drawPromptText(tempRect, canvas);


    if (resultBitmap != null) {
      // Draw the opaque result bitmap over the scanning rectangle
      paint.setAlpha(CURRENT_POINT_OPACITY);
      canvas.drawBitmap(resultBitmap, null, frame, paint);
    } else {

      // Draw a red "laser scanner" line through the middle to show decoding is active
      //画扫描线
//      paint.setColor(laserColor);
//      paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
//      scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
//      int middle = frame.height() / 2 + frame.top;
//      canvas.drawRect(frame.left + 2, middle - 1, frame.right - 1, middle + 2, paint);
//
//      float scaleX = frame.width() / (float) previewFrame.width();
//      float scaleY = frame.height() / (float) previewFrame.height();
//
//      List<ResultPoint> currentPossible = possibleResultPoints;
//      List<ResultPoint> currentLast = lastPossibleResultPoints;
//      int frameLeft = frame.left;
//      int frameTop = frame.top;
//      if (currentPossible.isEmpty()) {
//        lastPossibleResultPoints = null;
//      } else {
//        possibleResultPoints = new ArrayList<>(5);
//        lastPossibleResultPoints = currentPossible;
//        paint.setAlpha(CURRENT_POINT_OPACITY);
//        paint.setColor(resultPointColor);
//        synchronized (currentPossible) {
//          for (ResultPoint point : currentPossible) {
//            canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
//                              frameTop + (int) (point.getY() * scaleY),
//                              POINT_SIZE, paint);
//          }
//        }
//      }
//      if (currentLast != null) {
//        paint.setAlpha(CURRENT_POINT_OPACITY / 2);
//        paint.setColor(resultPointColor);
//        synchronized (currentLast) {
//          float radius = POINT_SIZE / 2.0f;
//          for (ResultPoint point : currentLast) {
//            canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
//                              frameTop + (int) (point.getY() * scaleY),
//                              radius, paint);
//          }
//        }
//      }

      //修改扫描线为图片形式
      drawScanLight(canvas, tempRect, (int)(realHeight*0.1));

      // Request another update at the animation interval, but only repaint the laser line,
      // not the entire viewfinder mask.
      postInvalidateDelayed(ANIMATION_DELAY,
                            frame.left - POINT_SIZE,
                            frame.top - POINT_SIZE,
                            frame.right + POINT_SIZE,
                            frame.bottom + POINT_SIZE);
    }
  }

  public void drawViewfinder() {
    Bitmap resultBitmap = this.resultBitmap;
    this.resultBitmap = null;
    if (resultBitmap != null) {
      resultBitmap.recycle();
    }
    invalidate();
  }

  /**
   * Draw a bitmap with the result points highlighted instead of the live scanning display.
   *
   * @param barcode An image of the decoded barcode.
   */
  public void drawResultBitmap(Bitmap barcode) {
    resultBitmap = barcode;
    invalidate();
  }

  public void addPossibleResultPoint(ResultPoint point) {
    List<ResultPoint> points = possibleResultPoints;
    synchronized (points) {
      points.add(point);
      int size = points.size();
      if (size > MAX_RESULT_POINTS) {
        // trim it
        points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
      }
    }
  }

  /////////////////////////////////////////////////////////
  //画图片扫描线
  private void drawScanLight(Canvas canvas, Rect frame, int scanHeight) {

    if (scanLineTop == 0) {
      scanLineTop = frame.top;
      scanDirection = true;
    }

    if(scanDirection) {
      if (scanLineTop >= frame.bottom - scanHeight) {
        //scanLineTop = frame.top;
        scanDirection = false;
      } else {
        scanLineTop += 5;
      }
    }
    else {
      if (scanLineTop <= frame.top) {
        scanDirection = true;
      } else {
        scanLineTop -= 5;
      }
    }

    Rect scanRect = new Rect(frame.left, scanLineTop, frame.right, scanLineTop + scanHeight);

    if(scanLight == null){
      scanLight = BitmapFactory.decodeResource(getResources(), R.drawable.scan_line);
    }
    canvas.drawBitmap(scanLight, null, scanRect, paint);
  }

  //画提示语
  private void drawPromptText(Rect frame, Canvas canvas) {
    mTextPaint.setTextSize(sp2px(mTextSize));
    mTextPaint.setTextAlign(Paint.Align.CENTER);
    mTextPaint.setAlpha(255);

    int startX = frame.left + frame.width() / 2;
    int startY = frame.bottom + mTextMargin;
    if (!TextUtils.isEmpty(mPromptText)) {
      canvas.drawText(mPromptText, startX, startY, mTextPaint);
    }
  }

  public void setTopHeight(int topHeight){
    this.topHeight = topHeight;
  }

  public void setPromptText(String promptText){
    if(!TextUtils.isEmpty(promptText)){
      this.mPromptText = promptText;
    }
  }

  public void setTextMargin(int textMargin){
    if(textMargin >0){
      this.mTextMargin = textMargin;
    }
  }

  public void setPromptTextColor(String color) {
    if(!TextUtils.isEmpty(color)) {
      mTextPaint.setColor(Color.parseColor(color));
    }
  }

  public void setPromptTextSize(float size){
    if(size > 0) {
      this.mTextSize = size;
    }
  }

  private int sp2px(float spValue) {
    final float scale = getContext().getResources().getDisplayMetrics().scaledDensity;
    return (int) (spValue * scale + 0.5f);
  }

}
