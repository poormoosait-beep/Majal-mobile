package com.majal.mobile

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.concurrent.Executors

class RouterManagerActivity : Activity() {
    private val io = Executors.newSingleThreadExecutor()
    private lateinit var host: EditText
    private lateinit var port: EditText
    private lateinit var user: EditText
    private lateinit var pass: EditText
    private lateinit var interfaceName: EditText
    private lateinit var output: TextView
    private lateinit var tls: CheckBox
    private val prefs by lazy {
        val key = MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(this, "majal_router_secure", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xff081327.toInt()
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(22, 24, 22, 24)
            setBackgroundColor(0xff081327.toInt())
        }
        page.addView(text("مدیریت MikroTik • MAJAL", 23f, 0xff22d3ee.toInt(), true))
        page.addView(text("از VPN فعلی گوشی استفاده می‌شود؛ هیچ پورت عمومی جدیدی لازم نیست.", 13f, -1, false))
        host = field("IP یا نام میزبان روتر", prefs.getString("host", "") ?: "")
        port = field("پورت API (معمولاً 8729 برای API-SSL)", prefs.getString("port", "8729") ?: "8729")
        user = field("نام کاربری API", prefs.getString("user", "") ?: "")
        pass = field("رمز عبور API", prefs.getString("pass", "") ?: "", true)
        tls = CheckBox(this).apply { text = "استفاده از API-SSL (پیشنهادی)"; setTextColor(-1); isChecked = prefs.getBoolean("tls", true) }
        interfaceName = field("نام دقیق اینترفیس برای فعال/غیرفعال‌سازی", "")
        page.addView(host); page.addView(port); page.addView(user); page.addView(pass); page.addView(tls)
        page.addView(button("ذخیره امن مشخصات") { save() })
        page.addView(button("تست اتصال و نمایش منابع") { runApi { api ->
            val rows = api.command("/system/resource/print")
            "منابع روتر:\n" + rows.joinToString("\n") { it.entries.joinToString(" | ") { e -> "${e.key}: ${e.value}" } }
        } })
        page.addView(button("نمایش اینترفیس‌ها") { runApi { api ->
            val rows = api.command("/interface/print")
            rows.joinToString("\n") { r -> "${r["name"] ?: "?"} | ${r["type"] ?: ""} | ${r["running"] ?: ""} | disabled=${r["disabled"] ?: ""}" }.ifBlank { "اینترفیس ثبت‌شده‌ای برنگشت." }
        } })
        page.addView(interfaceName)
        page.addView(button("فعال‌سازی اینترفیس") { confirm("اینترفیس فعال شود؟") { runApi { it.command("/interface/enable", "=numbers=${interfaceName.text.toString().trim()}"); "درخواست فعال‌سازی ارسال شد." } } })
        page.addView(button("غیرفعال‌سازی اینترفیس") { confirm("با غیرفعال‌سازی ممکن است ارتباط VPN یا شبکه قطع شود. ادامه می‌دهی؟") { runApi { it.command("/interface/disable", "=numbers=${interfaceName.text.toString().trim()}"); "درخواست غیرفعال‌سازی ارسال شد." } } })
        page.addView(button("راه‌اندازی مجدد روتر") { confirm("روتر راه‌اندازی مجدد شود؟ همه کاربران شبکه موقتاً قطع می‌شوند.") { runApi { it.command("/system/reboot"); "درخواست راه‌اندازی مجدد ارسال شد." } } })
        output = text("نتیجه عملیات در این قسمت نمایش داده می‌شود.", 13f, -1, false)
        output.setPadding(0, 18, 0, 20)
        page.addView(output)
        setContentView(ScrollView(this).apply { addView(page) })
    }

    private fun save() {
        prefs.edit().putString("host", host.text.toString().trim())
            .putString("port", port.text.toString().trim()).putString("user", user.text.toString().trim())
            .putString("pass", pass.text.toString()).putBoolean("tls", tls.isChecked).apply()
        show("مشخصات در حافظه رمزگذاری‌شده گوشی ذخیره شد.")
    }

    private fun runApi(action: (RouterOsApi) -> String) {
        save()
        val h = host.text.toString().trim()
        val p = port.text.toString().toIntOrNull() ?: 8729
        val u = user.text.toString().trim()
        val pw = pass.text.toString()
        val secure = tls.isChecked
        if (h.isBlank() || u.isBlank() || pw.isBlank()) { show("IP، نام کاربری و رمز را کامل کن."); return }
        output.text = "در حال ارتباط با روتر..."
        io.execute {
            val result = try { RouterOsApi(h, p, u, pw, secure).use { action(it) } }
            catch (e: Exception) { "خطا: ${e.message ?: "ارتباط برقرار نشد"}" }
            runOnUiThread { output.text = result }
        }
    }

    private fun confirm(message: String, action: () -> Unit) {
        AlertDialog.Builder(this).setTitle("تأیید عملیات").setMessage(message)
            .setNegativeButton("انصراف", null).setPositiveButton("تأیید") { _, _ -> action() }.show()
    }
    private fun field(hintText: String, value: String, secret: Boolean = false) = EditText(this).apply {
        hint = hintText; setText(value); setTextColor(-1); setHintTextColor(0xffa7bed8.toInt())
        textSize = 14f; setSingleLine(true); layoutDirection = View.LAYOUT_DIRECTION_LTR
        if (secret) inputType = 129
        setPadding(12, 8, 12, 8)
    }
    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; setOnClickListener { action() }
    }
    private fun text(value: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); gravity = Gravity.RIGHT
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }
    private fun show(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    override fun onDestroy() { super.onDestroy(); io.shutdownNow() }
}
