package com.example.accessiblitydemo

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity

public class MainActivity : AppCompatActivity() {

    private lateinit var checkBox: CheckBox

    companion object{
        val EXTRA_FROM_QS_TILE: String? = "from_qs_tile"
        val ACTION_STATE_CHANGED: String? = "com.willme.topactivity.ACTION_STATE_CHANGED"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkBox = findViewById<CheckBox>(R.id.sw_state)
        initView()
        if (!checkBox.isChecked){
            AlertDialog.Builder(this)
                .setMessage(R.string.dialog_enable_accessibility_msg)
                .setPositiveButton(R.string.dialog_enable_accessibility_positive_btn,
                    DialogInterface.OnClickListener { dialog, which ->
                        SPHelper.setIsShowWindow(this@MainActivity, true)
                        val intent = Intent()
                        intent.action = "android.settings.ACCESSIBILITY_SETTINGS"
                        startActivity(intent)
                    })
                .create()
                .show()
        }
        checkBox.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->

        })
    }

    private fun initView() {
        if (WatchingAccessibilityService.getInstance() == null) {
            checkBox.isChecked = false
            SPHelper.setIsShowWindow(this, true)
        }else{
            checkBox.isChecked = true
            SPHelper.setIsShowWindow(this, false)
        }
    }


    override fun onResume() {
        super.onResume()
        initView()
    }
}