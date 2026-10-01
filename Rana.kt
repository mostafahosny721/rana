package app.rana

import android.animation.*
import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile
import android.view.Gravity
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject



data class Entry(val num: String, val name: String, val emoji: String, val anim: Int, val label: String)

object Calls { var call: android.telecom.Call? = null }

object Store {
    val FREE = listOf("😍", "😎", "🤓", "🤪", "🐣")
    val PRO = listOf("👑", "🐉", "🦄", "🚀", "🎧", "🦊", "🔮", "🌈", "🦁", "🎸", "💎", "🧙")
    val ANIMS = listOf("نطّ", "هزّ", "نبض", "لفّ", "طيران")

    private fun p(c: Context) = c.getSharedPreferences("rana", 0)
    fun isPro(c: Context) = p(c).getBoolean("pro", false)
    fun setPro(c: Context, v: Boolean) = p(c).edit().putBoolean("pro", v).apply()
    fun key(n: String) = n.filter { it.isDigit() }.takeLast(9)

    fun all(c: Context): List<Entry> {
        val a = JSONArray(p(c).getString("m", "[]"))
        return List(a.length()) {
            val o = a.getJSONObject(it)
            Entry(o.getString("n"), o.getString("nm"), o.getString("e"), o.getInt("a"), o.getString("l"))
        }
    }

    fun put(c: Context, e: Entry) {
        val a = JSONArray()
        (all(c).filter { key(it.num) != key(e.num) } + e).forEach {
            a.put(JSONObject().put("n", it.num).put("nm", it.name).put("e", it.emoji).put("a", it.anim).put("l", it.label))
        }
        p(c).edit().putString("m", a.toString()).apply()
    }

    fun find(c: Context, num: String?) = all(c).firstOrNull { key(it.num) == key(num ?: "") && key(it.num).isNotEmpty() }
}

fun View.play(kind: Int) {
    val a: ObjectAnimator = when (kind) {
        0 -> ObjectAnimator.ofFloat(this, "translationY", 0f, -50f).apply { duration = 350 }
        1 -> ObjectAnimator.ofFloat(this, "rotation", -14f, 14f).apply { duration = 250 }
        2 -> ObjectAnimator.ofPropertyValuesHolder(this,
            PropertyValuesHolder.ofFloat("scaleX", 1f, 1.25f),
            PropertyValuesHolder.ofFloat("scaleY", 1f, 1.25f)).apply { duration = 450 }
        3 -> ObjectAnimator.ofFloat(this, "rotation", 0f, 360f).apply { duration = 2200; interpolator = LinearInterpolator() }
        else -> ObjectAnimator.ofFloat(this, "translationX", -30f, 30f).apply { duration = 1200 }
    }
    a.repeatCount = ValueAnimator.INFINITE
    a.repeatMode = if (kind == 3) ValueAnimator.RESTART else ValueAnimator.REVERSE
    a.start()
}



class CallService : InCallService() {
    override fun onCallAdded(call: Call) {
        Calls.call = call
        startActivity(Intent(this, CallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    override fun onCallRemoved(call: Call) {
        if (Calls.call == call) Calls.call = null
    }
}



class CallActivity : Activity() {
    private val cb = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) { draw() }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setShowWhenLocked(true); setTurnScreenOn(true)
        Calls.call?.registerCallback(cb)
        draw()
    }

    override fun onDestroy() { Calls.call?.unregisterCallback(cb); super.onDestroy() }

    private fun tv(t: String, sp: Float, col: Int = Color.WHITE) =
        TextView(this).apply { text = t; textSize = sp; setTextColor(col); gravity = Gravity.CENTER }

    private fun round(t: String, col: Int, f: () -> Unit) =
        Button(this).apply { text = t; textSize = 22f; setTextColor(Color.WHITE); setBackgroundColor(col); setOnClickListener { f() } }

    private fun draw() {
        val call = Calls.call
        if (call == null || call.state == Call.STATE_DISCONNECTED || call.state == Call.STATE_DISCONNECTING) { finish(); return }
        val num = call.details.handle?.schemeSpecificPart
        val e = Store.find(this, num)
        val emo = tv(e?.emoji ?: "📞", 96f)
        if (e != null) emo.play(e.anim)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setBackgroundColor(0xFF241F3D.toInt()); setPadding(40, 40, 40, 40)
        }
        root.addView(emo)
        root.addView(tv(e?.label ?: (num ?: "رقم غير معروف"), 30f))
        root.addView(tv(if (call.state == Call.STATE_RINGING) "مكالمة واردة" else "المكالمة شغالة", 16f, 0xFFA9A3C6.toInt()))
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER; setPadding(0, 80, 0, 0) }
        if (call.state == Call.STATE_RINGING) {
            row.addView(round("✕", 0xFFE5484D.toInt()) { call.reject(false, null) })
            row.addView(round("✆", 0xFF2FB765.toInt()) { call.answer(VideoProfile.STATE_AUDIO_ONLY) })
        } else {
            row.addView(round("إنهاء", 0xFFE5484D.toInt()) { call.disconnect() })
        }
        root.addView(row)
        setContentView(root)
    }
}



class MainActivity : Activity() {
    private lateinit var box: LinearLayout

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 80, 40, 40) }
        setContentView(ScrollView(this).apply { addView(box) })
        render()
    }

    private fun btn(t: String, f: () -> Unit) = Button(this).apply { text = t; setOnClickListener { f() } }

    private fun render() {
        box.removeAllViews()
        box.addView(TextView(this).apply { text = "رنّة"; textSize = 30f })
        box.addView(btn("1) خليه تطبيق الاتصال الافتراضي") {
            val rm = getSystemService(RoleManager::class.java)
            startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER), 1)
        })
        box.addView(btn("2) ضيف جهة اتصال") {
            startActivityForResult(Intent(Intent.ACTION_PICK, Phone.CONTENT_URI), 2)
        })
        box.addView(btn(if (Store.isPro(this)) "باقة النجوم مفعّلة ✓" else "افتح باقة النجوم (تجريبي)") {
            Store.setPro(this, true); render()
        })
        Store.all(this).forEach { e -> box.addView(btn("${e.emoji}  ${e.label.ifBlank { e.name }}") { pickEmoji(e) }) }
    }

    override fun onActivityResult(r: Int, c: Int, d: Intent?) {
        super.onActivityResult(r, c, d)
        val uri = d?.data
        if (r == 2 && c == RESULT_OK && uri != null) {
            contentResolver.query(uri, arrayOf(Phone.NUMBER, Phone.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) pickEmoji(Entry(it.getString(0), it.getString(1), "😍", 0, it.getString(1)))
            }
        }
    }

    private fun pickEmoji(e: Entry) {
        val pro = Store.isPro(this)
        val all = Store.FREE + Store.PRO
        val names = all.mapIndexed { i, x -> if (i >= Store.FREE.size && !pro) "$x 🔒" else x }
        AlertDialog.Builder(this).setTitle("اختار الإيموجي").setItems(names.toTypedArray()) { _, i ->
            if (i >= Store.FREE.size && !pro) {
                Toast.makeText(this, "الإيموجي ده في باقة النجوم", Toast.LENGTH_SHORT).show()
            } else pickAnim(e.copy(emoji = all[i]))
        }.show()
    }

    private fun pickAnim(e: Entry) {
        AlertDialog.Builder(this).setTitle("اختار الحركة").setItems(Store.ANIMS.toTypedArray()) { _, i ->
            pickLabel(e.copy(anim = i))
        }.show()
    }

    private fun pickLabel(e: Entry) {
        val et = EditText(this).apply { setText(e.label) }
        AlertDialog.Builder(this).setTitle("النص تحت الإيموجي").setView(et)
            .setPositiveButton("حفظ") { _, _ -> Store.put(this, e.copy(label = et.text.toString())); render() }.show()
    }
}

