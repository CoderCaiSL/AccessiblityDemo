package com.example.accessiblitydemo

import android.view.accessibility.AccessibilityNodeInfo

/**
 * @author: CaiSongL
 * @date: 2023/2/1 17:58
 */
data class AccessibilityData(val id:String,var state:Boolean,var activityName:String="",var accessibilityNodeInfo: AccessibilityNodeInfo)