package com.example.stockup.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.util.Locale

class CurrencyAmountInputVisualTransformation(
    private val zeroPrefix: Boolean = true
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val cleanDigits = text.text.filter { it.isDigit() }
        val formatted = if (cleanDigits.isEmpty()) {
            if (zeroPrefix) "R$ 0,00" else ""
        } else {
            val cents = cleanDigits.toLongOrNull() ?: 0L
            val reais = cents / 100
            val centavos = cents % 100
            val reaisFormatted = String.format(Locale("pt", "BR"), "%,d", reais)
            "R$ $reaisFormatted," + String.format("%02d", centavos)
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return formatted.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                return text.length
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

object CurrencyUtils {

    fun formatCalculatorPrice(rawInput: String): String {
        val cleanDigits = rawInput.filter { it.isDigit() }
        if (cleanDigits.isEmpty()) return "R$ 0,00"
        
        val safeDigits = if (cleanDigits.length > 10) cleanDigits.take(10) else cleanDigits
        val cents = safeDigits.toLongOrNull() ?: 0L
        val reais = cents / 100
        val centavos = cents % 100
        val reaisFormatted = String.format(Locale("pt", "BR"), "%,d", reais)
        return "R$ $reaisFormatted," + String.format("%02d", centavos)
    }

    fun parseCalculatorPriceToDouble(rawInput: String): Double {
        val cleanDigits = rawInput.filter { it.isDigit() }
        if (cleanDigits.isEmpty()) return 0.0
        val safeDigits = if (cleanDigits.length > 10) cleanDigits.take(10) else cleanDigits
        val cents = safeDigits.toLongOrNull() ?: 0L
        return cents / 100.0
    }

    fun doubleToCalculatorDigits(value: Double?): String {
        if (value == null || value <= 0.0) return ""
        val cents = Math.round(value * 100)
        return cents.toString()
    }

    /**
     * Lê a quantidade digitada pelo usuário.
     *
     * O teclado decimal do Android em pt-BR digita VÍRGULA. Antes, cada leitor fazia
     * `quantity.toDoubleOrNull() ?: 1.0`: "1,5" virava null e era silenciosamente
     * tratado como 1.0 — o botão "+" gravava "2" e o alerta de estoque mínimo
     * comparava o número errado. Aqui a vírgula é normalizada e valores não finitos
     * (NaN/Infinity) são rejeitados.
     */
    fun parseQuantity(raw: String, fallback: Double = 1.0): Double =
        raw.trim()
            .replace(",", ".")
            .toDoubleOrNull()
            ?.takeIf { it.isFinite() }
            ?: fallback

    /**
     * Escreve a quantidade de volta no formato que o app usa: inteiro sem casas
     * ("3") e fração com vírgula ("1,5").
     *
     * Existe para o desconto de 1 na pergunta do dia não gravar "2.0" na despensa.
     */
    fun formatarQuantidade(valor: Double): String =
        if (valor % 1.0 == 0.0) valor.toInt().toString()
        else String.format(Locale("pt", "BR"), "%.2f", valor).trimEnd('0').trimEnd(',')

    fun calculateItemTotal(quantityStr: String, unit: String, price: Double?): Double {
        if (price == null || price <= 0.0) return 0.0
        val qty = parseQuantity(quantityStr)
        return when (unit.lowercase().trim()) {
            "g" -> (qty / 1000.0) * price
            "ml" -> (qty / 1000.0) * price
            else -> qty * price
        }
    }
}
