package io.github.takusan23.colorinversionservice;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

import static android.provider.Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED;

public class MainActivity extends AppCompatActivity {

    //ボタン
    private Button start_Button;
    private Button stop_Button;
    //権限TextView
    private TextView permission_TextView;
    //間隔変更
    private Button timer_Button;
    private EditText timer_EditText;
    //Preference
    private SharedPreferences pref_setting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Preference
        pref_setting = PreferenceManager.getDefaultSharedPreferences(this);

        start_Button = findViewById(R.id.service_start_button);
        stop_Button = findViewById(R.id.service_stop_button);
        permission_TextView = findViewById(R.id.permission_check_textview);

        timer_Button = findViewById(R.id.time_button);
        timer_EditText = findViewById(R.id.time_editText);

        //パーミッション（WRITE_SECURE_SETTINGS）チェック
        //なかったらADBで操作してもらう
        /*
         *  ADB コマンド
         *  adb shell pm grant io.github.takusan23.colorinversionservice android.permission.WRITE_SECURE_SETTINGS
         *
         *
         * */
        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_SECURE_SETTINGS);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            //権限がある
            String success_text = "権限があります。利用可能です。";
            permission_TextView.setText(success_text);
            //Drawable
            Drawable success_icon = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_done_black_24dp, null);
            success_icon.setTint(Color.parseColor("#008000"));
            permission_TextView.setCompoundDrawablesWithIntrinsicBounds(success_icon, null, null, null);

            //クリックイベント設定
            start_Button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //開始
                    Intent intent = new Intent(MainActivity.this, ColorInversionTimerService.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        //Oreo以降
                        startForegroundService(intent);
                    } else {
                        //Nougat
                        startService(intent);
                    }
                }
            });
            stop_Button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //終了
                    Intent intent = new Intent(getApplication(), ColorInversionTimerService.class);
                    stopService(intent);
                    //色反転戻す
                    setColorInversionOff();
                    //一応通知
                    showNotification("終了しました",R.drawable.ic_invert_colors_off_black_24dp);
                }
            });


        } else {
            //権限がない
            String error_text = "権限がありません。ADBで[WRITE_SECURE_SETTINGS]パーミッションを" + getPackageName() + "に付与してください";
            Toast.makeText(MainActivity.this, error_text, Toast.LENGTH_LONG).show();
            permission_TextView.setText(error_text);
            //Drawable
            Drawable error_icon = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_error_outline_black_24dp, null);
            error_icon.setTint(Color.parseColor("#ff0000"));
            permission_TextView.setCompoundDrawablesWithIntrinsicBounds(error_icon, null, null, null);
        }

        //間隔変更機能
        timer_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //String→long
                long time = Long.valueOf(timer_EditText.getText().toString());
                //かける1000でミリ秒にする
                time *= 1000;
                //Preference保存
                SharedPreferences.Editor editor = pref_setting.edit();
                editor.putLong("time", time);
                editor.apply();
                Intent intent = new Intent(MainActivity.this, ColorInversionTimerService.class);
                //終了
                stopService(intent);
                //サービス再起動
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    //Oreo以降
                    startForegroundService(intent);
                } else {
                    //Nougat
                    startService(intent);
                }
            }
        });

    }

    /**
     * 色反転を戻す
     */
    private void setColorInversionOff() {
        //パーミッション（WRITE_SECURE_SETTINGS）チェック
        //なかったら諦める
        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_SECURE_SETTINGS);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            //戻す
            Settings.Secure.putInt(getContentResolver(), ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 0);
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

}
