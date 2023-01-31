package com.example.accessiblitydemo

import android.accessibilityservice.AccessibilityService
import android.app.Application
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.view.accessibility.AccessibilityNodeInfo

import android.os.Build

import android.os.Bundle

import android.annotation.TargetApi
import android.app.ActivityManager
import android.content.*

import android.content.pm.PackageManager
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import java.lang.StringBuilder
import java.util.*


/**
 * @author: CaiSongL
 * @date: 2022/5/21 19:35
 */
open class BaseAccessibilityService :AccessibilityService() , CoroutineScope by MainScope(){

    val TAG = "无障碍"

    var tmpClassName: CharSequence? = null
    var tmpPkgName: CharSequence? = null
    var tmpActivityName = ""
    private var mAccessibilityManager: AccessibilityManager? = null
    private var mContext: Application? = null
    private var mInstance: BaseAccessibilityService? = null
    lateinit var activityManager : ActivityManager

    val setPackages: MutableSet<String> = mutableSetOf()
    val setHomes: MutableSet<String> = mutableSetOf()
    val setIMEApps: MutableSet<String> = mutableSetOf()

    fun init(context: Application) {
        mContext = context
        mAccessibilityManager = mContext?.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager?
    }

    fun getInstance(): BaseAccessibilityService? {
        if (mInstance == null) {
            mInstance = BaseAccessibilityService()
        }
        return mInstance
    }

    open fun getData(){

    }

    override fun onCreate() {
        super.onCreate()
        getData()
        updatePackage()
        activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        logD(TAG,"onCreate")
    }

    fun updatePackage() {
        setPackages.clear()

        // find all launchers
        var intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        var resolveInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        for (e in resolveInfoList) {
            setPackages.add(e.activityInfo.packageName)
        }
        // find all homes
        // find all homes
        intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        resolveInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        for (e in resolveInfoList) {
            setHomes.add(e.activityInfo.packageName)
        }
        // find all input methods
        val inputMethodInfoList =
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).inputMethodList
        for (e in inputMethodInfoList) {
            setIMEApps.add(e.packageName)
        }
        // ignore some packages in hardcoded way
        // https://support.google.com/a/answer/7292363?hl=en

        // 从 pkgLaunchers 中删除白名单、系统、家庭和临时包

        setPackages.removeAll(setHomes)
        setPackages.removeAll(setIMEApps)
        setPackages.remove(packageName)
        setPackages.remove("com.android.settings")
    }


    override fun onServiceConnected() {
        super.onServiceConnected()
        logD(TAG,"onServiceConnected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        try {
            logD(TAG,"onAccessibilityEvent")
            event?.let {
                when(event.eventType){
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED->{
                        tmpPkgName = event!!.packageName
                        tmpClassName = event!!.className
                        if (tmpPkgName == null || tmpClassName == null) return
                        tmpActivityName = event.className.toString();
                        var componentName = ComponentName(
                            event.packageName.toString(),
                            event.className.toString()
                        )
                        try {
                            var activityName = packageManager.getActivityInfo(componentName, 0).toString()
                            tmpActivityName = activityName.substring(activityName.indexOf(" "),activityName.indexOf("}"))
                        }catch (e:Exception){
                            logE(TAG,e.message.toString(),e)
                        }
                        AccessibilityUtil.instant.updateActivityAndPgName(tmpPkgName.toString(),tmpActivityName)
//                        AccessibilityUtil.instant.tmpPkgName = tmpPkgName.toString()
//                        AccessibilityUtil.instant.tmpClassName = tmpClassName.toString()
//                        AccessibilityUtil.instant.tmpActivityName = tmpActivityName
                    }
                }
                onCustomEvent(event)
            }
        }catch (e:Exception){
            logE(TAG,e.message.toString(),e)
        }

    }

    override fun onUnbind(intent: Intent?): Boolean {

        return super.onUnbind(intent)

    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }



    open fun onCustomEvent(event: AccessibilityEvent){


    }


    override fun onInterrupt() {
        logD(TAG,"onInterrupt")
    }


    /**
     * Check当前辅助服务是否启用
     *
     * @param serviceName serviceName
     * @return 是否启用
     */
    private fun checkAccessibilityEnabled(serviceName: String): Boolean {
        val accessibilityServices =
            mAccessibilityManager!!.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        for (info in accessibilityServices) {
            if (info.id == serviceName) {
                return true
            }
        }
        return false
    }

    /**
     * 前往开启辅助服务界面
     */
    fun goAccess() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        mContext!!.startActivity(intent)
    }

    /**
     * 模拟点击事件
     *
     * @param nodeInfo nodeInfo
     */
    fun performViewClick(nodeInfo: AccessibilityNodeInfo?) {
        var nodeInfo = nodeInfo
        if (nodeInfo == null) {
            return
        }
        while (nodeInfo != null) {
            if (nodeInfo.isClickable) {
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                break
            }
            nodeInfo = nodeInfo.parent
        }
    }

    /**
     * 模拟返回操作
     */
    fun performBackClick() {
        try {
            Thread.sleep(500)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * 模拟下滑操作
     */
    fun performScrollBackward() {
        try {
            Thread.sleep(500)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * 模拟上滑操作
     */
    @Synchronized
    fun performScrollForward(nodeInfo: AccessibilityNodeInfo) {
        try {
            Thread.sleep(500)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }

    /**
     * 查找对应文本的View
     *
     * @param text text
     * @return View
     */
    fun findViewByText(text: String?): AccessibilityNodeInfo? {
        return findViewByText(text, false)
    }

    /**
     * 查找对应文本的View
     *
     * @param text      text
     * @param clickable 该View是否可以点击
     * @return View
     */
    fun findViewByText(text: String?, clickable: Boolean): AccessibilityNodeInfo? {
        val accessibilityNodeInfo = rootInActiveWindow ?: return null
        val nodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByText(text)
        if (nodeInfoList != null && !nodeInfoList.isEmpty()) {
            for (nodeInfo in nodeInfoList) {
                if (nodeInfo != null && nodeInfo.isClickable == clickable) {
                    return nodeInfo
                }
            }
        }
        return null
    }

    /**
     * 查找对应ID的View
     *
     * @param id id
     * @return View
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun findViewByID(id: String?): AccessibilityNodeInfo? {
        val accessibilityNodeInfo = rootInActiveWindow ?: return null
        val nodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId(
            id!!
        )
        if (nodeInfoList != null && !nodeInfoList.isEmpty()) {
            for (nodeInfo in nodeInfoList) {
                if (nodeInfo != null) {
                    return nodeInfo
                }
            }
        }
        return null
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun clickTextViewByText(text: String?) : AccessibilityNodeInfo? {
        val accessibilityNodeInfo = rootInActiveWindow ?: return null
        val nodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByText(text)
        if (nodeInfoList != null && nodeInfoList.isNotEmpty()) {
            for (nodeInfo in nodeInfoList) {
                if (nodeInfo != null) {
                    return nodeInfo
                }
            }
        }
        return null
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun clickTextViewByID(id: String?) : AccessibilityNodeInfo?{
        val accessibilityNodeInfo = rootInActiveWindow ?: return null
        val nodeInfoList = accessibilityNodeInfo.findAccessibilityNodeInfosByViewId(
            id!!
        )
        if (nodeInfoList != null && !nodeInfoList.isEmpty()) {
            for (nodeInfo in nodeInfoList) {
                if (nodeInfo != null) {
                    return nodeInfo
                }
            }
        }
        return null
    }

    /**
     * 模拟输入
     * @param nodeInfo nodeInfo
     * @param text     text
     */
    fun inputText(nodeInfo: AccessibilityNodeInfo, text: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val arguments = Bundle()
            arguments.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            val clipboard: ClipboardManager =
                getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("label", text)
            clipboard.setPrimaryClip(clip)
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        }
    }

    private fun dumpRootNode(root: AccessibilityNodeInfo): String {
        val nodeList = ArrayList<AccessibilityNodeInfo>()
        val dumpString = StringBuilder()
        dumpChildNodes(root, nodeList, dumpString, "")
        return dumpString.toString()
    }

    private fun dumpChildNodes(
        root: AccessibilityNodeInfo?,
        list: MutableList<AccessibilityNodeInfo>,
        dumpString: StringBuilder,
        indent: String
    ) {
        if (root == null) return
        list.add(root)
        dumpString.append(
            """
            $indent${describeAccessibilityNode(root).toString()}
            
            """.trimIndent()
        )
        for (n in 0 until root.childCount) {
            val child = root.getChild(n)
            dumpChildNodes(child, list, dumpString, "$indent ")
        }
    }

    /**
     * 查找所有的控件
     */
    open fun findAllNode(
        roots: List<AccessibilityNodeInfo>,
        list: MutableList<AccessibilityNodeInfo>,
        indent: String
    ) {
        val childrenList = ArrayList<AccessibilityNodeInfo>()
        for (e in roots) {
            if (e == null) continue
            list.add(e)
            logD(TAG,describeAccessibilityNode(e))
            for (n in 0 until e.childCount) {
                childrenList.add(e.getChild(n))
            }
        }
        if (childrenList.isNotEmpty()) {
            findAllNode(childrenList, list, "$indent  ")
        }
    }

    open fun describeAccessibilityNode(e: AccessibilityNodeInfo?): String {
        if (e == null) {
            return "null"
        }
        var result = "Node"
        result += " class =" + e.className.toString()
        val rect = Rect()
        e.getBoundsInScreen(rect)
        result += String.format(
            " Position=[%d, %d, %d, %d]",
            rect.left,
            rect.right,
            rect.top,
            rect.bottom
        )
        val id: CharSequence? = e.viewIdResourceName
        if (id != null) {
            result += " ResourceId=$id"
        }
        val description = e.contentDescription
        if (description != null) {
            result += " Description=$description"
        }
        val text = e.text
        if (text != null) {
            result += " Text=$text"
        }
        return result
    }

    /**
     * 模拟点击
     */
    fun click(X: Int, Y: Int, start_time: Long, duration: Long): Boolean {
        val path = Path()
        path.moveTo(X.toFloat(), Y.toFloat())
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val builder = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, start_time, duration))
            this.dispatchGesture(builder.build(), null, null)
        } else {
            false
        }
    }

    fun ShowToastInIntentService(sText: String?) {
        logD("测试",sText!!)
//        val myContext: Context = this
//        // show one toast in 5 seconds only
//        Handler(Looper.getMainLooper()).post {
//            val toast = Toast.makeText(myContext, sText, Toast.LENGTH_SHORT)
//            toast.show()
//        }
    }
}