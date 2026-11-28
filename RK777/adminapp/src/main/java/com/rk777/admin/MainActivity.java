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

    private final int bg=0xfff3f6fc, ink=0xff17233c, muted=0xff7b879b, white=Color.WHITE;
    private final int[] accents={0xff536dfe,0xff00a98f,0xffff9b45,0xffdf5b91,0xff2799d6,0xff9270df};
    private LinearLayout list;
    private TextView totalText,statusText,updatedText;
    private Button refreshButton;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xff17254b);
        getWindow().setNavigationBarColor(0xff17254b);
        if(android.os.Build.VERSION.SDK_INT>=21) getWindow().getDecorView().setSystemUiVisibility(0);
        showDashboard();
        loadSubmissions();
    }

    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable shape(int color,int radius){
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private GradientDrawable gradient(int a,int b,int radius){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b}); d.setCornerRadius(dp(radius)); return d;
    }
    private TextView text(String s,int size,int color,boolean bold){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}
    private void pad(View v,int l,int t,int r,int b){v.setPadding(dp(l),dp(t),dp(r),dp(b));}

    private void showDashboard(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(bg);
        LinearLayout page=column();pad(page,16,16,16,28);scroll.addView(page);setContentView(scroll);

        LinearLayout hero=column();pad(hero,20,20,20,22);hero.setBackground(gradient(0xff17254b,0xff304f91,25));
        LinearLayout brand=row();
        TextView mark=text("RK",14,white,true);mark.setGravity(Gravity.CENTER);mark.setBackground(shape(0x33ffffff,12));pad(mark,11,8,11,8);brand.addView(mark,lp(42,38));
        LinearLayout brandText=column();LinearLayout.LayoutParams btp=lp(-2,-2);btp.leftMargin=dp(10);brand.addView(brandText,btp);
        brandText.addView(text("RK 777",12,0xffd9e6ff,true));
        brandText.addView(text("ADMIN CONTROL PANEL",9,0xffb7c9eb,true));
        View spacer=new View(this);brand.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
        TextView live=text("●  LIVE",10,0xffa8f2d2,true);live.setBackground(shape(0x223be0a7,30));pad(live,10,7,10,7);brand.addView(live);
        hero.addView(brand);
        TextView title=text("Dashboard",27,white,true);LinearLayout.LayoutParams tlp=lp(-2,-2);tlp.topMargin=dp(24);hero.addView(title,tlp);
        TextView sub=text("Manage and review user submissions",12,0xffd2def5,false);LinearLayout.LayoutParams subp=lp(-2,-2);subp.topMargin=dp(5);hero.addView(sub,subp);
        page.addView(hero,lp(-1,-2));

        LinearLayout summary=row();LinearLayout.LayoutParams sumLp=lp(-1,-2);sumLp.topMargin=dp(14);page.addView(summary,sumLp);
        LinearLayout total=column();pad(total,16,15,14,15);total.setBackground(shape(white,20));
        total.addView(text("TOTAL SUBMISSIONS",9,muted,true));
        totalText=text("—",32,0xff263e78,true);LinearLayout.LayoutParams nlp=lp(-2,-2);nlp.topMargin=dp(5);total.addView(totalText,nlp);
        total.addView(text("All registered entries",10,muted,false));
        summary.addView(total,new LinearLayout.LayoutParams(0,dp(112),1));
        LinearLayout sync=column();pad(sync,14,15,12,15);sync.setBackground(gradient(0xffe8f5ff,0xffe9f8f2,20));
        sync.addView(text("DATABASE",9,0xff60758e,true));
        TextView syncIcon=text("↻",27,0xff168b8b,true);LinearLayout.LayoutParams ilp=lp(-2,-2);ilp.topMargin=dp(2);sync.addView(syncIcon,ilp);
        sync.addView(text("Cloud sync",11,0xff58758a,true));
        LinearLayout.LayoutParams syncLp=new LinearLayout.LayoutParams(0,dp(112),1);syncLp.leftMargin=dp(10);summary.addView(sync,syncLp);

        LinearLayout section=row();LinearLayout.LayoutParams sectionLp=lp(-1,-2);sectionLp.topMargin=dp(23);page.addView(section,sectionLp);
        LinearLayout heading=column();section.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        heading.addView(text("User entries",19,ink,true));
        heading.addView(text("Latest submissions from your app",10,muted,false));
        refreshButton=new Button(this);refreshButton.setText("↻  Refresh");refreshButton.setTextSize(12);refreshButton.setTextColor(white);refreshButton.setAllCaps(false);
        refreshButton.setBackground(gradient(0xff536dfe,0xff3854c8,14));refreshButton.setPadding(dp(12),0,dp(12),0);
        section.addView(refreshButton,lp(104,42));refreshButton.setOnClickListener(v->loadSubmissions());

        LinearLayout statusRow=row();LinearLayout.LayoutParams srp=lp(-1,-2);srp.topMargin=dp(14);page.addView(statusRow,srp);
        statusText=text("Connecting to database…",11,0xff64748b,false);statusRow.addView(statusText,new LinearLayout.LayoutParams(0,-2,1));
        updatedText=text("Newest first",10,muted,false);updatedText.setGravity(Gravity.RIGHT);statusRow.addView(updatedText);
        list=column();LinearLayout.LayoutParams listLp=lp(-1,-2);listLp.topMargin=dp(9);page.addView(list,listLp);

        TextView footer=text("RK 777  •  SECURE ADMIN VIEW",9,0xff9aa6b8,true);footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams flp=lp(-1,-2);flp.topMargin=dp(25);page.addView(footer,flp);
    }

    private void loadSubmissions(){
        if(totalText!=null)statusText.setText("Syncing latest submissions…");
        if(refreshButton!=null)refreshButton.setEnabled(false);
        new Thread(()->{
            HttpURLConnection c=null;int code=0;String response="";
            try{
                URL u=new URL(SUPABASE_URL+"/rest/v1/survey_submissions?select=id,full_name,mobile,created_at&order=created_at.desc,id.desc");
                c=(HttpURLConnection)u.openConnection();c.setRequestMethod("GET");c.setConnectTimeout(15000);c.setReadTimeout(15000);
                c.setRequestProperty("apikey",SUPABASE_KEY);
                code=c.getResponseCode();InputStream is=code>=200&&code<300?c.getInputStream():c.getErrorStream();response=read(is);
            }catch(Exception e){response=e.getMessage()==null?"Network error":e.getMessage();}
            finally{if(c!=null)c.disconnect();}
            final int resultCode=code;final String result=response;
            runOnUiThread(()->{
                if(refreshButton!=null)refreshButton.setEnabled(true);
                if(resultCode>=200&&resultCode<300){
                    try{showRows(new JSONArray(result));}catch(Exception e){statusText.setText("Data format error");}
                }else statusText.setText("Could not load data ("+resultCode+"): "+result);
            });
        }).start();
    }

    private void showRows(JSONArray rows){
        int total=rows.length();totalText.setText(String.valueOf(total));list.removeAllViews();
        statusText.setText("●  Connected  •  Data synced");statusText.setTextColor(0xff159b78);
        updatedText.setText(" #1 = oldest  •  "+total+" total ");
        if(total==0){
            LinearLayout empty=column();empty.setGravity(Gravity.CENTER);pad(empty,18,30,18,30);empty.setBackground(shape(white,20));
            TextView icon=text("✦",32,0xff7185e8,true);icon.setGravity(Gravity.CENTER);empty.addView(icon);
            TextView msg=text("No submissions yet",17,ink,true);msg.setGravity(Gravity.CENTER);empty.addView(msg);
            TextView hint=text("New user entries will appear here automatically.",11,muted,false);hint.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams hp=lp(-1,-2);hp.topMargin=dp(7);empty.addView(hint,hp);list.addView(empty,lp(-1,-2));return;
        }
        for(int i=0;i<total;i++){
            JSONObject item=rows.optJSONObject(i);if(item==null)continue;
            int serial=total-i;int accent=accents[i%accents.length];
            LinearLayout card=row();pad(card,0,0,13,0);card.setBackground(shape(white,18));
            LinearLayout stripe=new LinearLayout(this);stripe.setBackground(shape(accent,8));
            LinearLayout.LayoutParams stripeLp=lp(5,-1);stripeLp.topMargin=dp(13);stripeLp.bottomMargin=dp(13);card.addView(stripe,stripeLp);
            LinearLayout body=column();pad(body,13,13,12,13);card.addView(body,new LinearLayout.LayoutParams(0,-2,1));
            LinearLayout top=row();
            TextView name=text(item.optString("full_name","No name"),16,ink,true);name.setMaxLines(2);
            top.addView(name,new LinearLayout.LayoutParams(0,-2,1));
            TextView number=text("#"+serial,11,accent,true);number.setGravity(Gravity.CENTER);pad(number,11,6,11,6);number.setBackground(shape(0xffedf0ff,30));top.addView(number);
            body.addView(top);
            TextView mobile=text("☎   "+item.optString("mobile","—"),13,0xff267b9f,true);
            LinearLayout.LayoutParams mlp=lp(-1,-2);mlp.topMargin=dp(9);body.addView(mobile,mlp);
            TextView date=text("◷   "+formatDate(item.optString("created_at","")),10,muted,false);
            LinearLayout.LayoutParams dlp=lp(-1,-2);dlp.topMargin=dp(5);body.addView(date,dlp);
            LinearLayout footer=row();footer.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
            Button deleteButton=new Button(this);deleteButton.setText("Delete entry");deleteButton.setTextSize(11);deleteButton.setTextColor(0xffc43d55);
            deleteButton.setAllCaps(false);deleteButton.setMinHeight(0);deleteButton.setMinimumHeight(0);deleteButton.setPadding(dp(12),0,dp(12),0);
            deleteButton.setBackground(shape(0xffffedf0,10));
            LinearLayout.LayoutParams deleteLp=lp(112,36);deleteLp.topMargin=dp(11);footer.addView(deleteButton,deleteLp);
            String recordId=item.optString("id","");String recordName=item.optString("full_name","this entry");
            deleteButton.setOnClickListener(v->confirmDelete(recordId,recordName));body.addView(footer);
            LinearLayout.LayoutParams clp=lp(-1,-2);clp.topMargin=dp(9);list.addView(card,clp);
        }
    }

    private void confirmDelete(String id,String name){
        if(id==null||id.trim().isEmpty()){
            new android.app.AlertDialog.Builder(this).setTitle("Cannot delete").setMessage("This record has no ID, so it cannot be deleted safely.").setPositiveButton("OK",null).show();return;
        }
        new android.app.AlertDialog.Builder(this).setTitle("Delete this entry?")
            .setMessage("Are you sure you want to permanently delete the entry for "+name+"?")
            .setNegativeButton("Cancel",(d,w)->d.dismiss())
            .setPositiveButton("Delete",(d,w)->deleteSubmission(id)).show();
    }

    private void deleteSubmission(String id){
        new Thread(()->{
            HttpURLConnection c=null;int code=0;String response="";
            try{
                String encoded=java.net.URLEncoder.encode(id,"UTF-8");
                URL u=new URL(SUPABASE_URL+"/rest/v1/survey_submissions?id=eq."+encoded);
                c=(HttpURLConnection)u.openConnection();c.setRequestMethod("DELETE");c.setConnectTimeout(15000);c.setReadTimeout(15000);
                c.setRequestProperty("apikey",SUPABASE_KEY);c.setRequestProperty("Prefer","return=minimal");
                code=c.getResponseCode();InputStream is=code>=200&&code<300?c.getInputStream():c.getErrorStream();if(is!=null)response=read(is);
            }catch(Exception e){response=e.getMessage()==null?"Network error":e.getMessage();}
            finally{if(c!=null)c.disconnect();}
            final int resultCode=code;final String result=response;
            runOnUiThread(()->{
                if(resultCode>=200&&resultCode<300){Toast.makeText(this,"Entry deleted",Toast.LENGTH_SHORT).show();loadSubmissions();}
                else new android.app.AlertDialog.Builder(this).setTitle("Delete failed ("+resultCode+")")
                    .setMessage(result.isEmpty()?"Supabase did not allow this deletion. Check the DELETE policy.":result).setPositiveButton("OK",null).show();
            });
        }).start();
    }

    private String formatDate(String raw){
        try{SimpleDateFormat in=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",Locale.US);Date d=in.parse(raw.substring(0,19));return new SimpleDateFormat("dd MMM yyyy, hh:mm a",Locale.getDefault()).format(d);}
        catch(Exception e){return raw.replace("T"," ");}
    }
    private String read(InputStream is)throws Exception{
        if(is==null)return "";ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[2048];int n;
        while((n=is.read(b))!=-1)out.write(b,0,n);return out.toString("UTF-8");
    }
}
