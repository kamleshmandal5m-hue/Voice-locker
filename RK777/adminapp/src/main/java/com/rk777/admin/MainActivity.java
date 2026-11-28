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
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(0xff080f20);
        LinearLayout page=column();pad(page,12,12,12,22);scroll.addView(page);setContentView(scroll);

        LinearLayout hero=column();pad(hero,16,15,16,16);hero.setBackground(gradient(0xff5146d8,0xff087fbd,18));
        TextView brand=text("RK 777  •  CONTROL CENTER",10,0xffdceaff,true);hero.addView(brand);
        TextView title=text("Admin Dashboard",23,white,true);LinearLayout.LayoutParams titleLp=lp(-2,-2);titleLp.topMargin=dp(7);hero.addView(title,titleLp);
        TextView sub=text("Live survey submissions overview",11,0xffd9eaff,false);LinearLayout.LayoutParams subLp=lp(-2,-2);subLp.topMargin=dp(3);hero.addView(sub,subLp);
        page.addView(hero,lp(-1,-2));

        LinearLayout summary=row();LinearLayout.LayoutParams sumLp=lp(-1,-2);sumLp.topMargin=dp(10);page.addView(summary,sumLp);
        LinearLayout total=column();pad(total,13,10,12,10);total.setBackground(shape(0xff15223a,16));
        total.addView(text("TOTAL ENTRIES",9,0xffaebbd2,true));
        totalText=text("—",27,0xff42e0c1,true);LinearLayout.LayoutParams numLp=lp(-2,-2);numLp.topMargin=dp(2);total.addView(totalText,numLp);
        total.addView(text("All submissions",9,0xffaebbd2,false));
        summary.addView(total,new LinearLayout.LayoutParams(0,dp(83),1));
        LinearLayout sync=column();pad(sync,13,10,12,10);sync.setBackground(shape(0xff15223a,16));
        sync.addView(text("STATUS",9,0xffaebbd2,true));
        statusText=text("●  CONNECTING",15,0xffffc45e,true);LinearLayout.LayoutParams statusLp=lp(-1,-2);statusLp.topMargin=dp(9);sync.addView(statusText,statusLp);
        sync.addView(text("Tap refresh to sync",9,0xffaebbd2,false));
        LinearLayout.LayoutParams syncLp=new LinearLayout.LayoutParams(0,dp(83),1);syncLp.leftMargin=dp(9);summary.addView(sync,syncLp);

        LinearLayout section=row();LinearLayout.LayoutParams sectionLp=lp(-1,-2);sectionLp.topMargin=dp(16);page.addView(section,sectionLp);
        TextView heading=text("Recent submissions",17,white,true);section.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        refreshButton=new Button(this);refreshButton.setText("↻ Refresh");refreshButton.setTextSize(11);refreshButton.setTextColor(white);refreshButton.setAllCaps(false);
        refreshButton.setBackground(gradient(0xffff9a25,0xffff5365,12));refreshButton.setPadding(dp(10),0,dp(10),0);
        section.addView(refreshButton,lp(94,36));refreshButton.setOnClickListener(v->loadSubmissions());

        LinearLayout statusRow=row();LinearLayout.LayoutParams srp=lp(-1,-2);srp.topMargin=dp(7);page.addView(statusRow,srp);
        updatedText=text("Newest entries appear first  •  #1 = oldest",9,0xff9ba9c0,false);statusRow.addView(updatedText);
        list=column();LinearLayout.LayoutParams listLp=lp(-1,-2);listLp.topMargin=dp(8);page.addView(list,listLp);

        TextView footer=text("RK 777  •  ADMIN PANEL",9,0xff71809b,true);footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams flp=lp(-1,-2);flp.topMargin=dp(16);page.addView(footer,flp);
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
        statusText.setText("●  CONNECTED");statusText.setTextColor(0xff43e2bd);
        updatedText.setText("Newest entries appear first  •  #1 = oldest  •  "+total+" total");
        if(total==0){
            LinearLayout empty=column();empty.setGravity(Gravity.CENTER);pad(empty,16,22,16,22);empty.setBackground(shape(0xff15223a,16));
            TextView msg=text("No submissions yet",14,white,true);msg.setGravity(Gravity.CENTER);empty.addView(msg);
            TextView hint=text("New user entries will appear here",10,0xff9ba9c0,false);hint.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams hp=lp(-1,-2);hp.topMargin=dp(5);empty.addView(hint,hp);list.addView(empty,lp(-1,-2));return;
        }
        for(int i=0;i<total;i++){
            JSONObject item=rows.optJSONObject(i);if(item==null)continue;
            int serial=total-i;int accent=accents[i%accents.length];
            LinearLayout card=column();pad(card,12,10,12,10);card.setBackground(shape(0xff15223a,16));
            LinearLayout top=row();
            TextView nameLabel=text("NAME",8,0xff8495b3,true);top.addView(nameLabel,new LinearLayout.LayoutParams(0,-2,1));
            TextView number=text("#"+serial,10,white,true);number.setGravity(Gravity.CENTER);pad(number,10,5,10,5);number.setBackground(shape(accent,20));top.addView(number);
            card.addView(top);
            TextView name=text(item.optString("full_name","No name"),14,white,true);name.setMaxLines(2);
            LinearLayout.LayoutParams nameLp=lp(-1,-2);nameLp.topMargin=dp(2);card.addView(name,nameLp);

            LinearLayout fields=row();LinearLayout.LayoutParams fieldsLp=lp(-1,-2);fieldsLp.topMargin=dp(8);card.addView(fields,fieldsLp);
            LinearLayout mobileBox=column();pad(mobileBox,9,7,7,7);mobileBox.setBackground(shape(0xff1c2d49,10));
            mobileBox.addView(text("MOBILE",8,0xff8fa2c0,true));
            TextView mobile=text(item.optString("mobile","—"),12,0xff66d8f4,true);mobile.setMaxLines(1);mobile.setSingleLine(true);mobileBox.addView(mobile);
            fields.addView(mobileBox,new LinearLayout.LayoutParams(0,-2,1));
            LinearLayout dateBox=column();pad(dateBox,9,7,7,7);dateBox.setBackground(shape(0xff1c2d49,10));
            dateBox.addView(text("DATE & TIME",8,0xff8fa2c0,true));
            TextView date=text(formatDate(item.optString("created_at","")),10,0xffd1d9e8,false);date.setMaxLines(1);date.setSingleLine(true);dateBox.addView(date);
            LinearLayout.LayoutParams dateLp=new LinearLayout.LayoutParams(0,-2,1);dateLp.leftMargin=dp(7);fields.addView(dateBox,dateLp);

            LinearLayout actions=row();actions.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
            Button deleteButton=new Button(this);deleteButton.setText("DELETE");deleteButton.setTextSize(9);deleteButton.setTextColor(0xffff8795);
            deleteButton.setAllCaps(false);deleteButton.setMinHeight(0);deleteButton.setMinimumHeight(0);deleteButton.setPadding(dp(9),0,dp(9),0);
            deleteButton.setBackground(shape(0xff3a2638,9));
            LinearLayout.LayoutParams deleteLp=lp(78,30);deleteLp.topMargin=dp(7);actions.addView(deleteButton,deleteLp);
            String recordId=item.optString("id","");String recordName=item.optString("full_name","this entry");
            deleteButton.setOnClickListener(v->confirmDelete(recordId,recordName));card.addView(actions);
            LinearLayout.LayoutParams clp=lp(-1,-2);clp.topMargin=dp(8);list.addView(card,clp);
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
