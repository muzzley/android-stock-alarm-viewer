package com.muzzley.stockalarmviewer;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static android.animation.ValueAnimator.ofObject;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB;

public class ClockActivity extends Activity implements ClockController.Listener {
  private static final DateFormat FORMAT = new SimpleDateFormat("EEE HH:mm");
  private TextView label;
  private ClockController controller;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_clock);

    controller = new ClockController(this, new Handler(Looper.getMainLooper()));
    controller.setListener(this);

    label = (TextView) findViewById(R.id.clock);
    findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        Calendar now = Calendar.getInstance();
        now.add(Calendar.MINUTE, 5);
        controller.setAlarm(now);
      }
    });
  }

  @Override protected void onResume() {
    super.onResume();

    updateNextClockAlarm();
    controller.registerContentObserver();
  }

  @Override protected void onPause() {
    super.onPause();
    controller.unregisterContentObserver();
  }

  @Override public void onChange() {
    updateNextClockAlarm();
  }

  private void updateNextClockAlarm() {
    Calendar calendar = controller.getNextAlarm();
    if (calendar == null) {
      label.setText(getText(R.string.next_alarm_not_available));
    } else {
      label.setText(FORMAT.format(calendar.getTime()));
    }

    if (SDK_INT < HONEYCOMB) return;

    label.setTextColor(getResources().getColor(android.R.color.tertiary_text_light));
    Integer colorFrom = getResources().getColor(android.R.color.tertiary_text_light);
    Integer colorTo = getResources().getColor(android.R.color.primary_text_light);
    ValueAnimator animator = ofObject(new ArgbEvaluator(), colorFrom, colorTo);
    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animator) {
        label.setTextColor((Integer) animator.getAnimatedValue());
      }
    });
    animator.setDuration(200).start();
  }
}
