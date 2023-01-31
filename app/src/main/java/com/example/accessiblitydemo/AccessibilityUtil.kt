package com.example.accessiblitydemo

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.*
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.*
import java.lang.ref.WeakReference

/**
 * @author: CaiSongL
 * @date: 2022/5/23 11:59
 */
class AccessibilityUtil private constructor(){

    private var tvActivityName: TextView ?=null
    private var tvPackageName: TextView ?= null
    private val TAG = "布局检测器"
    var tmpPkgName = ""
    var tmpActivityName = ""
    var weakService : WeakReference<BaseAccessibilityService> ?= null
    var isRunJin10VideoJB  = false
    var isRunTest = false;

    init {
    }

    private object Holder{
        val INSTANT = AccessibilityUtil()
    }


    companion object{
        val instant = Holder.INSTANT
    }


    // display activity customization dialog, and allow users to pick widget or positions
    @SuppressLint("ClickableViewAccessibility")
    fun showActivityCustomizationDialog(application: Application) {
        weakService = WeakReference<BaseAccessibilityService>(WatchingAccessibilityService.sInstance)
        if (weakService == null){
            Toast.makeText(application,"脚本服务尚未启动",Toast.LENGTH_SHORT).show()
            return
        }
        // show activity customization window
        val windowManager = weakService?.get()?.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)
        val b = metrics.heightPixels > metrics.widthPixels
        val width = if (b) metrics.widthPixels else metrics.heightPixels
        val height = if (b) metrics.heightPixels else metrics.widthPixels
        val inflater = LayoutInflater.from(weakService?.get())
        // activity customization view
        val viewCustomization: View = inflater.inflate(R.layout.layout_activity_customization, null)
        tvPackageName = viewCustomization.findViewById<TextView>(R.id.tv_package_name)
        tvActivityName = viewCustomization.findViewById<TextView>(R.id.tv_activity_name)
        val tvWidgetInfo = viewCustomization.findViewById<TextView>(R.id.tv_widget_info)
        val tvPositionInfo = viewCustomization.findViewById<TextView>(R.id.tv_position_info)
        val btShowOutline = viewCustomization.findViewById<Button>(R.id.button_show_outline)
        val btAddWidget = viewCustomization.findViewById<Button>(R.id.button_add_widget)
        val btShowTarget = viewCustomization.findViewById<Button>(R.id.button_show_target)
        val btAddPosition = viewCustomization.findViewById<Button>(R.id.button_add_position)
        val btDumpScreen = viewCustomization.findViewById<Button>(R.id.button_dump_screen)
        val btQuit = viewCustomization.findViewById<Button>(R.id.button_quit)
        val viewTarget: View = inflater.inflate(R.layout.layout_accessibility_node_desc, null)
        val layoutOverlayOutline = viewTarget.findViewById<FrameLayout>(R.id.frame)
        val imageTarget = ImageView(weakService?.get())
        imageTarget.setImageResource(R.drawable.ic_target)

        // define view positions
        val customizationParams: WindowManager.LayoutParams
        val outlineParams: WindowManager.LayoutParams
        val targetParams: WindowManager.LayoutParams
        customizationParams = WindowManager.LayoutParams()
        customizationParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        customizationParams.format = PixelFormat.TRANSPARENT
        customizationParams.gravity = Gravity.START or Gravity.TOP
        customizationParams.flags =
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        customizationParams.width = width
        customizationParams.height = height / 5
        customizationParams.x = (metrics.widthPixels - customizationParams.width) / 2
        customizationParams.y = metrics.heightPixels - customizationParams.height
        customizationParams.alpha = 0.8f
        outlineParams = WindowManager.LayoutParams()
        outlineParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        outlineParams.format = PixelFormat.TRANSPARENT
        outlineParams.gravity = Gravity.START or Gravity.TOP
        outlineParams.width = metrics.widthPixels
        outlineParams.height = metrics.heightPixels
        outlineParams.flags =
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        outlineParams.alpha = 0f
        targetParams = WindowManager.LayoutParams()
        targetParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        targetParams.format = PixelFormat.TRANSPARENT
        targetParams.flags =
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        targetParams.gravity = Gravity.START or Gravity.TOP
        targetParams.height = width / 6
        targetParams.width = targetParams.height
        targetParams.x = (metrics.widthPixels - targetParams.width) / 2
        targetParams.y = (metrics.heightPixels - targetParams.height) / 2
        targetParams.alpha = 0f
        viewCustomization.setOnTouchListener(object : View.OnTouchListener {
            var x = 0
            var y = 0
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        x = Math.round(event.rawX)
                        y = Math.round(event.rawY)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        customizationParams.x = Math.round(customizationParams.x + (event.rawX - x))
                        customizationParams.y = Math.round(customizationParams.y + (event.rawY - y))
                        x = Math.round(event.rawX)
                        y = Math.round(event.rawY)
                        windowManager.updateViewLayout(viewCustomization, customizationParams)
                    }
                }
                return true
            }
        })
        imageTarget.setOnTouchListener(object : View.OnTouchListener {
            var x = 0
            var y = 0
            var width = targetParams.width / 2
            var height = targetParams.height / 2
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        btAddPosition.isEnabled = true
                        targetParams.alpha = 0.9f
                        windowManager.updateViewLayout(imageTarget, targetParams)
                        x = Math.round(event.rawX)
                        y = Math.round(event.rawY)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        targetParams.x = Math.round(targetParams.x + (event.rawX - x))
                        targetParams.y = Math.round(targetParams.y + (event.rawY - y))
                        x = Math.round(event.rawX)
                        y = Math.round(event.rawY)
                        windowManager.updateViewLayout(imageTarget, targetParams)
                        tvPackageName!!.setText(tmpPkgName)
                        tvActivityName!!.setText(tmpActivityName)
                        tvPositionInfo.text =
                            "X轴：" + targetParams.x + width + "    " + "Y轴：" + targetParams.y + height + "    " + "(其他参数默认)"
                    }
                    MotionEvent.ACTION_UP -> {
                        targetParams.alpha = 0.5f
                        windowManager.updateViewLayout(imageTarget, targetParams)
                    }
                }
                return true
            }
        })
        btShowOutline.setOnClickListener(View.OnClickListener { v ->
            val button = v as Button
            if (outlineParams.alpha == 0f) {
                val root: AccessibilityNodeInfo =
                    weakService?.get()?.rootInActiveWindow ?: return@OnClickListener
                layoutOverlayOutline.removeAllViews()
                val roots = ArrayList<AccessibilityNodeInfo>()
                roots.add(root)
                val nodeList = ArrayList<AccessibilityNodeInfo>()
                findAllNode(roots, nodeList, "")
                nodeList.sortWith(Comparator { o1, o2 ->
                    val rectA = Rect()
                    val rectB = Rect()
                    o1.getBoundsInScreen(rectA)
                    o2.getBoundsInScreen(rectB)
                    rectB.width() * rectB.height() - rectA.width() * rectA.height()
                })
                for (e in nodeList) {
                    val temRect = Rect()
                    e.getBoundsInScreen(temRect)
                    val params = FrameLayout.LayoutParams(temRect.width(), temRect.height())
                    params.leftMargin = temRect.left
                    params.topMargin = temRect.top
                    if (!e.contentDescription.isNullOrEmpty()){
                        logE("测试界面爬取",e.contentDescription.toString()!!)
                    }
                    val img = ImageView(weakService?.get())
                    img.setBackgroundResource(R.drawable.node)
                    img.isFocusableInTouchMode = true
                    img.setOnClickListener { v -> v.requestFocus() }
                    img.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                        if (hasFocus) {
                            val classCheck = Class.forName(e.className.toString())
                            try {
                                var tmp = classCheck.getDeclaredConstructor().newInstance() as SeekBar
                                logD("测试进度条",tmp?.progress.toString()+"/")
                            }catch (e:Exception){

                            }
                            val fields = classCheck.fields
                            try {
                                for (tmp in fields){
                                    tmp.isAccessible = true
                                    logD("测试",tmp.name+"/"+tmp.get(null)+"//"+classCheck.getField("text"))
                                }
                            }catch (e:Exception){
                                logD("测试",e.message.toString())
                            }
                            val cId: CharSequence? = e.viewIdResourceName
                            val cDesc = e.contentDescription
                            val cText = e.text
                            btAddWidget.isEnabled = true
                            tvPackageName!!.text = tmpPkgName+"("+e.className+")"
                            tvActivityName!!.text = tmpActivityName
                            tvWidgetInfo.text =
                                "click:" + (if (e.isClickable) "true" else "false") + " " + "bonus:" + temRect.toShortString() + " " + "id:" +
                                        (cId?.toString()
                                    ?: "null") + " " + "desc:" + (cDesc?.toString()
                                    ?: "null") + " " + "text:" + (cText?.toString() ?: "null")
                            if (e.isClickable){
                                e.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            }
                            v.setBackgroundResource(R.drawable.node_focus)
                        } else {
                            v.setBackgroundResource(R.drawable.node)
                        }
                    }
                    layoutOverlayOutline.addView(img, params)
                }
                outlineParams.alpha = 0.5f
                outlineParams.flags =
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                windowManager.updateViewLayout(viewTarget, outlineParams)
                tvPackageName!!.setText(tmpPkgName)
                tvActivityName!!.setText(tmpActivityName)
                button.text = "隐藏布局"
            } else {
                outlineParams.alpha = 0f
                outlineParams.flags =
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                windowManager.updateViewLayout(viewTarget, outlineParams)
                btAddWidget.isEnabled = false
                button.text = "更新布局"
            }
        })
        btShowTarget.setOnClickListener { v ->
            val button = v as Button
            if (targetParams.alpha == 0f) {
//                positionDescription.packageName = tmpPkgName
//                positionDescription.activityName = tmpClassName
                targetParams.alpha = 0.5f
                targetParams.flags =
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                windowManager.updateViewLayout(imageTarget, targetParams)
                tvPackageName!!.setText(tmpPkgName)
                tvActivityName!!.setText(tmpActivityName)
                isRunTest = true
                button.text = "隐藏准心"
            } else {
                targetParams.alpha = 0f
                targetParams.flags =
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                windowManager.updateViewLayout(imageTarget, targetParams)
                btAddPosition.isEnabled = false
                isRunTest = false
                button.text = "显示准心"
            }
        }
        btAddWidget.setOnClickListener {
//
//            customAccViewModel.dataCustomAcc.value?.let {
//                var tmp : MutableList<PackageWidgetDescription> = mutableListOf()
//                tmp.addAll(customAccViewModel?.dataCustomAcc?.value!!)
//                tmp?.add(widgetDescription.copy())
//                customAccViewModel.dataCustomAcc.postValue(tmp)
//            }
//            btAddWidget.isEnabled = false
//            tvPackageName.text = widgetDescription.packageName + " (以下控件数据已保存)"
        }
        btAddPosition.setOnClickListener {
//            customAccViewModel.dataCustomAcc.value = mutableListOf()
            btAddPosition.isEnabled = false
            tvPackageName!!.text = "数据已清除，请重新进行添加"
        }
        btDumpScreen.setOnClickListener(View.OnClickListener {
            val root: AccessibilityNodeInfo =
                weakService?.get()?.rootInActiveWindow ?: return@OnClickListener
            val result: String = dumpRootNode(root)
            logD(TAG,result)
            val clipboard = weakService?.get()?.getSystemService(AccessibilityService.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("ACTIVITY", result)
            clipboard.setPrimaryClip(clip)
//            "窗口控件已复制到剪贴板！".showToast()
        })
        btQuit.setOnClickListener {
//            weakService?.get()?.let {
//                it.launch {
//                    customAccViewModel.saveData().collectLatest {
//                        windowManager.removeViewImmediate(viewTarget)
//                        windowManager.removeViewImmediate(viewCustomization)
//                        windowManager.removeViewImmediate(imageTarget)
//                        if (weakService?.get() is CustomAccService){
//                            (weakService?.get() as CustomAccService).getData()
//                        }
//                    }
//                }
//            }
        }
        windowManager.addView(viewTarget, outlineParams)
        windowManager.addView(viewCustomization, customizationParams)
        windowManager.addView(imageTarget, targetParams)
    }

    public fun getPageName(application: Application){
        val rtis = (application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getRunningTasks(1)
        tmpPkgName = rtis.get(0).topActivity!!.packageName
        tmpActivityName = rtis.get(0).topActivity!!.className
    }

    public fun updateActivityAndPgName(pgName : String,activityName:String){
        tmpPkgName = pgName
        tvPackageName?.text = pgName
        if (!activityName.isNullOrEmpty()){
            tmpActivityName = activityName
            tvActivityName?.text = activityName
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





}