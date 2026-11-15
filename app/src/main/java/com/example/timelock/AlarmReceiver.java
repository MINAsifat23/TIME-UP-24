package com.example.timelock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        String text = intent.getStringExtra("text");
        int id = intent.getIntExtra("id", 0);
        if (text == null) text = "";
        final String say = "মিস্টার শাওন, আজ আপনার রিমাইন্ডার আছে। " + text;
        showNote(ctx, id, text);

        try {
            new ToneGenerator(AudioManager.STREAM_ALARM, 100)
                    .startTone(ToneGenerator.TONE_PROP_PROMPT, 300);
        } catch (Exception e) { }

        final PendingResult pr = goAsync();
        final AtomicBoolean done = new AtomicBoolean(false);
        final TextToSpeech[] box = new TextToSpeech[1];

        box[0] = new TextToSpeech(ctx.getApplicationContext(), status -> {
            final TextToSpeech t = box[0];
            if (status != TextToSpeech.SUCCESS || t == null) {
                if (done.compareAndSet(false, true)) pr.finish();
                return;
            }
            int r = t.setLanguage(new Locale("bn", "BD"));
            if (r == TextToSpeech.LANG_MISSING_DATA
                    || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                t.setLanguage(Locale.getDefault());
            }
            t.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
            t.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String s) { }
                @Override public void onDone(String s) {
                    t.shutdown();
                    if (done.compareAndSet(false, true)) pr.finish();
                }
                @Override public void onError(String s) {
                    t.shutdown();
                    if (done.compareAndSet(false, true)) pr.finish();
                }
            });
            new Handler(Looper.getMainLooper()).postDelayed(
                    () -> t.speak(say, TextToSpeech.QUEUE_FLUSH, null, "r1"), 600);
        });

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (done.compareAndSet(false, true)) pr.finish();
        }, 20000);
    }

    private void showNote(Context ctx, int id, String text) {
        NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(
                    "reminders", "Reminders", NotificationManager.IMPORTANCE_HIGH));
            b = new Notification.Builder(ctx, "reminders");
        } else {
            b = new Notification.Builder(ctx);
        }
        b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Time Lock")
                .setContentText(text)
                .setAutoCancel(true);
        nm.notify(id, b.build());
    }
}
