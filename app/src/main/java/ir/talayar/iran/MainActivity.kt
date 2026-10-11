package ir.talayar.iran

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

val Gold = Color(0xFFD4AF37)
val BgDark = Color(0xFF0E1116)
val CardBg = Color(0xFF161B22)
val BuyGreen = Color(0xFF16A34A)
val SellRed = Color(0xFFDC2626)
val WaitAmber = Color(0xFFF59E0B)
val TextGray = Color(0xFF8B96A8)
val TextLight = Color(0xFFE6EAF2)

data class PriceItem(val label: String, val value: String)

data class Analysis(
    val direction: String,
    val confidence: Int,
    val reason: String,
    val entry: String,
    val sl: String,
    val tp: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme { AppScreen() }
        }
    }
}

@Composable
fun AppScreen() {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var prices by remember { mutableStateOf<List<PriceItem>>(emptyList()) }
    var analysis by remember { mutableStateOf<Analysis?>(null) }

    fun doAnalyze() {
        loading = true; error = null
        scope.launch {
            try {
                val r = withContext(Dispatchers.IO) { fetchAndAnalyze() }
                prices = r.first; analysis = r.second
            } catch (e: Exception) {
                error = e.message ?: "خطای ناشناخته"
            } finally { loading = false }
        }
    }

    LaunchedEffect(Unit) { doAnalyze() }

    Column(
        modifier = Modifier.fillMaxSize().background(BgDark)
            .verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Text("طلا یار ایران", color = Gold, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("تحلیل طلای ۱۸ و آب‌شده", color = TextGray, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { doAnalyze() },
            enabled = !loading,
            colors = ButtonDefaults.buttonColors(containerColor = Gold),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (loading) CircularProgressIndicator(color = BgDark,
                modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            else Text("تحلیل مجدد", color = BgDark, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))

        error?.let {
            Card(
                colors = CardDefaults.cardColors(containerColor = SellRed.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("خطا", color = SellRed, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = TextLight, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (prices.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("قیمت‌های لحظه‌ای", color = TextLight,
                        fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    prices.forEach { item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.label, color = TextGray, fontSize = 14.sp)
                            Text(item.value, color = Gold, fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        analysis?.let { a ->
            val dirColor = when (a.direction) {
                "BUY" -> BuyGreen; "SELL" -> SellRed; else -> WaitAmber
            }
            val dirLabel = when (a.direction) {
                "BUY" -> "خرید"; "SELL" -> "فروش"; else -> "صبر"
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("وضعیت", color = TextGray, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.clip(RoundedCornerShape(12.dp))
                        .background(dirColor.copy(alpha = 0.15f))
                        .padding(horizontal = 24.dp, vertical = 12.dp)) {
                        Text(dirLabel, color = dirColor, fontSize = 26.sp,
                            fontWeight = FontWeight.Bold)
                    }
                    if (a.direction != "WAIT") {
                        Spacer(Modifier.height(8.dp))
                        Text("قدرت سیگنال: ${a.confidence}%", color = dirColor,
                            fontSize = 14.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            if (a.direction != "WAIT") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("محدوده معامله", color = TextLight,
                            fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        RowLine("Entry", a.entry, Gold)
                        RowLine("Stop Loss", a.sl, SellRed)
                        RowLine("Take Profit", a.tp, BuyGreen)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("تحلیل", color = TextLight,
                        fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(a.reason, color = TextGray, fontSize = 14.sp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("این پیشنهاد است، نه دستور معامله.\nمسئولیت تصمیم با شماست.",
            color = TextGray, fontSize = 11.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun RowLine(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextGray, fontSize = 14.sp)
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

fun fetchAndAnalyze(): Pair<List<PriceItem>, Analysis> {
    val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    val request = Request.Builder()
        .url("https://call1.tgju.org/ajax.json")
        .header("User-Agent", "Mozilla/5.0 (Linux; Android 13)")
        .header("Referer", "https://www.tgju.org/")
        .build()

    val response = client.newCall(request).execute()
    val body = response.body?.string() ?: throw Exception("پاسخ خالی از سرور")
    if (!response.isSuccessful) throw Exception("خطای شبکه: " + response.code)

    val json = JSONObject(body)
    val current = json.optJSONObject("current") ?: throw Exception("داده‌ای دریافت نشد")

    fun getPrice(key: String): Double? {
        val item = current.optJSONObject(key) ?: return null
        val p = item.optString("p", "")
        return p.replace(",", "").toDoubleOrNull()
    }

    val gold18 = getPrice("geram18")
    val goldUsed = getPrice("gerami")
    val gold24 = getPrice("geram24")
    val mesghal = getPrice("mesghal")
    val coin = getPrice("sekke_emami")
    val dollar = getPrice("price_dollar_rl")
    val ounce = getPrice("gold")

    val pricesList = listOf(
        PriceItem("طلای ۱۸ عیار", gold18?.let { formatToman(it) } ?: "---"),
        PriceItem("طلای آب‌شده", goldUsed?.let { formatToman(it) } ?: "---"),
        PriceItem("طلای ۲۴ عیار", gold24?.let { formatToman(it) } ?: "---"),
        PriceItem("مثقال طلا", mesghal?.let { formatToman(it) } ?: "---"),
        PriceItem("سکه امامی", coin?.let { formatToman(it) } ?: "---"),
        PriceItem("دلار آزاد", dollar?.let { formatToman(it) } ?: "---"),
        PriceItem("اونس جهانی", ounce?.let { "$" + String.format("%.2f", it) } ?: "---"),
    )

    val mainPrice = gold18 ?: goldUsed ?: 0.0
    if (mainPrice <= 0.0) throw Exception("قیمت طلا دریافت نشد")

    val reasons = ArrayList<String>()
    var score = 0

    if (dollar != null && ounce != null) {
        val theoretical = (ounce * dollar / 31.1) * (18.0 / 24.0)
        val bubble = mainPrice - theoretical
        val bubblePct = bubble / theoretical * 100

        reasons.add("حباب طلای ۱۸: ${String.format("%.1f", bubblePct)}%")

        when {
            bubblePct < 3 -> { score += 30; reasons.add("حباب پایین - فرصت خرید") }
            bubblePct > 10 -> { score -= 30; reasons.add("حباب بالا - خطر اصلاح") }
            else -> { reasons.add("حباب در محدوده نرمال") }
        }
    }

    val ratio = if (goldUsed != null && goldUsed > 0)
        (mainPrice - goldUsed) / goldUsed * 100 else 0.0
    if (ratio > 0.5) {
        score += 15
        reasons.add("اختلاف طلای ۱۸ با آب‌شده بالا")
    }

    val direction = when {
        score >= 40 -> "BUY"
        score <= -40 -> "SELL"
        else -> "WAIT"
    }
    val confidence = minOf(95, kotlin.math.abs(score) + 40)

    val atr = mainPrice * 0.01
    var entry = "-"; var sl = "-"; var tp = "-"
    if (direction == "BUY") {
        entry = formatToman(mainPrice - atr * 0.3) + " تا " + formatToman(mainPrice + atr * 0.3)
        sl = formatToman(mainPrice - atr * 1.5)
        tp = formatToman(mainPrice + atr * 3.0)
    } else if (direction == "SELL") {
        entry = formatToman(mainPrice - atr * 0.3) + " تا " + formatToman(mainPrice + atr * 0.3)
        sl = formatToman(mainPrice + atr * 1.5)
        tp = formatToman(mainPrice - atr * 3.0)
    }

    return Pair(pricesList, Analysis(direction, confidence,
        reasons.joinToString("\n"), entry, sl, tp))
}

fun formatToman(v: Double): String {
    val l = v.toLong()
    return String.format("%,d تومان", l)
}
