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
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImlsdWl4aXB4amp2a25vYWhjb2RmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTU4MjU4NzcsImV4cCI6MjExMTQwMTg3N30.U1dJaIDy0E_qcDaQiGJrSARk5im3ZQYGvFR4ORAXl30";
    private final int bg = 0xff101522, gold = 0xffffc857;
    private EditText email, password;
    private LinearLayout root, list;
    private String accessToken;

    @Override public void onCreate(Bundle b) { super.onCreate(b); showLogin(); }

    private TextView label(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextColor(Color.WHITE); t.setTextSize(size);
        t.setGravity(Gravity.CENTER); t.setPadding(8, 14, 8, 14);
        return t;
    }
    private EditText field(String hint, int type) {
        EditText e = new EditText(this); e.setSingleLine(true); e.setHint(hint);
        e.setTextColor(Color.WHITE); e.setHintTextColor(0xffb5bdca);
        e.setInputType(type); e.setPadding(18, 12, 18, 12);
        e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(gold));
        return e;
    }
    private void base() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(22, 28, 22, 28); root.setBackgroundColor(bg);
        scroll.addView(root); setContentView(scroll);
    }
    private void showLogin() {
        base();
        TextView title = label("RK 777 ADMIN", 28);
        title.setTextColor(gold); title.setTypeface(null, Typeface.BOLD); root.addView(title);
        root.addView(label("Secure administrator login", 16));
        email = field("Admin email", android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        password = field("Password", android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        root.addView(email); root.addView(password);
        Button login = new Button(this); login.setText("LOGIN");
        login.setBackgroundTintList(android.content.res.ColorStateList.valueOf(gold)); root.addView(login);
        login.setOnClickListener(v -> login());
    }
    private void login() {
        String e = email.getText().toString().trim(), p = password.getText().toString();
        if (e.isEmpty() || p.isEmpty()) { toast("Email aur password bharein"); return; }
        toast("Logging in...");
        new Thread(() -> {
            HttpURLConnection c = null; int code = 0; String response = "";
            try {
                URL u = new URL(SUPABASE_URL + "/auth/v1/token?grant_type=password");
                c = (HttpURLConnection)u.openConnection(); c.setRequestMethod("POST");
                c.setConnectTimeout(15000); c.setReadTimeout(15000); c.setDoOutput(true);
                c.setRequestProperty("apikey", SUPABASE_KEY); c.setRequestProperty("Content-Type", "application/json");
                JSONObject body = new JSONObject(); body.put("email", e); body.put("password", p);
                try (OutputStream os = c.getOutputStream()) { os.write(body.toString().getBytes("UTF-8")); }
                code = c.getResponseCode(); InputStream is = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
                response = read(is);
            } catch (Exception ex) { response = ex.getMessage(); }
            finally { if (c != null) c.disconnect(); }
            final int resultCode = code; final String result = response;
            runOnUiThread(() -> {
                if (resultCode >= 200 && resultCode < 300) {
                    try { accessToken = new JSONObject(result).getString("access_token"); loadSubmissions(); }
                    catch (Exception ex) { toast("Login response samajh nahi aaya"); }
                } else toast("Login fail: " + result);
            });
        }).start();
    }
    private void showDashboard(JSONArray rows) {
        base();
        TextView title = label("RK 777 ADMIN", 26); title.setTextColor(gold); title.setTypeface(null, Typeface.BOLD); root.addView(title);
        root.addView(label("Total submissions: " + rows.length(), 18));
        Button refresh = new Button(this); refresh.setText("REFRESH"); root.addView(refresh); refresh.setOnClickListener(v -> loadSubmissions());
        Button logout = new Button(this); logout.setText("LOG OUT"); root.addView(logout); logout.setOnClickListener(v -> { accessToken = null; showLogin(); });
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); root.addView(list);
        if (rows.length() == 0) list.addView(label("Abhi koi submission nahi hai.", 16));
        for (int i = 0; i < rows.length(); i++) {
            JSONObject item = rows.optJSONObject(i); if (item == null) continue;
            TextView card = label((i + 1) + ". " + item.optString("full_name") + "\nMobile: " + item.optString("mobile") + "\nDate: " + item.optString("created_at"), 16);
            card.setGravity(Gravity.LEFT); card.setPadding(16, 18, 16, 18);
            card.setBackgroundColor(0xff202b3d); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 8, 0, 8); list.addView(card, lp);
        }
    }
    private void loadSubmissions() {
        if (accessToken == null) return;
        runOnUiThread(() -> toast("Loading submissions..."));
        new Thread(() -> {
            HttpURLConnection c = null; int code = 0; String response = "";
            try {
                URL u = new URL(SUPABASE_URL + "/rest/v1/survey_submissions?select=id,full_name,mobile,created_at&order=created_at.desc");
                c = (HttpURLConnection)u.openConnection(); c.setRequestMethod("GET");
                c.setConnectTimeout(15000); c.setReadTimeout(15000);
                c.setRequestProperty("apikey", SUPABASE_KEY); c.setRequestProperty("Authorization", "Bearer " + accessToken);
                code = c.getResponseCode(); InputStream is = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
                response = read(is);
            } catch (Exception ex) { response = ex.getMessage(); }
            finally { if (c != null) c.disconnect(); }
            final int resultCode = code; final String result = response;
            runOnUiThread(() -> {
                if (resultCode >= 200 && resultCode < 300) {
                    try { showDashboard(new JSONArray(result)); }
                    catch (Exception ex) { toast("Data parse nahi hua"); }
                } else toast("Data nahi mila: " + result);
            });
        }).start();
    }
    private String read(InputStream is) throws Exception {
        if (is == null) return "";
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] b = new byte[2048]; int n;
        while ((n = is.read(b)) != -1) out.write(b, 0, n);
        return out.toString("UTF-8");
    }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}
