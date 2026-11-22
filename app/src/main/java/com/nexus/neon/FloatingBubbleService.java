package com.nexus.neon;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.HashMap;
import java.util.Map;

public class FloatingBubbleService extends Service {
    private static final String CHANNEL_ID = "nexus_bubble";
    private WindowManager windowManager;
    private View bubble;
    private View menu;
    private EditText editor;
    private TextView output;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams menuParams;

    @Override public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(9, buildNotification());
        if (Settings.canDrawOverlays(this)) showBubble(); else stopSelf();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "NEXUS NEON bubble", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Floating NEXUS NEON script editor shortcut.");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        return b.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("NEXUS NEON je aktívny")
                .setContentText("Plávajúci editor je pripravený.").setContentIntent(pending).setOngoing(true).build();
    }

    private int overlayType() {
        return Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
    }
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private GradientDrawable bg(int color, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); d.setStroke(dp(1), stroke); return d;
    }

    private TextView label(String value, int size) {
        TextView v = new TextView(this); v.setText(value); v.setTextColor(Color.WHITE); v.setTextSize(size);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD); v.setGravity(Gravity.CENTER); v.setPadding(dp(9), dp(8), dp(9), dp(8)); return v;
    }

    private void showBubble() {
        if (bubble != null) return;
        windowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
        TextView b = label("N", 22); b.setTextColor(Color.rgb(8,8,23)); b.setBackground(bg(Color.rgb(77,205,255), Color.rgb(174,112,255), 20));
        bubble = b;
        bubbleParams = new WindowManager.LayoutParams(dp(58), dp(58), overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.START; bubbleParams.x = dp(16); bubbleParams.y = dp(220);
        b.setOnTouchListener(new View.OnTouchListener() {
            int sx, sy; float dx, dy; boolean moved;
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction()==MotionEvent.ACTION_DOWN) { sx=bubbleParams.x; sy=bubbleParams.y; dx=e.getRawX(); dy=e.getRawY(); moved=false; return true; }
                if (e.getAction()==MotionEvent.ACTION_MOVE) {
                    int x=(int)(e.getRawX()-dx), y=(int)(e.getRawY()-dy);
                    if (Math.abs(x)>dp(5)||Math.abs(y)>dp(5)) moved=true;
                    bubbleParams.x=Math.max(0,sx+x); bubbleParams.y=Math.max(0,sy+y); windowManager.updateViewLayout(bubble,bubbleParams); return true;
                }
                if (e.getAction()==MotionEvent.ACTION_UP) { if (!moved) toggleMenu(); return true; }
                return false;
            }
        });
        windowManager.addView(bubble,bubbleParams);
    }

    private void toggleMenu() {
        if (menu != null) { removeMenu(); return; }
        LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(dp(10),dp(8),dp(10),dp(10));
        panel.setBackground(bg(Color.rgb(13,13,31),Color.rgb(157,100,255),18));
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setOrientation(LinearLayout.HORIZONTAL);
        TextView title=label("NEXUS NEON",16); title.setTextColor(Color.rgb(82,218,255));
        top.addView(title,new LinearLayout.LayoutParams(0,dp(42),1));
        TextView close=label("×",24); close.setOnClickListener(v->removeMenu()); top.addView(close,new LinearLayout.LayoutParams(dp(42),dp(42)));
        panel.addView(top);
        TextView sub=label("LOCAL SCRIPT EDITOR",10); sub.setTextColor(Color.rgb(167,160,205)); panel.addView(sub);
        editor=new EditText(this); editor.setTextColor(Color.rgb(226,220,255)); editor.setHintTextColor(Color.rgb(130,125,160));
        editor.setTextSize(13); editor.setGravity(Gravity.TOP|Gravity.START); editor.setTypeface(Typeface.MONOSPACE);
        editor.setPadding(dp(10),dp(10),dp(10),dp(10)); editor.setHint("-- Napíš jednoduchý príklad\nprint(\"Hello!\")");
        editor.setText("-- NEXUS local test\nlocal player = \"Player\"\nprint(player)");
        editor.setMinLines(6); editor.setMaxLines(9); editor.setBackground(bg(Color.rgb(7,7,20),Color.rgb(57,50,101),12));
        panel.addView(editor,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(190)));
        LinearLayout actions=new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
        TextView run=label("▶ TEST",12); run.setBackground(bg(Color.rgb(92,57,180),Color.rgb(126,105,255),12));
        actions.addView(run,new LinearLayout.LayoutParams(0,dp(44),1));
        TextView hide=label("MINIMIZE",11); hide.setBackground(bg(Color.rgb(35,31,72),Color.rgb(73,67,125),12));
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(0,dp(44),1); hp.setMargins(dp(6),0,0,0); actions.addView(hide,hp);
        panel.addView(actions);
        TextView outTitle=label("OUTPUT",11); outTitle.setGravity(Gravity.START|Gravity.CENTER_VERTICAL); outTitle.setTextColor(Color.rgb(82,218,255)); panel.addView(outTitle);
        output=label("Ready. Local test mode.",11); output.setGravity(Gravity.START|Gravity.TOP); output.setTypeface(Typeface.MONOSPACE);
        output.setBackground(bg(Color.rgb(7,7,20),Color.rgb(45,42,78),10)); panel.addView(output,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(58)));
        TextView note=label("Runs only in this local sandbox — not inside Roblox.",9); note.setTextColor(Color.rgb(164,158,190)); panel.addView(note);
        run.setOnClickListener(v->runLocalTest());
        hide.setOnClickListener(v->{ hideKeyboard(); removeMenu(); });
        menu=panel;
        menuParams=new WindowManager.LayoutParams(Math.min(dp(330),getResources().getDisplayMetrics().widthPixels-dp(24)),
                WindowManager.LayoutParams.WRAP_CONTENT,overlayType(),WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);
        menuParams.gravity=Gravity.TOP|Gravity.START; menuParams.x=Math.min(bubbleParams.x,Math.max(0,getResources().getDisplayMetrics().widthPixels-dp(340)));
        menuParams.y=Math.min(bubbleParams.y+dp(66),dp(300)); menuParams.softInputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        windowManager.addView(menu,menuParams);
        editor.requestFocus();
    }

    private void runLocalTest() {
        if (editor==null||output==null) return;
        try { output.setText(runLimited(editor.getText().toString())); }
        catch (Exception e) { output.setText("Error: "+e.getMessage()); }
    }

    // Deliberately tiny educational interpreter: assignments and print of strings, numbers, booleans or variables only.
    private String runLimited(String source) {
        Map<String,String> vars=new HashMap<>(); StringBuilder result=new StringBuilder();
        for (String raw:source.split("\\r?\\n")) {
            String s=raw.trim(); if(s.isEmpty()||s.startsWith("--")) continue;
            if(s.matches("(local\\s+)?[A-Za-z_][A-Za-z0-9_]*\\s*=.*")) {
                String[] pair=s.replaceFirst("^local\\s+","").split("=",2); String name=pair[0].trim(); String val=pair[1].trim();
                vars.put(name,resolve(val,vars)); continue;
            }
            if(s.startsWith("print(")&&s.endsWith(")")) {
                String val=s.substring(6,s.length()-1).trim(); result.append(resolve(val,vars)).append("\\n"); continue;
            }
            throw new IllegalArgumentException("Supported: local name = value and print(value)");
        }
        return result.length()==0 ? "(No output)" : result.toString().trim();
    }

    private String resolve(String value, Map<String,String> vars) {
        if(vars.containsKey(value)) return vars.get(value);
        if(value.matches("\".*\"") || value.matches("'.*'")) return value.substring(1,value.length()-1);
        if(value.matches("-?[0-9]+(\\.[0-9]+)?") || value.equals("true") || value.equals("false")) return value;
        throw new IllegalArgumentException("Unsupported value: "+value);
    }

    private void hideKeyboard() {
        try { ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(editor.getWindowToken(),0); } catch(Exception ignored) {}
    }
    private void removeMenu() {
        if(menu!=null&&windowManager!=null) { hideKeyboard(); try{windowManager.removeView(menu);}catch(Exception ignored){} menu=null; editor=null; output=null; }
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId) { if(bubble==null&&Settings.canDrawOverlays(this)) showBubble(); return START_STICKY; }
    @Override public void onDestroy() { removeMenu(); if(bubble!=null&&windowManager!=null){try{windowManager.removeView(bubble);}catch(Exception ignored){} bubble=null;} super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
