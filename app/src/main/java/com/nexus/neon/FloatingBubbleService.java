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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FloatingBubbleService extends Service {
    private static final String CHANNEL_ID = "nexus_bubble";
    private WindowManager windowManager;
    private View bubble;
    private View menu;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams menuParams;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(9, buildNotification());
        if (Settings.canDrawOverlays(this)) showBubble();
        else stopSelf();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "NEXUS NEON bubble",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Keeps the NEXUS NEON floating shortcut available.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        return builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("NEXUS NEON je aktívny")
                .setContentText("Plávajúca bublina je pripravená.")
                .setContentIntent(pending).setOngoing(true).build();
    }

    private int overlayType() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private GradientDrawable background(int color, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(18));
        d.setStroke(dp(1), stroke);
        return d;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, int size) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(Color.WHITE);
        v.setTextSize(size);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(12), dp(10), dp(12), dp(10));
        return v;
    }

    private void showBubble() {
        if (bubble != null) return;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        TextView button = text("N", 22);
        button.setTextColor(Color.rgb(8, 8, 23));
        button.setBackground(background(Color.rgb(77, 205, 255), Color.rgb(174, 112, 255)));
        bubble = button;
        bubbleParams = new WindowManager.LayoutParams(dp(58), dp(58), overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = dp(16);
        bubbleParams.y = dp(220);
        button.setOnTouchListener(new View.OnTouchListener() {
            private int startX, startY;
            private float downX, downY;
            private boolean moved;
            @Override public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = bubbleParams.x; startY = bubbleParams.y;
                        downX = event.getRawX(); downY = event.getRawY(); moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int)(event.getRawX() - downX);
                        int dy = (int)(event.getRawY() - downY);
                        if (Math.abs(dx) > dp(5) || Math.abs(dy) > dp(5)) moved = true;
                        bubbleParams.x = Math.max(0, startX + dx);
                        bubbleParams.y = Math.max(0, startY + dy);
                        windowManager.updateViewLayout(bubble, bubbleParams);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) toggleMenu();
                        return true;
                }
                return false;
            }
        });
        windowManager.addView(bubble, bubbleParams);
    }

    private void toggleMenu() {
        if (menu != null) { removeMenu(); return; }
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8), dp(8), dp(8), dp(8));
        box.setBackground(background(Color.rgb(17, 17, 40), Color.rgb(157, 100, 255)));
        TextView heading = text("NEXUS NEON", 16);
        heading.setTextColor(Color.rgb(82, 218, 255));
        box.addView(heading);
        addMenuButton(box, "OPEN NEXUS APP", () -> {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            removeMenu();
        });
        addMenuButton(box, "CLOSE MENU", this::removeMenu);
        addMenuButton(box, "HIDE BUBBLE", () -> stopSelf());
        menu = box;
        menuParams = new WindowManager.LayoutParams(dp(210), WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        menuParams.gravity = Gravity.TOP | Gravity.START;
        menuParams.x = bubbleParams.x;
        menuParams.y = bubbleParams.y + dp(66);
        windowManager.addView(menu, menuParams);
    }

    private void addMenuButton(LinearLayout parent, String label, Runnable action) {
        TextView b = text(label, 13);
        b.setBackground(background(Color.rgb(35, 31, 72), Color.rgb(73, 67, 125)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(3), dp(4), dp(3), dp(4));
        parent.addView(b, lp);
        b.setOnClickListener(v -> action.run());
    }

    private void removeMenu() {
        if (menu != null && windowManager != null) {
            try { windowManager.removeView(menu); } catch (Exception ignored) { }
            menu = null;
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (bubble == null && Settings.canDrawOverlays(this)) showBubble();
        return START_STICKY;
    }

    @Override public void onDestroy() {
        removeMenu();
        if (bubble != null && windowManager != null) {
            try { windowManager.removeView(bubble); } catch (Exception ignored) { }
            bubble = null;
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
