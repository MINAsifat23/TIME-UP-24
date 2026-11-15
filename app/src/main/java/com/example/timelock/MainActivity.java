package com.example.timelock;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    static final int GREEN = Color.parseColor("#1B8A3E");
    static final String USER = "SifatMina";
    Calendar when = Calendar.getInstance();
    Button dateBtn, timeBtn;
    EditText input;
    LinearLayout listBox;
    SimpleDateFormat fmt =
            new SimpleDateFormat("EEEE, dd MMM yyyy, hh:mm a", new Locale("bn", "BD"));

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }
        when.set(Calendar.SECOND, 0);
        when.add(Calendar.HOUR_OF_DAY, 1);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        ImageView cover = new ImageView(this);
        cover.setImageResource(R.drawable.cover);
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        int h = (int) (180 * getResources().getDisplayMetrics().density);
        cover.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, h));
        root.addView(cover);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(32, 24, 32, 32);
        root.addView(body);

        TextView title = new TextView(this);
        title.setText("Time Lock");
        title.setTextSize(28);
        title.setTextColor(GREEN);
        title.setGravity(Gravity.CENTER);
        body.addView(title);

        TextView hello = new TextView(this);
        hello.setText("স্বাগতম, " + USER);
        hello.setTextSize(16);
        hello.setGravity(Gravity.CENTER);
        body.addView(hello);

        input = new EditText(this);
        input.setHint("কাজের কথা লিখুন");
        body.addView(input);

        dateBtn = new Button(this);
        dateBtn.setOnClickListener(v -> new DatePickerDialog(this,
                (dp, y, mo, d) -> { when.set(y, mo, d); refresh(); },
                when.get(Calendar.YEAR), when.get(Calendar.MONTH),
                when.get(Calendar.DAY_OF_MONTH)).show());
        body.addView(dateBtn);

        timeBtn = new Button(this);
        timeBtn.setOnClickListener(v -> new TimePickerDialog(this,
                (tp, hr, mi) -> { when.set(Calendar.HOUR_OF_DAY, hr);
                    when.set(Calendar.MINUTE, mi); when.set(Calendar.SECOND, 0); refresh(); },
                when.get(Calendar.HOUR_OF_DAY), when.get(Calendar.MINUTE), false).show());
        body.addView(timeBtn);

        Button save = new Button(this);
        save.setText("সেভ করুন");
        save.setBackgroundColor(GREEN);
        save.setTextColor(Color.WHITE);
        save.setOnClickListener(v -> saveNote());
        body.addView(save);

        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        body.addView(listBox);

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);
        refresh();
        welcomeOnce();
    }

    void welcomeOnce() {
        SharedPreferences sp = getSharedPreferences("notes", MODE_PRIVATE);
        if (sp.getBoolean("welcomed", false)) return;
        sp.edit().putBoolean("welcomed", true).apply();
        new AlertDialog.Builder(this)
                .setTitle("শুভেচ্ছা")
                .setMessage("Time Lock-এ স্বাগতম, " + USER + "!")
                .setPositiveButton("ঠিক আছে", null)
                .show();
    }

    void refresh() {
        dateBtn.setText("তারিখ: " + new SimpleDateFormat("dd MMM yyyy",
                new Locale("bn", "BD")).format(when.getTime()));
        timeBtn.setText("সময়: " + new SimpleDateFormat("hh:mm a",
                new Locale("bn", "BD")).format(when.getTime()));
        listBox.removeAllViews();
        for (String[] it : load()) {
            final int id = Integer.parseInt(it[0]);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, 16, 0, 16);
            TextView tv = new TextView(this);
            tv.setText(fmt.format(new Date(Long.parseLong(it[1]))) + "\n" + it[2]);
            tv.setTextSize(16);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            Button del = new Button(this);
            del.setText("মুছুন");
            del.setOnClickListener(v -> { cancelAlarm(id); remove(id); refresh(); });
            row.addView(tv);
            row.addView(del);
            listBox.addView(row);
        }
    }

    void saveNote() {
        String text = input.getText().toString().trim()
                .replace("|", " ").replace("\n", " ");
        if (text.isEmpty()) { toast("কাজের কথা লিখুন"); return; }
        long ms = when.getTimeInMillis();
        if (ms <= System.currentTimeMillis()) { toast("ভবিষ্যতের সময় দিন"); return; }
        int id = (int) (System.currentTimeMillis() % 1000000000L);

        Intent i = new Intent(this, AlarmReceiver.class);
        i.putExtra("text", text);
        i.putExtra("id", id);
        PendingIntent pi = PendingIntent.getBroadcast(this, id, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent show = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.setAlarmClock(new AlarmManager.AlarmClockInfo(ms, show), pi);

        SharedPreferences sp = getSharedPreferences("notes", MODE_PRIVATE);
        String old = sp.getString("list", "");
        sp.edit().putString("list", old + id + "|" + ms + "|" + text + "\n").apply();
        input.setText("");
        toast("সেভ হয়েছে");
        refresh();
    }

    void cancelAlarm(int id) {
        PendingIntent pi = PendingIntent.getBroadcast(this, id,
                new Intent(this, AlarmReceiver.class), PendingIntent.FLAG_IMMUTABLE);
        ((AlarmManager) getSystemService(Context.ALARM_SERVICE)).cancel(pi);
    }

    List<String[]> load() {
        List<String[]> out = new ArrayList<>();
        String s = getSharedPreferences("notes", MODE_PRIVATE).getString("list", "");
        for (String line : s.split("\n")) {
            String[] p = line.split("\\|", 3);
            if (p.length == 3) out.add(p);
        }
        Collections.sort(out, (a, c) -> Long.compare(Long.parseLong(a[1]), Long.parseLong(c[1])));
        return out;
    }

    void remove(int id) {
        StringBuilder sb = new StringBuilder();
        for (String[] p : load()) {
            if (Integer.parseInt(p[0]) != id) {
                sb.append(p[0]).append("|").append(p[1]).append("|").append(p[2]).append("\n");
            }
        }
        getSharedPreferences("notes", MODE_PRIVATE).edit()
                .putString("list", sb.toString()).apply();
    }

    void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
    }
