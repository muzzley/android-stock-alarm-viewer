package com.muzzley.stockalarmviewer;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.AlarmClock;
import android.provider.Settings;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static android.provider.Settings.System.NEXT_ALARM_FORMATTED;
import static android.provider.Settings.System.VALUE;
import static java.util.Calendar.DAY_OF_WEEK;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;

public class ClockController {
  private static final Uri URI = Settings.System.getUriFor(NEXT_ALARM_FORMATTED);
  private static final int[] FIELDS = new int[]{DAY_OF_WEEK, HOUR_OF_DAY, MINUTE, SECOND};
  private static final DateFormat FORMAT = new SimpleDateFormat("EEE HH:mm");
  private final ContentResolver resolver;
  private final ContentObserver observer;
  private final Context context;
  private Listener listener;

  public ClockController(Context context, Handler handler) {
    this.context = context;
    this.resolver = context.getContentResolver();
    this.observer = new ContentObserver(handler) {
      @Override public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        notifyListener();
      }
    };
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  private void notifyListener() {
    if (listener != null) listener.onChange();
  }

  public Calendar getNextAlarm() {
    Cursor c = resolver.query(URI, null, null, null, null);
    if (c != null) {
      try {
        if (c.getCount() > 0) {
          c.moveToFirst();
          try {
            Date dt = FORMAT.parse(c.getString(c.getColumnIndex(VALUE)));
            Calendar now = Calendar.getInstance();
            now.setTime(dt);

            // Since the date returned is of format 'EEE HH:mm'
            // we need to inject the other values based on the date right now
            Calendar al = Calendar.getInstance();
            for (int field : FIELDS) {
              al.set(field, now.get(field));
            }
            return al;
          } catch (ParseException e) {
            return null;
          }
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
