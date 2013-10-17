package com.muzzley.stockalarmviewer;

import static android.provider.Settings.NameValueTable.VALUE;
import static android.provider.Settings.System.NEXT_ALARM_FORMATTED;
import static java.util.Calendar.DAY_OF_WEEK;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.util.Log;

public class ClockController {
  private static final Uri URI;
  private static final int[] FIELDS;
  private final ContentResolver resolver;
  private final ContentObserver observer;
  private final Context context;
  private Listener listener;

  private final static String DM12;
  private final static String DM24;
  private static final String TAG = "com.muzzley.stockalarmviewer";

  static {
    URI = Settings.System.getUriFor(NEXT_ALARM_FORMATTED);
    FIELDS = new int[] { DAY_OF_WEEK, HOUR_OF_DAY, MINUTE, SECOND };
    DM12 = "E h:mm aa";
    DM24 = "E k:mm";
  }

  public ClockController(Context context, Handler handler) {
    this.context = context;
    this.resolver = context.getContentResolver();
    this.observer = new ContentObserver(handler) {
      @Override
      public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        notifyListener();
      }
    };
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  private void notifyListener() {
    if (listener != null)
      listener.onChange();
  }

  private static Date parseDayAndTime(final Context context, String timeString) {
    String format = get24HourMode(context) ? DM24 : DM12;
    SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
    try {
      return sdf.parse(timeString);
    } catch (ParseException e) {
      format = (get24HourMode(context) ? DM24 : DM12).replace(":", "'h'");
      sdf = new SimpleDateFormat(format, Locale.getDefault());
      Log.d(TAG, e.getMessage(), e);
      try {
        return sdf.parse(timeString);
      } catch (ParseException pe) {
        Log.d(TAG, e.getMessage(), e);
        return null;
      }
    }
  }

  public Calendar getNextAlarm() {
    Cursor c = resolver.query(URI, null, null, null, null);
    if (c != null) {
      try {
        if (c.getCount() > 0) {
          c.moveToFirst();
          Calendar now = Calendar.getInstance();
          Date dt = parseDayAndTime(context, c.getString(c.getColumnIndex(VALUE)));
          
          if (dt == null) return null;
          
          // Since the date return only the time and day of the week
          // we need to inject the other values based on the date
          // right now
          Calendar al = Calendar.getInstance();
          for (int field : FIELDS) {
            al.set(field, now.get(field));
          }
          return al;
        }
      } finally {
        c.close();
      }
    }
    return null;
  }

  public void registerContentObserver() {
    resolver.registerContentObserver(URI, true, observer);
  }

  public void unregisterContentObserver() {
    resolver.unregisterContentObserver(observer);
  }

  static boolean get24HourMode(final Context context) {
    return android.text.format.DateFormat.is24HourFormat(context);
  }

  public void setAlarm(Calendar calendar) {
    Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .putExtra(AlarmClock.EXTRA_HOUR, calendar.get(Calendar.HOUR_OF_DAY))
        .putExtra(AlarmClock.EXTRA_MINUTES, calendar.get(Calendar.MINUTE))
        .putExtra(AlarmClock.EXTRA_SKIP_UI, true);
    context.startActivity(intent);
  }

  public static interface Listener {
    void onChange();
  }
}
