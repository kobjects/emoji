package org.kobjects.emoji.android;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;


/**
 * Provides a set of static drawText methods with support for vertical alignment
 * and emoji fallback images.
 */
public class TextHelper {
  private static final int RED = 0xffed6c30;
  private static final int GREEN = 0xffbdcf46;

  public enum VerticalAlign {
	  TOP, CENTER, BASELINE, BOTTOM
  };
  
  private enum Fallback {
	  NONE, COLOR_HEARTS_ONLY, FULL
  };
	
  private static final int FALLBACK_START = 0x1f300;
  private static final int FALLBACK_END = 0x1ff00;
  private static final int FALLBACK_SIZE = 64;
  private static final int CHARACTERS_PER_LINE = 18;
  private static Bitmap[] fallbackCache;
  private static Fallback fallback = 
		  android.os.Build.VERSION.SDK_INT >= 
		  	android.os.Build.VERSION_CODES.LOLLIPOP ? Fallback.NONE :
	      android.os.Build.VERSION.SDK_INT >= 
	      	android.os.Build.VERSION_CODES.KITKAT ? Fallback.COLOR_HEARTS_ONLY : 
	    	  Fallback.FULL;
  private static Rect rect = new Rect();
  private static BitmapRegionDecoder regionDecoder;
  private static Paint shapePaint = new Paint();

  public static Bitmap fallbackBitmap(Context context, int codepoint) {
    if (codepoint < FALLBACK_START || codepoint >= FALLBACK_END) {
      return null;
    }
    if (fallbackCache == null) {
      fallbackCache = new Bitmap[FALLBACK_END - FALLBACK_START];
    }
    int index = codepoint - FALLBACK_START;
    Bitmap bitmap = fallbackCache[codepoint - FALLBACK_START];
    if (bitmap == null) {
      if (regionDecoder == null) {
        try {
          InputStream is = context.getAssets().open("emoji_fallback.png");
          regionDecoder = BitmapRegionDecoder.newInstance(is, false);
        } catch(IOException e) {
          throw new RuntimeException(e);
        }
      }
      rect.left = (index % CHARACTERS_PER_LINE) * FALLBACK_SIZE;
      rect.top = (index / CHARACTERS_PER_LINE) * FALLBACK_SIZE;
      rect.right = rect.left + FALLBACK_SIZE;
      rect.bottom = rect.top + FALLBACK_SIZE;
      bitmap = regionDecoder.decodeRegion(rect, null);
      fallbackCache[index] = bitmap;
    }
    return bitmap;
  }
  
  public static void drawBoolean(Canvas canvas, boolean value, float cx, float cy,
      float radius, Paint paint) {
    shapePaint.setColor(value ? GREEN : RED);
    if (paint != null) {
      shapePaint.setAlpha(paint.getAlpha());
    }
    canvas.drawCircle(cx, cy, radius, shapePaint);
    shapePaint.setColor(shapePaint.getColor() | 0x0ffffff);
    shapePaint.setTypeface(Typeface.DEFAULT_BOLD);
    shapePaint.setTextAlign(Align.CENTER);
    shapePaint.setTextSize(radius * 3 / 2);
    
    shapePaint.setStyle(Style.STROKE);
    shapePaint.setStrokeWidth(radius / 4);
    shapePaint.setStrokeCap(value ? Cap.ROUND : Cap.BUTT);
    float d = radius / 2;
    if (value) {
      canvas.drawLine(cx - d, cy, cx-d/4, cy + d, shapePaint);
      canvas.drawLine(cx-d/4, cy + d, cx + d, cy - d, shapePaint);
    } else {
      canvas.drawLine(cx - d, cy - d, cx + d, cy + d, shapePaint);
      canvas.drawLine(cx - d, cy + d, cx + d, cy - d, shapePaint);
    }
    //drawText(null, canvas, value ? "\u2713" : "\u2715", cx, cy, shapePaint, VerticalAlign.CENTER);

    shapePaint.setStyle(Style.FILL);
  }
  
  public static void drawText(Context context, Canvas canvas, CharSequence text, 
		  float x, float y, Paint paint) {
	  drawText(context, canvas, text, 0, text.length(), x, y, paint, VerticalAlign.BASELINE);
  }
  
  public static void drawText(Context context, Canvas canvas, CharSequence text, 
		  float x, float y, Paint paint, VerticalAlign verticalAlign) {
	  drawText(context, canvas, text, 0, text.length(), x, y, paint, verticalAlign);
  }

  public static void drawText(Context context, Canvas canvas, CharSequence text, int start, int end, 
		  float x, float y, Paint paint) {
	  drawText(context, canvas, text, start, end, x, y, paint, VerticalAlign.BASELINE);
  }

  public static void getTextBounds(Paint paint, String text, VerticalAlign verticalAlign, RectF rectF) {
    getTextBounds(paint, text, 0, text.length(), verticalAlign, rectF);
  }

  public static void getTextBounds(Paint paint, String text, int start, int end, VerticalAlign verticalAlign, RectF rectF) {
    float ascent = Math.abs(paint.ascent());
    float descent = Math.abs(paint.descent());
    float size = ascent + descent;
    Align horizontalAlign = paint.getTextAlign();
    float width = measureText(paint, text, start, end);
    switch(horizontalAlign) {
      case LEFT: rectF.left = 0; rectF.right = width; break;
      case RIGHT: rectF.left = -width; rectF.right = 0; break;
      case CENTER: rectF.left = -width / 2; rectF.right = width / 2; break;
    }
    switch(verticalAlign) {
      case CENTER: rectF.top = -size / 2; rectF.bottom = size / 2; break;
      case BASELINE: rectF.top = -ascent; rectF.bottom = descent; break;
      case BOTTOM: rectF.top = -size; rectF.bottom = 0; break;
      case TOP: rectF.top = 0; rectF.bottom = size; break;
    }
  }

  public static void drawText(Context context, Canvas canvas, CharSequence text, int start, int end,
		  float x, float y, Paint paint, VerticalAlign verticalAlign) {
    float ascent = Math.abs(paint.ascent());
    float descent = Math.abs(paint.descent());
    float size = ascent + descent;
    Align horizontalAlign = paint.getTextAlign();
    switch(horizontalAlign) {
    case LEFT: break;
    case RIGHT: x -= measureText(paint, text, start, end); break;
    case CENTER: x -= measureText(paint, text, start, end) / 2; break;
    }
    switch(verticalAlign) {
    case CENTER: y -= size / 2f; break;
    case BASELINE: y -= ascent; break;
    case BOTTOM: y -= size; break;
    case TOP: y += 0; break;
    }
    paint.setTextAlign(Align.LEFT);
    int alpha = paint.getColor() & 0xff000000;
    int pos = start;
    while (pos < end) {
      int codepoint = Character.codePointAt(text, pos);
      if (codepoint == 0xf888 || codepoint == 0xf889 ||
          (fallback == Fallback.FULL && codepoint >= FALLBACK_START && codepoint < FALLBACK_END) || 
          fallback == Fallback.COLOR_HEARTS_ONLY && codepoint >= 0x1F499 && codepoint <= 0x1f49c) {
        canvas.drawText(text, start, pos, x, y + ascent, paint);
        x += paint.measureText(text, start, pos);
        switch (codepoint) {
        case 0xf888:
          drawBoolean(canvas, false, x + size / 2, y + size / 2, size / 2, paint);
          break;
        case 0xf889:
          drawBoolean(canvas, true, x + size / 2, y + size / 2, size / 2, paint);
          break;
        default:
          Bitmap bitmap = fallbackBitmap(context, codepoint);
          rect.left = (int) x;
          rect.top = (int) y;
          rect.right = (int) (x + size);
          rect.bottom = (int) (y + size);
          canvas.drawBitmap(bitmap, null, rect, paint);
        }
        x += size;
        pos += codepoint > 0x10000 ? 2 : 1;
        start = pos; 
      } else {
        pos += codepoint > 0x10000 ? 2 : 1;
      }
    }
    canvas.drawText(text, start, end, x, y + ascent, paint);
    paint.setTextAlign(horizontalAlign);
  }

  
  public static float measureText(Paint paint, CharSequence text) {
    return measureText(paint, text, 0, text.length());
  }
  
  public static float measureText(Paint paint, CharSequence text, int start, int end) {
    int pos = start;
    float ascent = Math.abs(paint.ascent());
    float descent = Math.abs(paint.descent());
    float size = ascent + descent;
    float width = 0;
    while (pos < end) {
      int codepoint = Character.codePointAt(text, pos);
      if (codepoint == 0xf888 || codepoint == 0xf889 ||
          (fallback == Fallback.FULL && codepoint >= FALLBACK_START && codepoint < FALLBACK_END)) {
        width += paint.measureText(text, start, pos) + size;
        pos += codepoint >= 0x10000 ? 2 : 1;
        start = pos;
      } else {
        pos += codepoint > 0x10000 ? 2 : 1;
      }
    }
    return width + paint.measureText(text, start, end);
  }
  
  
}
