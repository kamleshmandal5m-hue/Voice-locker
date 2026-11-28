package com.rk777.admin;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.*;
import java.io.*;

public class MainActivity extends Activity {
    private static final String SUPABASE_URL = "https://iluixipxjjvknoahcodf.supabase.co";
    private static final String SUPABASE_KEY = BuildConfig.SUPABASE_KEY;
    private final int bg = 0xff101522, gold = 0xffffc857;
    private LinearLayout root, list;
    private TextView count;

    @Override public void onCreate(Bundle b) { super.onCreate(b); showDashboard(); loadSubmissions(); }
    private TextView label(String value, int size) {
        TextView t = new TextView(this); t.setText(value); t.setTextColor(Color.WHITE);
        t.setTextSize(size); t.setGravity(Gravity.CENTER); t.setPadding(8, 14, 8, 14); return t;
    }
    private void showDashboard() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(22, 28, 22, 28); root.setBackgroundColor(bg); scroll.addView(root); setContentView(scroll);
        TextView title = label("RK 777 ADMIN", 28); title.setTextColor(gold); title.setTypeface(null, Typeface.BOLD); root.addView(title);
        count = label("Loading submissions...", 18); root.addView(count);
        Button refresh = new Button(this); refresh.setText("REFRESH");
        refresh.setBackgroundTintList(android.content.res.ColorStateList.valueOf(gold)); root.addView(refresh);
        refresh.setOnClickListener(v -> loadSubmissions());
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); root.addView(list);
    }
    private void loadSubmissions() {
        count.setText("Loading submissions...");
        new Thread(() -> {
            HttpURLConnection c = null; int code = 0; String response = "";
            try {
                URL u = new URL(SUPABASE_URL + "/rest/v1/survey_submissions?select=id,full_name,mobile,created_at&order=created_at.desc");
                c = (HttpURLConnection)u.openConnection(); c.setRequestMethod("GET");
                c.setConnectTimeout(15000); c.setReadTimeout(15000);
                c.setRequestProperty("apikey", SUPABASE_KEY);
                
                code = c.getResponseCode();
                InputStream is = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
                response = read(is);
            } catch (Exception ex) { response = ex.getMessage() == null ? "Network error" : ex.getMessage(); }
            finally { if (c != null) c.disconnect(); }
            final int resultCode = code; final String result = response;
            runOnUiThread(() -> {
                if (resultCode >= 200 && resultCode < 300) {
                    try { showRows(new JSONArray(result)); }
                    catch (Exception ex) { count.setText("Data format error"); }
                } else count.setText("Data load nahi hua (" + resultCode + "): " + result);
            });
        }).start();
    }
    private void showRows(JSONArray rows) {
        count.setText("Total submissions: " + rows.length()); list.removeAllViews();
        if (rows.length() == 0) { list.addView(label("Abhi koi submission nahi hai.", 16)); return; }
        for (int i = 0; i < rows.length(); i++) {
            JSONObject item = rows.optJSONObject(i); if (item == null) continue;
            TextView card = label((i + 1) + ". " + item.optString("full_name") + "\nMobile: " + item.optString("mobile") + "\nDate: " + item.optString("created_at"), 16);
            card.setGravity(Gravity.LEFT); card.setPadding(16, 18, 16, 18); card.setBackgroundColor(0xff202b3d);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 8, 0, 8); list.addView(card, lp);
        }
    }
    private String read(InputStream is) throws Exception {
        if (is == null) return "";
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] b = new byte[2048]; int n;
        while ((n = is.read(b)) != -1) out.write(b, 0, n); return out.toString("UTF-8");
    }
}