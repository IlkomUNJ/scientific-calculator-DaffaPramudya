import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Stack
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


//import androidx.compose.runtime.Composable

private const val ERROR = "Error"

// Mapping label tombol ke token yang dipakai parser
private val buttonMap = mapOf(
    "√x" to "√(",
    "xʸ" to "^",
    "sin" to "sin(",
    "cos" to "cos(",
    "tan" to "tan(",
    "asin" to "asin(",
    "acos" to "acos(",
    "atan" to "atan(",
    "ln" to "ln(",
    "log" to "log("
)

@Composable
fun CalculatorScreen() {
    var displayText by remember { mutableStateOf("0") }
    var operationText by remember { mutableStateOf("") }

    val buttons = listOf(
        "sin","cos","tan","ln",
        "asin","acos","atan","log",
        "√x","x!","1/x","xʸ",
        "%","÷","×","-",
        "7","8","9","+",
        "4","5","6","(",
        "1","2","3",")",
        ".","0","+/−","="
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        // Baris ekspresi yang sedang diketik
        Text(
            text = operationText,
            fontSize = 20.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            textAlign = TextAlign.End,
            color = Color.LightGray
        )

        // Display
        Text(
            text = displayText,
            fontSize = 32.sp,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
//                .background(Color(0xFFEEEEEE))
                .padding(16.dp),
            textAlign = TextAlign.End,
            color = Color.LightGray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CalculatorButton(
                "C", Modifier.weight(1f),
                backgroundColor = Color(0xFFFF7970) // merah
            ) {
                operationText = removeLastToken(operationText)
                displayText = operationText
            }
            CalculatorButton(
                "AC", Modifier.weight(1f),
                backgroundColor = Color(0xFFFF7970) // merah
            ) {
                displayText = "0"
                operationText = ""
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val equalSet = "="
        val operatorSet = setOf("+","-","×","÷","=","%","(",")",".","+/−")
        val funcSet = setOf("sin","cos","tan","ln","√x","log","asin","acos","atan","x!","1/x","xʸ")
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(bottom = 8.dp)  // beri jarak kecil
        ) {
            items(buttons) { label ->
                val bgColor = when {
                    label in equalSet -> Color(0xFF009688)
                    label in operatorSet -> Color(0xFFE38602)   // oranye
                    label in funcSet -> Color(0xFF1C7CCB)      // biru
                    else -> Color(0xFFEEEEEE)                  // angka
                }

                val textColor = if (label in operatorSet || label in funcSet) Color.White else Color.Black

                CalculatorButton(
                    label = label,
                    backgroundColor = bgColor,
                    contentColor = textColor
                ) {
                    val (newDisplay, newOperation) = handleButton(label, operationText)
                    displayText = newDisplay
                    operationText = newOperation
                }
            }
        }
    }
}

// Fungsi untuk hapus kata dari fungsi, bukan per huruf
fun removeLastToken(expr: String): String {
    if (expr.isEmpty() || expr == "0") return "0"

    val tokens = tokenize(expr).toMutableList()
    if (tokens.isNotEmpty()) {
        tokens.removeAt(tokens.lastIndex) // ganti removeLast()
    }

    return if (tokens.isEmpty()) "0" else tokens.joinToString("")
}

fun evaluateExpression(expr: String): Double {
    val tokens = tokenize(expr)
    val rpn = infixToPostfix(tokens)
    return evalPostfix(rpn)
}

fun tokenize(expr: String): List<String> {
    val tokens = mutableListOf<String>()
    var current = ""

    var i = 0
    while (i < expr.length) {
        val c = expr[i]

        when {
            c.isDigit() || c == '.' -> {
                current += c
            }

            c.isLetter() || c == '√' -> { // fungsi atau simbol akar
                if (current.isNotEmpty() && (current.last().isLetter() || current.last() == '√')) {
                    current += c
                } else {
                    if (current.isNotEmpty()) {
                        tokens.add(current)
                        current = ""
                    }
                    current += c
                }
            }

            else -> { // operator atau tanda kurung
                if (current.isNotEmpty()) {
                    tokens.add(current)
                    current = ""
                }

                // Cek unary minus
                if (c == '-' && (tokens.isEmpty() || tokens.last() in listOf("+", "-", "×", "÷", "^", "(", "%"))) {
                    // kalau minus di awal atau setelah operator → anggap 0 - ...
                    tokens.add("0")
                }

                tokens.add(c.toString())
            }
        }
        i++
    }
    if (current.isNotEmpty()) tokens.add(current)

    return tokens
}

fun infixToPostfix(tokens: List<String>): List<String> {
    val output = mutableListOf<String>()
    val stack = Stack<String>()
    val precedence = mapOf(
        "+" to 1, "-" to 1,
        "×" to 2, "÷" to 2, "%" to 2,
        "^" to 3,
        "sin" to 4, "cos" to 4, "tan" to 4,
        "asin" to 4, "acos" to 4, "atan" to 4,
        "√" to 4, "ln" to 4, "log" to 4, "!" to 4
    )

    val functions = setOf("sin","cos","tan","asin","acos","atan","ln","log","√","!")

    for (token in tokens) {
        when {
            token.toDoubleOrNull() != null -> output.add(token)

            token in functions -> {
                stack.push(token)
            }

            token in precedence.keys -> {
                while (stack.isNotEmpty() && stack.peek() != "(" &&
                    precedence[stack.peek()]!! >= precedence[token]!!) {
                    output.add(stack.pop())
                }
                stack.push(token)
            }

            token == "(" -> stack.push(token)

            token == ")" -> {
                while (stack.isNotEmpty() && stack.peek() != "(") {
                    output.add(stack.pop())
                }
                if (stack.isNotEmpty() && stack.peek() == "(") stack.pop()

                // cek kalau setelah ")" ada fungsi di atas stack
                if (stack.isNotEmpty() && stack.peek() in functions) {
                    output.add(stack.pop())
                }
            }
        }
    }

    while (stack.isNotEmpty()) output.add(stack.pop())
    return output
}

fun evalPostfix(tokens: List<String>): Double {
    val stack = Stack<Double>()
    for (token in tokens) {
        when {
            token.toDoubleOrNull() != null -> stack.push(token.toDouble())

            token in listOf("+", "-", "×", "÷", "%", "^") -> {
                val b = stack.pop()
                val a = stack.pop()
                val res = when (token) {
                    "+" -> a + b
                    "-" -> a - b
                    "×" -> a * b
                    "÷" -> a / b
                    "%" -> a % b
                    "^" -> Math.pow(a, b)
                    else -> 0.0
                }
                stack.push(res)
            }

            token in listOf("sin", "cos", "tan", "asin", "acos", "atan", "ln", "log", "√", "!") -> {
                val a = stack.pop()
                val res = when (token) {
                    "sin" -> Math.sin(Math.toRadians(a))
                    "cos" -> Math.cos(Math.toRadians(a))
                    "tan" -> {
                        val radians = Math.toRadians(a)
                        val cos = Math.cos(radians)
                        if (Math.abs(cos) < 1e-10) {
                            Double.NaN // agar nanti ditandai undefined oleh checkUndefined
                        } else {
                            Math.tan(radians)
                        }
                    }
                    "asin" -> Math.toDegrees(Math.asin(a))
                    "acos" -> Math.toDegrees(Math.acos(a))
                    "atan" -> Math.toDegrees(Math.atan(a))
                    "ln" -> Math.log(a)
                    "log" -> Math.log10(a)
                    "√" -> Math.sqrt(a)
                    "!" -> factorial(a.toInt())
                    else -> 0.0
                }
                stack.push(res)
            }
        }
    }
    return stack.pop()
}

fun isZero(x: Double, eps: Double = 1e-9) = kotlin.math.abs(x) < eps

fun getUndefinedMessage(func: String, a: Double, b: Double? = null): String? {
    return when (func) {
        "tan" -> {
            val mod = (a - 90) % 180.0
            if (Math.abs(mod) < 1e-9) "tan($a) is undefined" else null
        }
        "sin" -> null
        "cos" -> null
        "asin" -> if (a < -1 || a > 1) "asin($a) domain error" else null
        "acos" -> if (a < -1 || a > 1) "acos($a) domain error" else null
        "atan" -> null
        "ln"   -> if (a <= 0) "ln($a) domain error" else null
        "log"  -> if (a <= 0) "log($a) domain error" else null
        "√"    -> if (a < 0) "sqrt($a) domain error" else null
        "!"    -> {
            if (a < 0) {
                "factorial($a) undefined (negative)"
            } else if (a % 1 != 0.0) {
                "factorial($a) undefined (non-integer)"
            } else null
        }
        "÷" -> if (b != null && isZero(b)) "cannot divide by zero" else null
        "^" -> when {
            b == null -> null
            isZero(a) && b <= 0.0 -> "0^$b is undefined"
            a < 0 && b % 1 != 0.0 -> "$a^$b is not a real number"
            else -> null
        }
        else -> null
    }
}

fun checkUndefined(expr: String, result: Double): String? {
    if (result.isNaN() || result.isInfinite()) return "result is not a real number"

    val patterns = mapOf(
        "tan" to Regex("tan\\(([-0-9.]+)\\)"),
        "sin" to Regex("sin\\(([-0-9.]+)\\)"),
        "cos" to Regex("cos\\(([-0-9.]+)\\)"),
        "asin" to Regex("asin\\(([-0-9.]+)\\)"),
        "acos" to Regex("acos\\(([-0-9.]+)\\)"),
        "atan" to Regex("atan\\(([-0-9.]+)\\)"),
        "ln" to Regex("ln\\(([-0-9.]+)\\)"),
        "log" to Regex("log\\(([-0-9.]+)\\)"),
        "√" to Regex("√\\(([-0-9.]+)\\)"),
        "!" to Regex("([-0-9.]+)!"),
    )

    for ((func, regex) in patterns) {
        regex.findAll(expr).forEach { match ->
            val a = match.groupValues[1].toDouble()
            val msg = getUndefinedMessage(func, a)
            if (msg != null) return msg
        }
    }

    // Bagi dua bilangan langsung
    val divRegex = Regex("([-0-9.]+)÷([-0-9.]+)")
    divRegex.findAll(expr).forEach {
        val a = it.groupValues[1].toDouble()
        val b = it.groupValues[2].toDouble()
        val msg = getUndefinedMessage("÷", a, b)
        if (msg != null) return msg
    }

    // Bagi ekspresi dalam kurung, contoh: 1÷(cos(90))
    val divFuncRegex = Regex("([-0-9.]+)÷\\((.+)\\)")
    divFuncRegex.findAll(expr).forEach {
        val a = it.groupValues[1].toDouble()
        val bExpr = it.groupValues[2]
        val b = evaluateExpression(bExpr)
        val msg = getUndefinedMessage("÷", a, b)
        if (msg != null) return msg
    }

    // Pangkat
    val powRegex = Regex("([-0-9.]+)\\^([-0-9.]+)")
    powRegex.findAll(expr).forEach {
        val a = it.groupValues[1].toDouble()
        val b = it.groupValues[2].toDouble()
        val msg = getUndefinedMessage("^", a, b)
        if (msg != null) return msg
    }

    return null
}

fun formatResult(value: Double): String {
    // kalau hasil sangat kecil, anggap nol
    if (Math.abs(value) < 1e-10) return "0"

    // tampilkan max 15 digit desimal, lalu rapikan nol di belakang
    return String.format("%.15f", value)
        .trimEnd('0')
        .trimEnd('.')
}

fun factorial(n: Int): Double {
    return if (n <= 1) 1.0 else n * factorial(n - 1)
}

fun handleButton(label: String, current: String): Pair<String, String> {
    return when (label) {
        "=" -> {
            try {
                val result = evaluateExpression(current)
                val msg = checkUndefined(current, result)
                if (msg != null) {
                    Pair(msg, current)   // display pesan error, history tetap
                } else {
                    Pair(formatResult(result), current) // display hasil, history tetap
                }
            } catch (e: Exception) {
                Pair(ERROR, current)
            }
        }

        "+/−" -> {
            val newOp = if (current.startsWith("-")) {
                current.removePrefix("-")
            } else {
                "-$current"
            }
            Pair(newOp, newOp)
        }

        "1/x" -> {
            if (current.startsWith("1÷(") && current.endsWith(")")) {
                try {
                    val result = evaluateExpression(current)
                    val msg = checkUndefined(current, result)
                    if (msg != null) Pair(msg, current) else Pair(formatResult(result), current)
                } catch (e: Exception) {
                    Pair(ERROR, current)
                }
            } else {
                val newOp = "1÷($current)"
                Pair(newOp, newOp) // display & history ikut berubah
            }
        }

        else -> {
            val newToken = buttonMap[label] ?: label
            val newOp = if (current == "0") newToken else current + newToken
            Pair(newOp, newOp)
        }
    }
}

@Composable
fun CalculatorButton(
    label: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFFEEEEEE),
    contentColor: Color = Color.Black,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.elevatedButtonElevation(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 16.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CalculatorPreview() {
    CalculatorScreen()
}