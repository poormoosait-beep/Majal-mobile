package com.majal.mobile

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private val navy = Color.rgb(8, 19, 39)
    private val panel = Color.rgb(17, 35, 62)
    private val cyan = Color.rgb(34, 211, 238)
    private val muted = Color.rgb(167, 190, 216)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = navy
        window.navigationBarColor = navy
        window.decorView.setBackgroundColor(navy)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP
            setPadding(24, 44, 24, 24)
            layoutDirection = LinearLayout.LAYOUT_DIRECTION_RTL
            setBackgroundColor(navy)
        }

        root.addView(label("MAJAL MOBILE", 14f, cyan, true))
        root.addView(label("شبکه گستر مجال", 28f, Color.WHITE, true).apply {
            setPadding(0, 12, 0, 4)
        })
        root.addView(label("مدیریت و پایش هوشمند شبکه شرکت‌ها", 14f, muted, false))

        root.addView(card("وضعیت اتصال", "هنوز هیچ VPN متصل نشده است"))
        root.addView(card("شرکت‌های من", "هنوز شرکتی ثبت نشده؛ در نسخه بعدی اضافه می‌شود"))
        root.addView(card("دستگاه‌های شبکه", "میکروتیک • سیسکو • سرور و ESXi"))
        root.addView(card("هشدارها و گزارش‌ها", "هشدارها پس از اتصال به شبکه نمایش داده می‌شوند"))

        root.addView(label("پشتیبانی شبکه گستر مجال", 13f, muted, true).apply {
            setPadding(0, 28, 0, 4)
        })
        root.addView(label("09386066008", 16f, cyan, true))

        setContentView(root)
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.RIGHT
        }

    private fun card(title: String, detail: String): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = LinearLayout.LAYOUT_DIRECTION_RTL
            setPadding(18, 16, 18, 16)
            setBackgroundColor(panel)
        }
        box.addView(label(title, 17f, Color.WHITE, true))
        box.addView(label(detail, 13f, muted, false).apply {
            setPadding(0, 8, 0, 0)
        })
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 16 }
        return box.apply { layoutParams = params }
    }
}
