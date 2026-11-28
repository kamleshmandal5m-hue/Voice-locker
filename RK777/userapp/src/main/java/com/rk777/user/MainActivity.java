package com.rk777.user;

import android.app.*;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.content.Intent;
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
    private CheckBox consent;
    private EditText senderInput, messageInput;
    private LinearLayout page;

    public void onCreate(Bundle b){super.onCreate(b);showForm(); handleSharedText(getIntent());}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleSharedText(intent);}
    private void handleSharedText(Intent intent){if(intent!=null&&Intent.ACTION_SEND.equals(intent.getAction())&&"text/plain".equals(intent.getType())){CharSequence shared=intent.getCharSequenceExtra(Intent.EXTRA_TEXT);if(messageInput!=null&&shared!=null)messageInput.setText(shared.toString());}}
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
        page.addView(text("Share a message with Admin",16));
        senderInput=field("SMS sender / phone number",android.text.InputType.TYPE_CLASS_PHONE);page.addView(senderInput);
        messageInput=new EditText(this);messageInput.setHint("Paste or share the SMS text here");messageInput.setTextColor(Color.WHITE);messageInput.setHintTextColor(0xffb5bdca);messageInput.setMinLines(3);messageInput.setGravity(Gravity.TOP);messageInput.setPadding(18,12,18,12);messageInput.setBackgroundTintList(android.content.res.ColorStateList.valueOf(gold));page.addView(messageInput);
        Button sendMessage=new Button(this);sendMessage.setText("SEND MESSAGE TO ADMIN");sendMessage.setTextColor(navy);sendMessage.setBackgroundTintList(android.content.res.ColorStateList.valueOf(gold));page.addView(sendMessage);
        sendMessage.setOnClickListener(v->sendMessage());
        Button submit=new Button(this);submit.setText("SUBMIT");submit.setTextColor(navy);submit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(gold));page.addView(submit);
        submit.setOnClickListener(v->submit());
    }

    void sendMessage(){
        String sender=senderInput.getText().toString().trim();
        String message=messageInput.getText().toString().trim();
        String submissionId=getSharedPreferences("rk777",MODE_PRIVATE).getString("submission_id","");
        if(submissionId.isEmpty()){Toast.makeText(this,"Pehle user details SUBMIT karein.",Toast.LENGTH_LONG).show();return;}
        if(sender.isEmpty()||message.isEmpty()){Toast.makeText(this,"SMS sender aur message dono bharein.",Toast.LENGTH_LONG).show();return;}
        new Thread(()->{
            HttpURLConnection c=null;int status=0;String error=null;
            try{
                URL url=new URL(SUPABASE_URL+"/rest/v1/sms_messages");
                c=(HttpURLConnection)url.openConnection();c.setRequestMethod("POST");c.setConnectTimeout(15000);c.setReadTimeout(15000);c.setDoOutput(true);
                c.setRequestProperty("apikey",SUPABASE_KEY);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Prefer","return=minimal");
                JSONObject body=new JSONObject();body.put("submission_id",submissionId);body.put("sender",sender);body.put("message_body",message);body.put("received_at",new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX",java.util.Locale.US).format(new java.util.Date()));
                try(OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes("UTF-8"));}
                status=c.getResponseCode();if(status<200||status>=300){InputStream es=c.getErrorStream();if(es!=null){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[1024];int r;while((r=es.read(buf))!=-1)out.write(buf,0,r);error=out.toString("UTF-8");}}
            }catch(Exception e){error=e.getMessage();}finally{if(c!=null)c.disconnect();}
            final int code=status;final String err=error;
            runOnUiThread(()->{if(code>=200&&code<300){messageInput.setText("");Toast.makeText(this,"Message Admin me bhej diya gaya.",Toast.LENGTH_LONG).show();}else Toast.makeText(this,"Message send nahi hua: "+(err==null?"Error":err),Toast.LENGTH_LONG).show();});
        }).start();
    }
    void submit(){
        String n=name.getText().toString().trim(),m=mobile.getText().toString().trim();
        if(n.isEmpty()||!m.matches("[0-9]{10}")||!consent.isChecked()){Toast.makeText(this,"Name, 10-digit mobile aur consent zaroori hai",Toast.LENGTH_LONG).show();return;}
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
                    prefs.edit().putString("submission_id",submissionId).apply();
                }
            }catch(Exception e){error=e.getMessage();}finally{if(c!=null)c.disconnect();}
            final int code=status;final String err=error;
            runOnUiThread(()->{if(code>=200&&code<300){new AlertDialog.Builder(this).setTitle("Submitted").setMessage("Your survey response has been submitted successfully. You can paste or share an SMS above to send it to Admin.").setPositiveButton("OK",(d,w)->showForm()).show();}else Toast.makeText(this,"Submit nahi hua. Network/Database settings check karein. "+(err==null?"":err),Toast.LENGTH_LONG).show();});
        }).start();
    }
}