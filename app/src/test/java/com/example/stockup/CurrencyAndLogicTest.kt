package com.example.stockup

import com.example.stockup.utils.CurrencyUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyAndLogicTest {

    @Test
    fun testCurrencyFormatting() {
        assertEquals("R$ 0,00", CurrencyUtils.formatCalculatorPrice(""))
        assertEquals("R$ 0,01", CurrencyUtils.formatCalculatorPrice("1"))
        assertEquals("R$ 0,12", CurrencyUtils.formatCalculatorPrice("12"))
        assertEquals("R$ 1,25", CurrencyUtils.formatCalculatorPrice("125"))
        assertEquals("R$ 12,50", CurrencyUtils.formatCalculatorPrice("1250"))
        assertEquals("R$ 125,00", CurrencyUtils.formatCalculatorPrice("12500"))
        assertEquals("R$ 1.250,00", CurrencyUtils.formatCalculatorPrice("125000"))
    }

    @Test
    fun testParsePriceToDouble() {
        assertEquals(0.0, CurrencyUtils.parseCalculatorPriceToDouble(""), 0.001)
        assertEquals(0.01, CurrencyUtils.parseCalculatorPriceToDouble("1"), 0.001)
        assertEquals(0.12, CurrencyUtils.parseCalculatorPriceToDouble("12"), 0.001)
        assertEquals(12.50, CurrencyUtils.parseCalculatorPriceToDouble("1250"), 0.001)
        assertEquals(34.50, CurrencyUtils.parseCalculatorPriceToDouble("3450"), 0.001)
    }

    @Test
    fun testGramsAndLitresCalculation() {
        val meatTotal = CurrencyUtils.calculateItemTotal("90", "g", 34.50)
        assertEquals(3.105, meatTotal, 0.01)

        val cheeseTotal = CurrencyUtils.calculateItemTotal("500", "g", 40.0)
        assertEquals(20.0, cheeseTotal, 0.01)

        val milkTotal = CurrencyUtils.calculateItemTotal("250", "ml", 8.0)
        assertEquals(2.0, milkTotal, 0.01)

        val riceTotal = CurrencyUtils.calculateItemTotal("2", "kg", 6.0)
        assertEquals(12.0, riceTotal, 0.01)

        val soapTotal = CurrencyUtils.calculateItemTotal("3", "un", 4.50)
        assertEquals(13.50, soapTotal, 0.01)
    }
}
