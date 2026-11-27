package com.rk777.user;

import android.app.*;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.Calendar;

public class MainActivity extends Activity {
    int navy = Color.rgb(16, 21, 34), gold = Color.rgb(255, 200, 87);
    EditText name1, mobile1, name2, mobile2, dob;
    LinearLayout page;
    String savedName="", savedMobile="";
    public void onCreate(Bundle b){ super.onCreate(b); showFirst(); }
    TextView text(String s,int size){ TextView t=new TextView(this); t.setText(s); t.setTextColor(Color.WHITE); t.setTextSize(size); t.setGravity(Gravity.CENTER); t.setPadding(8,16,8,16); return t; }
    EditText field(String hint,String value,int type){ EditText e=new EditText(this); e.setSingleLine(true); e.setHint(hint); e.setText(value); e.setTextColor(Color.WHITE); e.setHintTextColor(0xffb5bdca); e.setInputType(type); e.setPadding(18,12,18,12); e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(gold)); return e; }
    void base(String heading){
        ScrollView scroll=new ScrollView(this); page=new LinearLayout(this); page.setOrientation(1); page.setGravity(Gravity.CENTER_HORIZONTAL); page.setPadding(24,35,24,30); page.setBackgroundColor(navy);
        scroll.addView(page); setContentView(scroll); TextView brand=text("RK 777",30); brand.setTextColor(gold); brand.setTypeface(null,Typeface.BOLD); page.addView(brand); page.addView(text(heading,20));
    }
    void addField(EditText e){ LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,10,0,10); page.addView(e,p); }
    void button(String label,Runnable action){ Button b=new Button(this); b.setText(label); b.setTextColor(navy); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(gold)); page.addView(b,new LinearLayout.LayoutParams(-1,-2)); b.setOnClickListener(v->action.run()); }
    void showFirst(){
        base("Survey • Step 1 of 3");
        page.addView(text("Enter your details",16));
        name1=field("Full name","",android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS); addField(name1);
        mobile1=field("Mobile number","",android.text.InputType.TYPE_CLASS_PHONE); addField(mobile1);
        button("NEXT",()->{
            savedName=name1.getText().toString().trim(); savedMobile=mobile1.getText().toString().trim();
            if(savedName.isEmpty()||savedMobile.length()!=10){ Toast.makeText(this,"Name aur 10-digit mobile number bharein",Toast.LENGTH_LONG).show(); return; }
            showSecond();
        });
    }
    void showSecond(){
        base("Survey • Step 2 of 3");
        page.addView(text("Confirm your details",16));
        name2=field("Full name",savedName,android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS); addField(name2);
        mobile2=field("Mobile number",savedMobile,android.text.InputType.TYPE_CLASS_PHONE); addField(mobile2);
        dob=field("Date of birth (DD/MM/YYYY)","",android.text.InputType.TYPE_CLASS_DATETIME); dob.setFocusable(false); dob.setOnClickListener(v->pickDate()); addField(dob);
        button("SUBMIT",()->{
            savedName=name2.getText().toString().trim(); savedMobile=mobile2.getText().toString().trim();
            if(savedName.isEmpty()||savedMobile.length()!=10||dob.getText().toString().isEmpty()){ Toast.makeText(this,"Sabhi details sahi bharein",Toast.LENGTH_LONG).show(); return; }
            showThanks();
        });
    }
    void pickDate(){
        Calendar c=Calendar.getInstance();
        new DatePickerDialog(this,(v,y,m,d)->dob.setText(String.format(java.util.Locale.US,"%02d/%02d/%04d",d,m+1,y)),c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();
    }
    void showThanks(){ base("Survey complete"); TextView thanks=text("Thank you for submitting survey",25); thanks.setTextColor(gold); thanks.setTypeface(null,Typeface.BOLD); page.addView(thanks); page.addView(text("Your response has been submitted on this device.",14)); }
}
