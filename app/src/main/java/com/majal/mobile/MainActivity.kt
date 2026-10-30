package com.majal.mobile

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private val navy = Color.rgb(8, 19, 39)
    private val panel = Color.rgb(17, 35, 62)
    private val cyan = Color.rgb(34, 211, 238)
    private val muted = Color.rgb(167, 190, 216)
    private val prefs by lazy { getSharedPreferences("majal_inventory", MODE_PRIVATE) }
    private val devices = mutableListOf<Device>()
    private lateinit var deviceList: LinearLayout

    data class Device(val id: String, val company: String, val name: String, val host: String, val type: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = navy
        window.navigationBarColor = navy
        window.decorView.setBackgroundColor(navy)
        loadDevices()

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(22, 30, 22, 24)
            setBackgroundColor(navy)
        }
        page.addView(label("MAJAL MOBILE", 14f, cyan, true))
        page.addView(label("پنل مدیریت تجهیزات شبکه", 25f, Color.WHITE, true).apply { setPadding(0, 8, 0, 4) })
        page.addView(label("شبکه گستر مجال • مدیریت از طریق VPN فعال گوشی", 13f, muted, false))
        page.addView(label("نوع تجهیزات", 18f, Color.WHITE, true).apply { setPadding(0, 24, 0, 10) })

        page.addView(actionButton("MikroTik RouterOS — مدیریت روتر", true) {
            startActivity(Intent(this, RouterManagerActivity::class.java))
        })
        page.addView(actionButton("Cisco — سوئیچ، پورت‌ها و VLAN", false) {
            toast("بخش Cisco هنوز در حال توسعه است؛ اتصال واقعی در نسخه بعد اضافه می‌شود.")
        })
        page.addView(actionButton("VMware ESXi — هاست و ماشین‌های مجازی", false) {
            toast("بخش ESXi هنوز در حال توسعه است؛ اتصال واقعی در نسخه بعد اضافه می‌شود.")
        })
        page.addView(actionButton("مودم و روترهای دیگر", false) {
            toast("پشتیبانی مودم‌ها بر اساس مدل و پروتکل مدیریتی اضافه می‌شود.")
        })

        page.addView(label("افزودن دستگاه به فهرست", 18f, Color.WHITE, true).apply { setPadding(0, 24, 0, 8) })
        val company = input("نام شرکت")
        val name = input("نام دستگاه (مثلاً روتر دفتر)")
        val host = input("IP یا نام میزبان (مثلاً 192.168.1.1)")
        val type = input("نوع: MikroTik / Cisco / ESXi / Modem")
        page.addView(company)
        page.addView(name)
        page.addView(host)
        page.addView(type)
        page.addView(actionButton("ذخیره در فهرست دستگاه‌ها", true) {
            val c = company.text.toString().trim()
            val n = name.text.toString().trim()
            val h = host.text.toString().trim()
            val t = type.text.toString().trim().ifBlank { "MikroTik" }
            if (c.isBlank() || n.isBlank() || h.isBlank()) {
                toast("نام شرکت، نام دستگاه و IP را وارد کن")
            } else {
                devices.add(Device(System.currentTimeMillis().toString(), c, n, h, t))
                saveDevices()
                company.text.clear(); name.text.clear(); host.text.clear(); type.text.clear()
                renderDevices()
                toast("دستگاه در فهرست ذخیره شد")
            }
        })

        page.addView(label("فهرست دستگاه‌ها", 18f, Color.WHITE, true).apply { setPadding(0, 24, 0, 8) })
        deviceList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        page.addView(deviceList)
        page.addView(label("توجه: فهرست فعلی دستگاه‌های ثبت‌شده است، نه کشف خودکار دستگاه‌های متصل. کشف زنده و وضعیت آنلاین باید از API هر نوع دستگاه پیاده‌سازی و آزمایش شود.", 12f, muted, false).apply { setPadding(0, 18, 0, 8) })
        page.addView(label("پشتیبانی شبکه گستر مجال • 09386066008", 13f, cyan, true).apply { setPadding(0, 22, 0, 10) })
        setContentView(ScrollView(this).apply { addView(page) })
        renderDevices()
    }

    private fun input(hintText: String) = EditText(this).apply {
        hint = hintText
        textSize = 14f
        setTextColor(Color.WHITE)
        setHintTextColor(muted)
        setSingleLine(true)
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(14, 10, 14, 10)
        setBackgroundColor(panel)
    }

    private fun loadDevices() {
        devices.clear()
        try {
            val arr = JSONArray(prefs.getString("devices", "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                devices.add(Device(o.optString("id"), o.optString("company"), o.optString("name"), o.optString("host"), o.optString("type")))
            }
        } catch (_: Exception) { devices.clear() }
    }

    private fun saveDevices() {
        val arr = JSONArray()
        devices.forEach { d ->
            arr.put(JSONObject().apply {
                put("id", d.id); put("company", d.company); put("name", d.name)
                put("host", d.host); put("type", d.type)
            })
        }
        prefs.edit().putString("devices", arr.toString()).apply()
    }

    private fun renderDevices() {
        if (!::deviceList.isInitialized) return
        deviceList.removeAllViews()
        if (devices.isEmpty()) {
            deviceList.addView(label("هنوز دستگاهی ثبت نشده است.", 13f, muted, false))
            return
        }
        devices.forEach { d ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                setPadding(14, 12, 14, 12)
                setBackgroundColor(panel)
            }
            card.addView(label(d.name, 16f, Color.WHITE, true))
            card.addView(label("${d.company} • ${d.type}\n${d.host}", 13f, muted, false).apply { setPadding(0, 5, 0, 4) })
            card.addView(actionButton("حذف از فهرست", false) {
                devices.remove(d); saveDevices(); renderDevices()
            })
            deviceList.addView(card, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) })
        }
    }

    private fun actionButton(title: String, primary: Boolean, onClick: () -> Unit): Button =
        Button(this).apply {
            text = title; textSize = 12f; isAllCaps = false
            setTextColor(if (primary) navy else Color.WHITE)
            setBackgroundColor(if (primary) cyan else Color.rgb(38, 62, 91))
            setOnClickListener { onClick() }
        }

    private fun label(text: String, size: Float, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            this.text = text; textSize = size; setTextColor(color)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.RIGHT
        }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
