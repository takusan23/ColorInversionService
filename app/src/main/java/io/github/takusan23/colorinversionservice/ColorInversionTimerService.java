package io.github.takusan23.colorinversionservice;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;

import java.util.Timer;
import java.util.TimerTask;

import static android.provider.Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED;

public class ColorInversionTimerService extends Service {

    //定期実行
    private Timer timer;

    private NotificationManager notificationManager;
    private SharedPreferences pref_setting;


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        notificationManager = getSystemService(NotificationManager.class);
        pref_setting = PreferenceManager.getDefaultSharedPreferences(this);
        //Oreoでサービスが止まらないように
        //サービス動いてるよ通知をすぐに出す
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            String channel = "color_inversion_service";
            String name = "色反転サービス実行中通知";
            NotificationChannel notificationChannel = new NotificationChannel(channel, name, NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription("Android 8からサービスに関して厳しくなったので");
            //チャンネル登録
            notificationManager.createNotificationChannel(notificationChannel);
            //通知作成
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channel)
                    .setContentTitle(name)
                    .setContentText("定期的に色反転が有効になります。")
                    .setSmallIcon(R.drawable.ic_error_outline_black_24dp);
            //通知を出せばサービスは起動されたままになる
            startForeground(R.string.app_name, builder.build());
        }


        //定期実行
        timer = new Timer();
        long time = pref_setting.getLong("time",60000);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                //パーミッション（WRITE_SECURE_SETTINGS）チェック
                //なかったら諦める
                int permissionCheck = ContextCompat.checkSelfPermission(ColorInversionTimerService.this, Manifest.permission.WRITE_SECURE_SETTINGS);
                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                    try {
                        //現在のモードを取得する
                        if (Settings.Secure.getInt(getContentResolver(), ACCESSIBILITY_DISPLAY_INVERSION_ENABLED) == 1) {
                            //ON
                            Settings.Secure.putInt(getContentResolver(), ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 0);
                            showNotification("色反転は無効になりました。", R.drawable.ic_invert_colors_off_black_24dp);
                        } else {
                            //OFF
                            Settings.Secure.putInt(getContentResolver(), ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 1);
                            showNotification("色反転は有効になりました。", R.drawable.ic_invert_colors_black_24dp);
                        }
                    } catch (Settings.SettingNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 0, time);     //開始、間隔

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //タイマー終了
        timer.cancel();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 通知を表示する
     *
     * @param message       通知の本文
     * @param icon_drawable 通知アイコン（R.drawable～の形で）
     */
    private void showNotification(String message, int icon_drawable) {
        //一応Nullチェック
        //Oreo以降とNougat
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            //Oreo以降
            String channel = "color_inversion_timer_service";
            String name = "定期色反転";
            NotificationChannel notificationChannel = new NotificationChannel(channel, name, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("色反転を定期的に実行します。\n権限があることを確認してください。");
            //チャンネル登録
            notificationManager.createNotificationChannel(notificationChannel);
            //通知作成
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channel)
                    .setContentTitle("[定期]色反転サービス")
                    .setContentText(message)
                    .setSmallIcon(icon_drawable);
            //表示
            NotificationManagerCompat.from(this).notify(1, builder.build());
        } else {
            //Nougat 非推奨出るけど多分回避不可能
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setContentTitle("[定期]色反転サービス")
                    .setContentText(message)
                    .setSmallIcon(icon_drawable);
            builder.setPriority(NotificationManager.IMPORTANCE_MAX);
            //バイブなし（ヘッドアップ通知したいだけ）
            builder.setVibrate(new long[]{});
            NotificationManagerCompat.from(this).notify(1, builder.build());

        }

    }
}
