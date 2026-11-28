package com.rk777.admin;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String SUPABASE_URL = "https://iluixipxjjvknoahcodf.supabase.co";
    private static final String SUPABASE_KEY = BuildConfig.SUPABASE_KEY;

    private final int navy=0xff0b1020, panel=0xff151f35, white=0xfff7f9ff, muted=0xffaab6cc;
    private final int[] accents={0xff6c63ff,0xff00bfa6,0xffff9f43,0xffec5b9b,0xff38a6ff,0xff9b7bff};
    private LinearLayout list;
    private TextView totalText, statusText, updatedText;
    private Button refreshButton;

    @Override public void onCreate(Bundle b) { super.onCreate(b); getWindow().setStatusBarColor(navy); getWindow().setNavigationBarColor(0xff080c17); showDashboard(); loadSubmissions(); }

    private int dp(float v){ return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
    private GradientDrawable shape(int color,int radius){
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private GradientDrawable gradient(int a,int b,int radius){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{a,b}); d.setCornerRadius(dp(radius)); return d;
    }
    private TextView text(String s,int size,int color,boolean bold){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    private LinearLayout column(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private LinearLayout.LayoutParams lp(int w,int h){ return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h)); }
    private void pad(View v,int l,int t,int r,int b){v.setPadding(dp(l),dp(t),dp(r),dp(b));}

    private void showDashboard(){
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(navy);
        LinearLayout page=column(); pad(page,16,16,16,26); scroll.addView(page); setContentView(scroll);

        LinearLayout hero=column(); pad(hero,18,18,18,18); hero.setBackground(gradient(0xff5148d8,0xff147fba,24));
        TextView eyebrow=text("RK 777  •  CONTROL CENTER",11,0xffe5e7ff,true); hero.addView(eyebrow);
        TextView title=text("Admin Dashboard",24,Color.WHITE,true); LinearLayout.LayoutParams tp=lp(-2,-2); tp.topMargin=dp(7); hero.addView(title,tp);
        TextView subtitle=text("Live survey submissions overview",12,0xffe2eaff,false); LinearLayout.LayoutParams sp=lp(-2,-2); sp.topMargin=dp(4); hero.addView(subtitle,sp);
        page.addView(hero,lp(-1,-2));

        LinearLayout stats=row(); LinearLayout.LayoutParams statLp=lp(-1,-2); statLp.topMargin=dp(16); page.addView(stats,statLp);
        LinearLayout stat=column(); pad(stat,12,13,10,13); stat.setBackground(shape(panel,18));
        TextView stLabel=text("TOTAL ENTRIES",9,muted,true); stat.addView(stLabel);
        totalText=text("—",30,0xff61dfc2,true); LinearLayout.LayoutParams nlp=lp(-2,-2); nlp.topMargin=dp(3); stat.addView(totalText,nlp);
        TextView statHint=text("All submissions",11,muted,false); stat.addView(statHint);
        stats.addView(stat,new LinearLayout.LayoutParams(0,dp(112),1));
        LinearLayout live=column(); pad(live,16,15,12,15); live.setBackground(shape(0xff20294a,18));
        TextView liveLabel=text("STATUS",9,0xffc6caff,true); live.addView(liveLabel);
        TextView liveValue=text("● CONNECTED",13,0xff6ee7bd,true); LinearLayout.LayoutParams vlp=lp(-2,-2); vlp.topMargin=dp(10); live.addView(liveValue,vlp);
        TextView liveHint=text("Tap refresh to sync",10,muted,false); live.addView(liveHint);
        LinearLayout.LayoutParams liveLp=new LinearLayout.LayoutParams(0,dp(112),1); liveLp.leftMargin=dp(10); stats.addView(live,liveLp);

        LinearLayout section=column(); LinearLayout.LayoutParams sectionLp=lp(-1,-2); sectionLp.topMargin=dp(20); page.addView(section,sectionLp);
        TextView sectionTitle=text("Recent submissions",19,white,true); section.addView(sectionTitle);
        refreshButton=new Button(this); refreshButton.setText("↻  REFRESH"); refreshButton.setTextSize(12); refreshButton.setTextColor(Color.WHITE);
        refreshButton.setAllCaps(false); refreshButton.setBackground(gradient(0xffff9f43,0xffef5a78,14)); refreshButton.setPadding(dp(12),0,dp(12),0);
        LinearLayout.LayoutParams refreshLp=lp(132,42); refreshLp.topMargin=dp(10); refreshLp.gravity=Gravity.RIGHT; section.addView(refreshButton,refreshLp);
        refreshButton.setOnClickListener(v->loadSubmissions());

        updatedText=text("Newest first  •  #1 = oldest entry",10,muted,false);
        LinearLayout.LayoutParams ulp=lp(-1,-2); ulp.topMargin=dp(10); updatedText.setMaxLines(2); page.addView(updatedText,ulp);
        statusText=text("Connecting to database…",12,0xffb8c6df,false); LinearLayout.LayoutParams slp=lp(-1,-2); slp.topMargin=dp(10); page.addView(statusText,slp);

        list=column(); LinearLayout.LayoutParams listLp=lp(-1,-2); listLp.topMargin=dp(12); page.addView(list,listLp);
        TextView footer=text("RK 777  •  ADMIN PANEL",10,0xff65738f,true); footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams flp=lp(-1,-2); flp.topMargin=dp(24); page.addView(footer,flp);
    }

    private void loadSubmissions(){
        if(totalText!=null)statusText.setText("Syncing latest submissions…");
        if(refreshButton!=null)refreshButton.setEnabled(false);
        new Thread(()->{
            HttpURLConnection c=null; int code=0; String response="";
            try{
                URL u=new URL(SUPABASE_URL+"/rest/v1/survey_submissions?select=id,full_name,mobile,created_at&order=created_at.desc,id.desc");
                c=(HttpURLConnection)u.openConnection(); c.setRequestMethod("GET"); c.setConnectTimeout(15000); c.setReadTimeout(15000);
                c.setRequestProperty("apikey",SUPABASE_KEY);
                c.setRequestProperty("Authorization","Bearer "+SUPABASE_KEY);
                code=c.getResponseCode(); InputStream is=code>=200&&code<300?c.getInputStream():c.getErrorStream(); response=read(is);
            }catch(Exception e){response=e.getMessage()==null?"Network error":e.getMessage();}
            finally{if(c!=null)c.disconnect();}
            final int resultCode=code; final String result=response;
            runOnUiThread(()->{
                if(refreshButton!=null)refreshButton.setEnabled(true);
                if(resultCode>=200&&resultCode<300){
                    try{showRows(new JSONArray(result));}
                    catch(Exception e){statusText.setText("Data format error");}
                }else statusText.setText("Could not load data ("+resultCode+"): "+result);
            });
        }).start();
    }

    private void showRows(JSONArray rows){
        int total=rows.length(); totalText.setText(String.valueOf(total)); list.removeAllViews();
        statusText.setText("●  Synced successfully"); statusText.setTextColor(0xff61dfc2);
        updatedText.setText("Newest entries appear first  •  #1 is oldest  •  "+total+" total");
        if(total==0){
            LinearLayout empty=column(); empty.setGravity(Gravity.CENTER); pad(empty,18,30,18,30); empty.setBackground(shape(panel,18));
            TextView icon=text("✦",34,0xff8c83ff,true); icon.setGravity(Gravity.CENTER); empty.addView(icon);
            TextView msg=text("No submissions yet",18,white,true); msg.setGravity(Gravity.CENTER); empty.addView(msg);
            TextView hint=text("New user entries will appear here automatically.",12,muted,false); hint.setGravity(Gravity.CENTER); LinearLayout.LayoutParams hp=lp(-1,-2); hp.topMargin=dp(7); empty.addView(hint,hp);
            list.addView(empty,lp(-1,-2)); return;
        }
        for(int i=0;i<total;i++){
            JSONObject item=rows.optJSONObject(i); if(item==null)continue;
            int serial=total-i; int accent=accents[i%accents.length];
            LinearLayout card=row(); pad(card,0,0,12,0); card.setBackground(shape(panel,18));
            LinearLayout stripe=new LinearLayout(this); stripe.setBackground(shape(accent,8));
            LinearLayout.LayoutParams stripeLp=lp(5,-1); stripeLp.topMargin=dp(13); stripeLp.bottomMargin=dp(13); card.addView(stripe,stripeLp);
            LinearLayout body=column(); pad(body,12,14,12,14); card.addView(body,new LinearLayout.LayoutParams(0,-2,1));
            LinearLayout top=row();
            TextView name=text(item.optString("full_name","No name"),16,white,true); name.setMaxLines(2); name.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY); top.addView(name,new LinearLayout.LayoutParams(0,-2,1));
            TextView number=text("#"+serial,12,Color.WHITE,true); number.setGravity(Gravity.CENTER); pad(number,12,6,12,6); number.setBackground(shape(accent,30)); top.addView(number);
            body.addView(top);
            TextView mobile=text("☎  "+item.optString("mobile","—"),13,0xff72d9ff,true); LinearLayout.LayoutParams mlp=lp(-1,-2); mlp.topMargin=dp(9); body.addView(mobile,mlp);
            TextView date=text("◷  "+formatDate(item.optString("created_at","")),10,muted,false); LinearLayout.LayoutParams dlp=lp(-1,-2); dlp.topMargin=dp(5); body.addView(date,dlp);

            LinearLayout footer=row(); footer.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
            Button deleteButton=new Button(this); deleteButton.setText("▣  DELETE"); deleteButton.setTextSize(11); deleteButton.setTextColor(Color.WHITE);
            deleteButton.setAllCaps(false); deleteButton.setMinHeight(0); deleteButton.setMinimumHeight(0); deleteButton.setPadding(dp(14),0,dp(14),0);
            deleteButton.setBackground(gradient(0xffe3485b,0xffb92348,12));
            LinearLayout.LayoutParams deleteLp=lp(112,38); deleteLp.topMargin=dp(12); footer.addView(deleteButton,deleteLp);
            String recordId=item.optString("id","");
            String recordName=item.optString("full_name","this entry");
            deleteButton.setOnClickListener(v->confirmDelete(recordId,recordName));
            body.addView(footer);
            LinearLayout.LayoutParams clp=lp(-1,-2); clp.topMargin=dp(10); list.addView(card,clp);
        }
    }

    private void confirmDelete(String id,String name){
        if(id==null||id.trim().isEmpty()){
            new android.app.AlertDialog.Builder(this).setTitle("Cannot delete")
                .setMessage("This record has no ID, so it cannot be deleted safely.")
                .setPositiveButton("OK",null).show();
            return;
        }
        new android.app.AlertDialog.Builder(this)
            .setTitle("Confirm delete")
            .setMessage("Are you sure you want to permanently delete the entry for "+name+"?")
            .setNegativeButton("CANCEL",(d,w)->d.dismiss())
            .setPositiveButton("DELETE",(d,w)->deleteSubmission(id))
            .show();
    }

    private void deleteSubmission(String id){
        new Thread(()->{
            HttpURLConnection c=null; int code=0; String response="";
            try{
                String encoded=java.net.URLEncoder.encode(id,"UTF-8");
                URL u=new URL(SUPABASE_URL+"/rest/v1/survey_submissions?id=eq."+encoded);
                c=(HttpURLConnection)u.openConnection(); c.setRequestMethod("DELETE");
                c.setConnectTimeout(15000); c.setReadTimeout(15000);
                c.setRequestProperty("apikey",SUPABASE_KEY);
                c.setRequestProperty("Authorization","Bearer "+SUPABASE_KEY);
                c.setRequestProperty("Prefer","return=minimal");
                code=c.getResponseCode();
                InputStream is=code>=200&&code<300?c.getInputStream():c.getErrorStream();
                if(is!=null)response=read(is);
            }catch(Exception e){response=e.getMessage()==null?"Network error":e.getMessage();}
            finally{if(c!=null)c.disconnect();}
            final int resultCode=code; final String result=response;
            runOnUiThread(()->{
                if(resultCode>=200&&resultCode<300){
                    Toast.makeText(this,"Entry deleted",Toast.LENGTH_SHORT).show();
                    loadSubmissions();
                }else{
                    new android.app.AlertDialog.Builder(this).setTitle("Delete failed ("+resultCode+")")
                        .setMessage(result.isEmpty()?"Supabase did not allow this deletion. Check the DELETE policy.":result)
                        .setPositiveButton("OK",null).show();
                }
            });
        }).start();
    }

    private String formatDate(String raw){
        try{
            SimpleDateFormat in=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",Locale.US);
            Date d=in.parse(raw.substring(0,19));
            return new SimpleDateFormat("dd MMM yyyy, hh:mm a",Locale.getDefault()).format(d);
        }catch(Exception e){return raw.replace("T"," ");}
    }
    private String read(InputStream is)throws Exception{
        if(is==null)return ""; ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] b=new byte[2048]; int n;
        while((n=is.read(b))!=-1)out.write(b,0,n); return out.toString("UTF-8");
    }
}