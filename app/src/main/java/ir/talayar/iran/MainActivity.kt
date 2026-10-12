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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// ============ رنگ‌ها ============
val Gold = Color(0xFFFFD700)
val BgDark = Color(0xFF1A2030)
val CardBg = Color(0xFF293242)
val CardBg2 = Color(0xFF323D4F)
val BuyGreen = Color(0xFF4ADE80)
val SellRed = Color(0xFFFF6B6B)
val WaitAmber = Color(0xFFFFC94D)
val TextGray = Color(0xFFC7CFDC)
val TextLight = Color(0xFFFFFFFF)

// ============ داده‌ها ============
data class PriceItem(val label: String, val value: String, val change: String)

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
    var lastUpdate by remember { mutableStateOf("") }

    fun doAnalyze() {
        loading = true; error = null
        scope.launch {
            try {
                val r = withContext(Dispatchers.IO) { fetchAndAnalyze() }
                prices = r.first
                analysis = r.second
                val fmt = SimpleDateFormat("HH:mm:ss", Locale("fa"))
                lastUpdate = fmt.format(Date())
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
        Spacer(Modifier.height(12.dp))
        Text("طلا یار ایران", color = Gold, fontSize = 32.sp,
            fontWeight = FontWeight.Bold)
        Text("تحلیل هوشمند طلای ۱۸ عیار", color = TextGray, fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { doAnalyze() },
            enabled = !loading,
            colors = ButtonDefaults.buttonColors(containerColor = Gold),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(color = BgDark,
                    modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text("🔄  تحلیل مجدد", color = BgDark,
                    fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        if (lastUpdate.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text("آخرین به‌روزرسانی: $lastUpdate",
                color = TextGray, fontSize = 11.sp)
        }

        Spacer(Modifier.height(16.dp))

        error?.let {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = SellRed.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("⚠️ خطا", color = SellRed,
                        fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = TextLight, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ---- کارت قیمت‌ها ----
        if (prices.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💰", fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("قیمت‌های لحظه‌ای", color = TextLight,
                            fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(16.dp))

                    prices.forEach { item ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.label, color = TextGray, fontSize = 14.sp)
                            Text(item.value, color = Gold, fontSize = 15.sp,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ---- کارت تحلیل ----
        analysis?.let { a ->
            val dirColor = when (a.direction) {
                "BUY" -> BuyGreen
                "SELL" -> SellRed
                else -> WaitAmber
            }
            val dirLabel = when (a.direction) {
                "BUY" -> "خرید"
                "SELL" -> "فروش"
                else -> "صبر"
            }
            val dirIcon = when (a.direction) {
                "BUY" -> "🟢"
                "SELL" -> "🔴"
                else -> "🟡"
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("وضعیت بازار", color = TextGray, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(dirColor.copy(alpha = 0.18f))
                            .padding(horizontal = 32.dp, vertical = 14.dp)
                    ) {
                        Text("$dirIcon  $dirLabel", color = dirColor,
                            fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    if (a.direction != "WAIT") {
                        Spacer(Modifier.height(12.dp))
                        Text("قدرت سیگنال: ${a.confidence}%",
                            color = dirColor, fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ---- محدوده معامله ----
            if (a.direction != "WAIT") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎯", fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("محدوده معامله", color = TextLight,
                                fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(14.dp))
                        RowLine("Entry", a.entry, Gold)
                        RowLine("Stop Loss", a.sl, SellRed)
                        RowLine("Take Profit", a.tp, BuyGreen)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ---- دلیل تحلیل ----
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📊", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("تحلیل", color = TextLight,
                            fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(a.reason, color = TextGray, fontSize = 14.sp,
                        lineHeight = 22.sp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = WaitAmber.copy(alpha = 0.10f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "⚠️ این پیشنهاد است، نه دستور معامله.\nمسئولیت تصمیم با شماست.",
                color = WaitAmber, fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(14.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun RowLine(label: String, value: String, color: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextGray, fontSize = 14.sp)
        Text(value, color = color, fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold)
    }
}

// ============ دریافت و تحلیل داده ============
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
    val current = json.optJSONObject("current")
        ?: throw Exception("داده‌ای دریافت نشد")

    fun getPrice(key: String): Double? {
        val item = current.optJSONObject(key) ?: return null
        val p = item.optString("p", "")
        if (p.isBlank() || p == "0") return null
        return p.replace(",", "").toDoubleOrNull()
    }

    val gold18 = getPrice("geram18")
    val goldUsed = getPrice("gerami")
    val gold24 = gold18?.let { it* (24.0 / 18.0) } 

    if (gold18 == null && goldUsed == null) {
        throw Exception("قیمت طلا دریافت نشد. اتصال اینترنت را بررسی کنید.")
    }

    val pricesList = listOf(
        PriceItem("طلای ۱۸ عیار (گرم)",
            gold18?.let { formatToman(it) } ?: "---", ""),
        PriceItem("طلای آب‌شده (گرم)",
            goldUsed?.let { formatToman(it) } ?: "---", ""),
        PriceItem("طلای ۲۴ عیار (گرم)",
            gold24?.let { formatToman(it) } ?: "---", ""),
    )

    // ---- تحلیل ----
    val mainPrice = gold18 ?: goldUsed ?: 0.0
    val reasons = ArrayList<String>()
    var score = 0

    // فاصله قیمت ۱۸ عیار با ۲۴ عیار (نسبت باید حدود 0.75 باشه)
    if (gold18 != null && gold24 != null && gold24 > 0) {
        val ratio = gold18 / gold24
        if (ratio in 0.72..0.78) {
            score += 20
            reasons.add("✅ نسبت قیمت ۱۸ به ۲۴ عیار نرمال (${String.format("%.2f", ratio)})")
        } else if (ratio < 0.70) {
            score += 30
            reasons.add("🔥 طلای ۱۸ عیار ارزون‌تر از حد معمول — فرصت خرید")
        } else if (ratio > 0.80) {
            score -= 20
            reasons.add("⚠️ طلای ۱۸ عیار گران‌تر از حد معمول")
        }
    }

    // فاصله ۱۸ عیار با آب‌شده
    if (gold18 != null && goldUsed != null && goldUsed > 0) {
        val diff = (gold18 - goldUsed) / goldUsed * 100
        if (kotlin.math.abs(diff) < 1.0) {
            score += 15
            reasons.add("✅ قیمت طلای ۱۸ عیار نزدیک به آب‌شده")
        } else if (diff > 2.0) {
            score -= 15
            reasons.add("⚠️ طلای ۱۸ عیار از آب‌شده گران‌تره")
        }
    }

    // بررسی روند با میانگین
    reasons.add("📊 قیمت پایه: ${formatToman(mainPrice)}")

    val direction = when {
        score >= 35 -> "BUY"
        score <= -20 -> "SELL"
        else -> "WAIT"
    }
    val confidence = minOf(95, kotlin.math.abs(score) + 45)

    val atr = mainPrice * 0.008
    var entry = "-"; var sl = "-"; var tp = "-"
    if (direction == "BUY") {
        entry = "${formatToman(mainPrice - atr * 0.3)} تا ${formatToman(mainPrice + atr * 0.3)}"
        sl = formatToman(mainPrice - atr * 1.5)
        tp = formatToman(mainPrice + atr * 3.0)
    } else if (direction == "SELL") {
        entry = "${formatToman(mainPrice - atr * 0.3)} تا ${formatToman(mainPrice + atr * 0.3)}"
        sl = formatToman(mainPrice + atr * 1.5)
        tp = formatToman(mainPrice - atr * 3.0)
    }

    return Pair(pricesList, Analysis(
        direction, confidence,
        reasons.joinToString("\n"), entry, sl, tp))
}

fun formatToman(v: Double): String {
    val l =(v / 10).toLong()
    return String.format("%,d تومان", l)
}
