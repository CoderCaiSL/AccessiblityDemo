package com.example.accessiblitydemo

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat

/**
 * @author: CaiSongL
 * @date: 2023/1/31 14:58
 */
public class WatchingAccessibilityService  : BaseAccessibilityService() {
    val ACTION_NOTIFICATION_RECEIVER = "om.example.accessiblitydemo.ACTION_NOTIFICATION_RECEIVER"


    companion object{
        var sInstance: WatchingAccessibilityService? = null
        fun getInstance(): WatchingAccessibilityService? {
            return sInstance
        }
    }

//    @SuppressLint("NewApi")
//    override fun onAccessibilityEvent(event: AccessibilityEvent) {
//
//    }
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        super.onAccessibilityEvent(event)
    }


    //创建前台通知，可写成方法体，也可单独写成一个类
    private fun createForegroundNotification(context: Context): Notification {
        val channelId = "CustomAccService";
        val channelName = "自动脚本";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, channelName,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.lightColor = Color.BLUE;
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel);
        }
        //点击通知时可进入的Activity
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("自动脚本服务")
            .setContentText("此服务运行则代表能设置好的脚本")
            .setSmallIcon(R.drawable.ic_launcher_foreground)//通知显示的图标
            .setContentIntent(pendingIntent)//点击通知进入Activity
            .setTicker("通知的提示语")
            .build();
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        sInstance = this
        if (SPHelper.isShowWindow(this)) {
            NotificationActionReceiver.showNotification(this, false)
        }
        sendBroadcast(Intent(QuickSettingTileService.ACTION_UPDATE_TITLE))
        super.onServiceConnected()
        AccessibilityUtil.instant.showActivityCustomizationDialog(application)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        sInstance = null
//        TasksWindow.dismiss(this)
        NotificationActionReceiver.cancelNotification(this)
        sendBroadcast(Intent(QuickSettingTileService.ACTION_UPDATE_TITLE))
        return super.onUnbind(intent)
    }
}