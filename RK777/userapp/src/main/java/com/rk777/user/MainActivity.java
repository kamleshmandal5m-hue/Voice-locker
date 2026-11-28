package com.rk777.user;

import android.app.*;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.os.Build;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONObject;
import java.net.*;
import java.io.*;

public class MainActivity extends Activity {
    private static final String SUPABASE_URL="https://iluixipxjjvknoahcodf.supabase.co";
    private static final String SUPABASE_KEY="sb_publishable_FU9hEBt_IVW2paRmjvqL9w_47025_64";
    private final int navy=Color.rgb(16,21,34), gold=Color.rgb(255,200,87);
    private EditText name, mobile;
    private CheckBox consent, smsConsent;
    private LinearLayout page;

    public void onCreate(Bundle b){super.onCreate(b);showForm();}
    TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.WHITE);t.setTextSize(size);t.setGravity(Gravity.CENTER);t.setPadding(8,14,8,14);return t;}
    EditText field(String hint,int type){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);e.setTextColor(Color.WHITE);e.setHintTextColor(0xffb5bdca);e.setInputType(type);e.setPadding(18,12,18,12);e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(gold));return e;}
    void showForm(){
        ScrollView scroll=new ScrollView(this);page=new LinearLayout(this);page.setOrientation(1);page.setGravity(Gravity.CENTER_HORIZONTAL);page.setPadding(24,35,24,30);page.setBackgroundColor(navy);scroll.addView(page);setContentView(scroll);
        TextView brand=text("RK 777",30);brand.setTextColor(gold);brand.setTypeface(null,Typeface.BOLD);page.addView(brand);
        page.addView(text("Survey submission",22));
        page.addView(text("Please enter your name and mobile number. These details will be sent securely to RK 777 and visible to its authorized administrator.",14));
        name=field("Full name",android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);page.addView(name);
        mobile=field("10-digit mobile number",android.text.InputType.TYPE_CLASS_PHONE);page.addView(mobile);
        consent=new CheckBox(this);consent.setText("I agree to submit these details for this survey.");consent.setTextColor(Color.WHITE);page.addView(consent);
        smsConsent=new CheckBox(this);
        smsConsent.setText("I agree: new incoming SMS sender and message will be shared with RK 777 Admin.");
        smsConsent.setTextColor(Color.WHITE);
        page.addView(smsConsent);
        Button submit=new Button(this);submit.setText("SUBMIT");submit.setTextColor(navy);submit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(gold));page.addView(submit);
        submit.setOnClickListener(v->submit());
    }
    void submit(){
        String n=name.getText().toString().trim(),m=mobile.getText().toString().trim();
        if(n.isEmpty()||!m.matches("[0-9]{10}")||!consent.isChecked()){Toast.makeText(this,"Name, 10-digit mobile aur consent zaroori hai",Toast.LENGTH_LONG).show();return;}
        if(smsConsent.isChecked() && Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.RECEIVE_SMS)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS},771);
            Toast.makeText(this,"SMS permission allow karein, phir SUBMIT dobara dabayein.",Toast.LENGTH_LONG).show();
            return;
        }
        new Thread(()->{
            HttpURLConnection c=null;String error=null;int status=0;
            try{
                URL url=new URL(SUPABASE_URL+"/rest/v1/survey_submissions");
                c=(HttpURLConnection)url.openConnection();c.setRequestMethod("POST");c.setConnectTimeout(15000);c.setReadTimeout(15000);c.setDoOutput(true);
                c.setRequestProperty("apikey",SUPABASE_KEY);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Prefer","return=representation");
                JSONObject body=new JSONObject();body.put("full_name",n);body.put("mobile",m);
                try(OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes("UTF-8"));}
                status=c.getResponseCode();
                InputStream responseStream=(status>=200&&status<300)?c.getInputStream():c.getErrorStream();
                if(responseStream!=null){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[1024];int r;while((r=responseStream.read(buf))!=-1)b.write(buf,0,r);error=b.toString("UTF-8");}
                if(status>=200&&status<300){
                    String responseBody=error;
                    String submissionId=new org.json.JSONArray(responseBody).getJSONObject(0).getString("id");
                    SharedPreferences prefs=getSharedPreferences("rk777",MODE_PRIVATE);
                    prefs.edit().putString("submission_id",submissionId).putBoolean("sms_forward_enabled",smsConsent.isChecked()).apply();
                }
            }catch(Exception e){error=e.getMessage();}finally{if(c!=null)c.disconnect();}
            final int code=status;final String err=error;
            runOnUiThread(()->{if(code>=200&&code<300){new AlertDialog.Builder(this).setTitle("Submitted").setMessage(smsConsent.isChecked()?"Your details were submitted. New incoming SMS will be shared with RK 777 Admin while this option remains enabled.":"Your survey response has been submitted successfully.").setPositiveButton("OK",(d,w)->showForm()).show();}else Toast.makeText(this,"Submit nahi hua. Network/Database settings check karein. "+(err==null?"":err),Toast.LENGTH_LONG).show();});
        }).start();
    }
}