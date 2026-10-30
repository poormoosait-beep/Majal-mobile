package com.majal.mobile

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
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
    private val prefs by lazy { getSharedPreferences("majal_companies", MODE_PRIVATE) }
    private val companies = mutableListOf<Company>()
    private var pendingCompanyId: String? = null
    private lateinit var listContainer: LinearLayout
    private lateinit var statusLabel: TextView

    data class Company(val id: String, val name: String, var profileUri: String = "", var imported: Boolean = false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = navy
        window.navigationBarColor = navy
        window.decorView.setBackgroundColor(navy)
        loadCompanies()

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(22, 32, 22, 24)
            setBackgroundColor(navy)
        }
        page.addView(label("MAJAL MOBILE", 14f, cyan, true))
        page.addView(label("شبکه گستر مجال", 27f, Color.WHITE, true).apply { setPadding(0, 8, 0, 4) })
        page.addView(label("مدیریت شرکت‌ها و اتصال امن OpenVPN", 14f, muted, false))
        statusLabel = label("وضعیت: برای اتصال، OpenVPN for Android باید نصب باشد.", 13f, cyan, false)
        statusLabel.setPadding(0, 18, 0, 12)
        page.addView(statusLabel)

        page.addView(label("افزودن شرکت", 17f, Color.WHITE, true).apply { setPadding(0, 12, 0, 8) })
        val companyName = EditText(this).apply {
            hint = "نام شرکت را وارد کنید"
            textSize = 15f
            setTextColor(Color.WHITE)
            setHintTextColor(muted)
            setSingleLine(true)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(14, 12, 14, 12)
            setBackgroundColor(panel)
        }
        page.addView(companyName)
        page.addView(actionButton("ثبت شرکت", true) {
            val name = companyName.text.toString().trim()
            if (name.isBlank()) toast("نام شرکت را وارد کن")
            else if (companies.any { it.name.equals(name, true) }) toast("این شرکت قبلاً ثبت شده است")
            else {
                companies.add(Company(System.currentTimeMillis().toString(), name))
                companyName.text.clear()
                saveCompanies()
                renderCompanies()
            }
        })

        page.addView(label("شرکت‌های من", 18f, Color.WHITE, true).apply { setPadding(0, 22, 0, 8) })
        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        page.addView(listContainer)
        page.addView(label("روش کار VPN", 16f, cyan, true).apply { setPadding(0, 22, 0, 8) })
        page.addView(label(
            "۱) برای هر شرکت فایل .ovpn را وارد کن.\n۲) فایل به OpenVPN for Android فرستاده می‌شود تا آنجا Import شود.\n۳) نام پروفایل داخل OpenVPN باید دقیقاً با نام شرکت یکی باشد.\n۴) سپس از همین صفحه اتصال یا قطع را بزن. نام کاربری/رمز یا گواهی در OpenVPN مدیریت می‌شود.",
            13f, muted, false
        ))
        page.addView(label("پشتیبانی شبکه گستر مجال • 09386066008", 13f, cyan, true).apply { setPadding(0, 26, 0, 12) })
        setContentView(ScrollView(this).apply { addView(page) })
        renderCompanies()
    }

    private fun loadCompanies() {
        companies.clear()
        try {
            val arr = JSONArray(prefs.getString("items", "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                companies.add(Company(o.optString("id"), o.optString("name"), o.optString("profileUri"), o.optBoolean("imported", false)))
            }
        } catch (_: Exception) { companies.clear() }
    }

    private fun saveCompanies() {
        val arr = JSONArray()
        companies.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id); put("name", c.name); put("profileUri", c.profileUri); put("imported", c.imported)
            })
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }

    private fun renderCompanies() {
        if (!::listContainer.isInitialized) return
        listContainer.removeAllViews()
        if (companies.isEmpty()) {
            listContainer.addView(label("هنوز شرکتی ثبت نشده است.", 13f, muted, false))
            return
        }
        companies.forEach { company ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                setPadding(16, 14, 16, 14)
                setBackgroundColor(panel)
            }
            card.addView(label(company.name, 17f, Color.WHITE, true))
            card.addView(label(
                if (company.imported) "پروفایل برای این شرکت علامت‌گذاری شده؛ اتصال را تست کن."
                else "فایل VPN هنوز برای این شرکت وارد نشده است.",
                12f, muted, false
            ).apply { setPadding(0, 6, 0, 10) })
            card.addView(actionButton("انتخاب و Import فایل .ovpn", false) {
                pendingCompanyId = company.id
                val pick = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/x-openvpn-profile", "application/octet-stream", "text/plain"))
                }
                startActivityForResult(pick, REQUEST_PROFILE)
            })
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
            row.addView(actionButton("اتصال", true) { connect(company) },
                LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(5) })
            row.addView(actionButton("قطع اتصال", false) { disconnect() },
                LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(5) })
            card.addView(row)
            card.addView(actionButton("حذف شرکت", false) {
                companies.remove(company); saveCompanies(); renderCompanies()
            })
            listContainer.addView(card, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) })
        }
    }

    private fun connect(company: Company) {
        if (!company.imported) { toast("ابتدا فایل این شرکت را Import کن"); return }
        if (!isOpenVpnInstalled()) { toast("ابتدا OpenVPN for Android را نصب کن"); return }
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                setClassName("de.blinkt.openvpn", "de.blinkt.openvpn.api.ConnectVPN")
                putExtra("de.blinkt.openvpn.api.profileName", company.name)
            }
            startActivity(intent)
            statusLabel.text = "درخواست اتصال برای ${company.name} ارسال شد؛ وضعیت نهایی را در اعلان VPN بررسی کن."
        } catch (_: ActivityNotFoundException) {
            toast("بخش اتصال OpenVPN پیدا نشد؛ برنامه OpenVPN را به‌روز کن")
        } catch (e: Exception) {
            toast("ارسال درخواست اتصال ناموفق بود: ${e.message ?: "خطای نامشخص"}")
        }
    }

    private fun disconnect() {
        if (!isOpenVpnInstalled()) { toast("OpenVPN for Android نصب نیست"); return }
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                setClassName("de.blinkt.openvpn", "de.blinkt.openvpn.api.DisconnectVPN")
            }
            startActivity(intent)
            statusLabel.text = "درخواست قطع اتصال به OpenVPN ارسال شد."
        } catch (_: Exception) { toast("ارسال درخواست قطع اتصال انجام نشد") }
    }

    private fun isOpenVpnInstalled(): Boolean = try {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo("de.blinkt.openvpn", 0)
        true
    } catch (_: Exception) { false }

    @Deprecated("Legacy document picker callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_PROFILE || resultCode != RESULT_OK) return
        val uri: Uri = data?.data ?: return
        val company = companies.firstOrNull { it.id == pendingCompanyId } ?: return
        try {
            val readFlag = data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, readFlag)
        } catch (_: Exception) { }
        company.profileUri = uri.toString()
        try {
            val open = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/x-openvpn-profile")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("de.blinkt.openvpn")
            }
            startActivity(open)
            toast("فایل به OpenVPN فرستاده شد. آنجا Import را کامل کن و نام پروفایل را دقیقاً «${company.name}» بگذار.")
        } catch (_: Exception) {
            try {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "application/x-openvpn-profile"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(share, "ارسال پروفایل به OpenVPN"))
                toast("OpenVPN را از فهرست انتخاب کن و Import را کامل کن.")
            } catch (_: Exception) { toast("ارسال فایل انجام نشد. OpenVPN for Android را نصب کن.") }
        }
        // This is only a user acknowledgement; the VPN app remains the source of truth for connection state.
        company.imported = true
        saveCompanies()
        renderCompanies()
        statusLabel.text = "بعد از Import در OpenVPN، اتصال شرکت را امتحان کن."
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

    companion object { private const val REQUEST_PROFILE = 5101 }
}
