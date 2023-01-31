package com.example.accessiblitydemo

import android.annotation.TargetApi
import android.content.*
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.accessiblitydemo.QuickSettingTileService.UpdateTileReceiver
import com.example.accessiblitydemo.SPHelper
import com.example.accessiblitydemo.MainActivity
import com.example.accessiblitydemo.QuickSettingTileService
import com.example.accessiblitydemo.WatchingAccessibilityService
import com.example.accessiblitydemo.NotificationActionReceiver

/**
 * Created by Wen on 5/3/16.
 */
@TargetApi(Build.VERSION_CODES.N)
class QuickSettingTileService : TileService() {
    private var mReceiver: UpdateTileReceiver? = null
    override fun onCreate() {
        super.onCreate()
        mReceiver = UpdateTileReceiver()
    }

    override fun onTileAdded() {
        SPHelper.setQSTileAdded(this, true)
        sendBroadcast(Intent(MainActivity.ACTION_STATE_CHANGED))
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        SPHelper.setQSTileAdded(this, false)
        sendBroadcast(Intent(MainActivity.ACTION_STATE_CHANGED))
    }

    override fun onStartListening() {
        registerReceiver(mReceiver, IntentFilter(ACTION_UPDATE_TITLE))
        super.onStartListening()
        updateTile()
    }

    override fun onStopListening() {
        unregisterReceiver(mReceiver)
        super.onStopListening()
    }

    override fun onClick() {
        if (WatchingAccessibilityService.getInstance() == null || !Settings.canDrawOverlays(this)) {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra(MainActivity.EXTRA_FROM_QS_TILE, true)
            startActivityAndCollapse(intent)
        } else {
            SPHelper.setIsShowWindow(this, !SPHelper.isShowWindow(this))
            if (SPHelper.isShowWindow(this)) {
//                TasksWindow.show(this, null);
                NotificationActionReceiver.showNotification(this, false)
            } else {
//                TasksWindow.dismiss(this);
                NotificationActionReceiver.showNotification(this, true)
            }
            sendBroadcast(Intent(MainActivity.ACTION_STATE_CHANGED))
        }
    }

    private fun updateTile() {
        if (WatchingAccessibilityService.getInstance() == null) {
            qsTile.state = Tile.STATE_INACTIVE
        } else {
            qsTile.state =
                if (SPHelper.isShowWindow(this)) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        }
        qsTile.updateTile()
    }

    internal inner class UpdateTileReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateTile()
        }
    }

    companion object {
        const val ACTION_UPDATE_TITLE = "com.example.accessiblitydemo.ACTION.UPDATE_TITLE"
        fun updateTile(context: Context) {
            requestListeningState(
                context.applicationContext,
                ComponentName(context, QuickSettingTileService::class.java)
            )
            context.sendBroadcast(Intent(ACTION_UPDATE_TITLE))
        }
    }
}