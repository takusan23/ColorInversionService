package io.github.takusan23.colorinversionservice;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;

import java.util.concurrent.TimeUnit;

import static android.provider.Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED;

public class QuickSettingTileService extends TileService {

    //追加したとき
    @Override
    public void onTileAdded() {
        SharedPreferences pref_setting = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Tile tile = getQsTile();
        //ON/OFF
        if (pref_setting.getBoolean("color", false)) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.setLabel("サービス実行中");
            tile.setIcon(Icon.createWithResource(getApplicationContext(), R.drawable.color_inversion_icon));
        } else {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setLabel("サービス休止中");
            tile.setIcon(Icon.createWithResource(getApplicationContext(), R.drawable.ic_invert_colors_off_black_24dp));
        }

    }

    //押したとき
    @Override
    public void onClick() {
        Tile tile = getQsTile();
        switch (tile.getState()) {
            case Tile.STATE_ACTIVE:
                tile.setState(Tile.STATE_INACTIVE);
                tile.setLabel("サービス休止中");
                tile.setIcon(Icon.createWithResource(getApplicationContext(), R.drawable.ic_invert_colors_off_black_24dp));
                colorService(1);
                break;
            case Tile.STATE_INACTIVE:
                tile.setState(Tile.STATE_ACTIVE);
                tile.setLabel("サービス実行中");
                tile.setIcon(Icon.createWithResource(getApplicationContext(), R.drawable.ic_invert_colors_black_24dp));
                colorService(0);
                break;
        }

        tile.updateTile();
    }


    @Override
    public void onTileRemoved() {
    }

    @Override
    public void onStartListening() {
    }

    @Override
    public void onStopListening() {
    }

    /**
     * サービス実行
     *
     * @param i 1無効2有効
     */
    private void colorService(int i) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
        //パーミッション（WRITE_SECURE_SETTINGS）チェック
        //なかったら諦める
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_SECURE_SETTINGS);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            //現在のモードを取得する
            if (i == 1) {
                //ON
                //終了
                Intent intent = new Intent(getApplication(), ColorInversionTimerService.class);
                stopService(intent);
                //色反転戻す
                setColorInversionOff();
                //QSTile用保存
                editor.putBoolean("color", false);
                editor.apply();
            } else {
                //OFF
                //開始
                Intent intent = new Intent(this, ColorInversionTimerService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    //Oreo以降
                    startForegroundService(intent);
                } else {
                    //Nougat
                    startService(intent);
                }
                //QSTile用保存
                editor.putBoolean("color", true);
                editor.apply();
            }
        }
    }


    /**
     * 通知を表示する
     *
     * @param message       通知の本文
     * @param icon_drawable 通知アイコン（R.drawable～の形で）
     */
    private void showNotification(String message, int icon_drawable) {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
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

    /**
     * 色反転を戻す
     */
    private void setColorInversionOff() {
        //パーミッション（WRITE_SECURE_SETTINGS）チェック
        //なかったら諦める
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_SECURE_SETTINGS);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            //戻す
            Settings.Secure.putInt(getContentResolver(), ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 0);
        }
    }

}
