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
        LinearLayout gridRow=null;
        for(int i=0;i<total;i++){
            if(i%2==0){
                gridRow=row();gridRow.setGravity(Gravity.TOP);
                LinearLayout.LayoutParams rp=lp(-1,-2);if(i>0)rp.topMargin=dp(9);list.addView(gridRow,rp);
            }
            JSONObject item=rows.optJSONObject(i);if(item==null)continue;
            View card=createRecordCard(item,total-i,i);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);
            if(i%2==1)cp.leftMargin=dp(8);
            gridRow.addView(card,cp);
        }
        if(total%2==1){
            View blank=new View(this);gridRow.addView(blank,new LinearLayout.LayoutParams(0,1,1));
        }
    }

    private View createRecordCard(JSONObject item,int serial,int index){
        int accent=accents[index%accents.length];
        LinearLayout card=column();pad(card,10,10,10,10);card.setBackground(shape(0xff15223a,14));
        LinearLayout top=row();
        TextView label=text("ENTRY",8,0xff8495b3,true);top.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        TextView number=text("#"+serial,10,white,true);number.setGravity(Gravity.CENTER);pad(number,9,4,9,4);number.setBackground(shape(accent,18));top.addView(number);
        card.addView(top);
        card.setOnClickListener(v->showUserDetail(item,serial));

        card.addView(text("NAME",8,0xff8495b3,true));
        TextView name=text(item.optString("full_name","No name"),12,white,true);name.setMaxLines(2);
        LinearLayout.LayoutParams np=lp(-1,-2);np.topMargin=dp(2);card.addView(name,np);

        TextView mobileLabel=text("MOBILE",8,0xff8495b3,true);
        LinearLayout.LayoutParams mlp=lp(-1,-2);mlp.topMargin=dp(8);card.addView(mobileLabel,mlp);
        TextView mobile=text(item.optString("mobile","—"),11,0xff66d8f4,true);mobile.setSingleLine(true);mobile.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams mp=lp(-1,-2);mp.topMargin=dp(2);card.addView(mobile,mp);

        TextView dateLabel=text("DATE & TIME",8,0xff8495b3,true);
        LinearLayout.LayoutParams dlp=lp(-1,-2);dlp.topMargin=dp(8);card.addView(dateLabel,dlp);
        TextView date=text(formatDate(item.optString("created_at","")),9,0xffd1d9e8,false);date.setMaxLines(2);
        LinearLayout.LayoutParams dtp=lp(-1,-2);dtp.topMargin=dp(2);card.addView(date,dtp);

        Button deleteButton=new Button(this);deleteButton.setText("DELETE");deleteButton.setTextSize(9);deleteButton.setTextColor(0xffff8795);
        deleteButton.setAllCaps(false);deleteButton.setMinHeight(0);deleteButton.setMinimumHeight(0);deleteButton.setPadding(dp(5),0,dp(5),0);
        deleteButton.setBackground(shape(0xff3a2638,8));
        LinearLayout.LayoutParams delp=lp(-1,32);delp.topMargin=dp(9);card.addView(deleteButton,delp);
        String recordId=item.optString("id","");String recordName=item.optString("full_name","this entry");
        deleteButton.setOnClickListener(v->confirmDelete(recordId,recordName));
        return card;
    }


    private void showUserDetail(JSONObject item,int serial){
        LinearLayout page=column();page.setBackgroundColor(0xff080f20);pad(page,14,14,14,20);
        TextView back=text("‹  BACK",13,0xff8edfff,true);back.setOnClickListener(v->{showDashboard();loadSubmissions();});page.addView(back,lp(-1,42));
        LinearLayout hero=column();pad(hero,16,16,16,16);hero.setBackground(gradient(0xff5146d8,0xff087fbd,18));
        hero.addView(text("USER PROFILE  •  #"+serial,10,0xffdceaff,true));
        TextView userName=text(item.optString("full_name","No name"),22,white,true);userName.setMaxLines(2);
        LinearLayout.LayoutParams unp=lp(-1,-2);unp.topMargin=dp(6);hero.addView(userName,unp);
        TextView phone=text(item.optString("mobile","—"),13,0xffd9eaff,false);
        LinearLayout.LayoutParams php=lp(-1,-2);php.topMargin=dp(4);hero.addView(phone,php);
        page.addView(hero,lp(-1,-2));
        TextView hint=text("SELECT A SECTION",10,0xff9ba9c0,true);
        LinearLayout.LayoutParams hip=lp(-1,-2);hip.topMargin=dp(18);page.addView(hint,hip);
        String[] labels={"DATA","MESSAGE","APPLICATION","STORAGE"};
        String[] subtitles={"User details","Messages","Applications","Files & storage"};
        int[] colors={0xff536dfe,0xff00a98f,0xffff9b45,0xffdf5b91};
        LinearLayout tiles=row();tiles.setGravity(Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams tlp=lp(-1,76);tlp.topMargin=dp(10);page.addView(tiles,tlp);
        for(int j=0;j<4;j++){
            final String section=labels[j];
            LinearLayout tile=column();tile.setGravity(Gravity.CENTER);pad(tile,2,7,2,7);tile.setBackground(shape(0xff15223a,11));
            TextView symbol=text(new String[]{"▦","●","▤","▣"}[j],15,white,true);
            symbol.setGravity(Gravity.CENTER);symbol.setBackground(shape(colors[j],8));tile.addView(symbol,lp(25,25));
            TextView name=text(labels[j],8,white,true);name.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams nlp=lp(-1,-2);nlp.topMargin=dp(6);tile.addView(name,nlp);
            tile.setOnClickListener(v->showUserSection(item,serial,section));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(76),1);if(j>0)cp.leftMargin=dp(5);tiles.addView(tile,cp);
        }
        setContentView(page);
    }

    private void showUserSection(JSONObject item,int serial,String section){
        LinearLayout page=column();page.setBackgroundColor(0xff080f20);pad(page,16,16,16,20);
        TextView back=text("‹  USER PROFILE",13,0xff8edfff,true);back.setOnClickListener(v->showUserDetail(item,serial));page.addView(back,lp(-1,44));
        TextView title=text(section,22,white,true);LinearLayout.LayoutParams tp=lp(-1,-2);tp.topMargin=dp(12);page.addView(title,tp);
        LinearLayout panel=column();pad(panel,15,16,15,16);panel.setBackground(shape(0xff15223a,14));
        LinearLayout.LayoutParams pp=lp(-1,-2);pp.topMargin=dp(14);page.addView(panel,pp);
        if("DATA".equals(section)){
            addDetailRow(panel,"NAME",item.optString("full_name","—"));
            addDetailRow(panel,"MOBILE",item.optString("mobile","—"));
            addDetailRow(panel,"DATE & TIME",formatDate(item.optString("created_at","")));
            addDetailRow(panel,"RECORD ID",item.optString("id","—"));
        }else{
            panel.addView(text("No "+section.toLowerCase(Locale.US)+" saved for this user yet.",13,0xffc4cee0,false));
            TextView note=text("This section is ready for its own user-specific content.",10,0xff8999b5,false);
            LinearLayout.LayoutParams np=lp(-1,-2);np.topMargin=dp(7);panel.addView(note,np);
        }
        setContentView(page);
    }

    private void addDetailRow(LinearLayout parent,String label,String value){
        parent.addView(text(label,9,0xff8495b3,true));
        TextView v=text(value,13,white,true);LinearLayout.LayoutParams vp=lp(-1,-2);vp.topMargin=dp(3);vp.bottomMargin=dp(12);parent.addView(v,vp);
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
        try{
            String value=raw==null?"":raw.trim();
            if(value.isEmpty())return "Time unavailable";
            // Supabase timestamptz is returned with an explicit UTC/offset suffix.
            // Strip fractional seconds only; preserve Z or the numeric UTC offset.
            value=value.replaceFirst("(\\.\\d+)(Z|[+-]\\d{2}:\\d{2})$","$2");
            if(value.matches(".*[+-]\\d{2}:\\d{2}$")||value.endsWith("Z")){
                SimpleDateFormat in=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX",Locale.US);
                in.setLenient(false);
                Date d=in.parse(value);
                SimpleDateFormat out=new SimpleDateFormat("dd MMM yyyy, hh:mm a",Locale.ENGLISH);
                out.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
                return out.format(d)+" IST";
            }
            // Older rows without an offset are interpreted as UTC (Supabase timestamptz).
            SimpleDateFormat in=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",Locale.US);
            in.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            Date d=in.parse(value.substring(0,19));
            SimpleDateFormat out=new SimpleDateFormat("dd MMM yyyy, hh:mm a",Locale.ENGLISH);
            out.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
            return out.format(d)+" IST";
        }catch(Exception e){return raw==null?"Time unavailable":raw;}
    }
    private String read(InputStream is)throws Exception{
        if(is==null)return "";ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[2048];int n;
        while((n=is.read(b))!=-1)out.write(b,0,n);return out.toString("UTF-8");
    }
}
