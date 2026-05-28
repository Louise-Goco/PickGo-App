package com.example.pickgo.utils

import java.text.NumberFormat
import java.util.Locale

object PriceFormatter {
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

    init {
        currencyFormat.minimumFractionDigits = 2
        currencyFormat.maximumFractionDigits = 2
    }

    fun format(amount: Double): String {
        return currencyFormat.format(amount)
    }

    fun formatInt(amount: Int): String {
        return currencyFormat.format(amount.toDouble())
    }
}