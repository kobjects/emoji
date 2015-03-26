package org.kobjects.android;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class TextHelper {
  private static final int EMOJI_START = 0x1f300;
  private static final int EMOJI_END = 0x1ff00;
  private static final int FALLBACK_SIZE = 64;
  private static final int CHARACTERS_PER_LINE = 18;
  private static Bitmap[] fallbackCache;
  private static boolean fallback = android.os.Build.VERSION.SDK_INT < 
      android.os.Build.VERSION_CODES.KITKAT;
  private static Rect rect = new Rect();
  private static BitmapRegionDecoder regionDecoder;

  public static Bitmap fallbackBitmap(Context context, int codepoint) {
    if (codepoint < EMOJI_START || codepoint >= EMOJI_END) {
      return null;
    }
    if (fallbackCache == null) {
      fallbackCache = new Bitmap[EMOJI_END - EMOJI_START];
    }
    int index = codepoint - EMOJI_START;
    Bitmap bitmap = fallbackCache[codepoint - EMOJI_START];
    if (bitmap == null) {
      if (regionDecoder == null) {
        try {
          InputStream is = context.getResources().openRawResource(
              org.kobjects.R.drawable.emoji_fallback);
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
  
  
  public static void drawText(Context context, Canvas canvas, CharSequence text, int start, int end, float x, float y, Paint paint) {
    int pos = start;
    if (fallback) {
      while (pos < end) {
        int codepoint = Character.codePointAt(text, pos);
        if (codepoint >= EMOJI_START && codepoint < EMOJI_END) {
          canvas.drawText(text, start, pos, x, y, paint);
          x += paint.measureText(text, start, pos);
          Bitmap bitmap = fallbackBitmap(context, codepoint);
          int size = Math.round(paint.descent() - paint.ascent());
          rect.left = (int) x;
          rect.top = (int) (y + paint.ascent());
          rect.right = rect.left + size;
          rect.bottom = rect.top + size;
          canvas.drawBitmap(bitmap, null, rect, paint);
          x += size;
          pos += codepoint > 0xffff ? 2 : 1;
          start = pos; 
        } else {
          pos += codepoint > 0xffff ? 2 : 1;
        }
      }
    }
    canvas.drawText(text, pos, end, x, y, paint);
  }
  
}
