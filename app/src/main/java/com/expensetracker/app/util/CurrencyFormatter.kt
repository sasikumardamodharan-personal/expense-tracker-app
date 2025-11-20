package com.expensetracker.app.util

import com.expensetracker.app.domain.model.Currency
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {
    
    fun format(amount: Double, currency: Currency): String {
        val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        
        val formatter = DecimalFormat("#,##0.00", symbols)
        val formattedAmount = formatter.format(amount)
        
        return when (currency) {
            Currency.INR -> "${currency.symbol}$formattedAmount"
            Currency.USD, Currency.AUD, Currency.CAD -> "${currency.symbol}$formattedAmount"
            Currency.EUR -> "$formattedAmount${currency.symbol}"
            Currency.GBP -> "${currency.symbol}$formattedAmount"
            Currency.JPY -> "${currency.symbol}${DecimalFormat("#,##0").format(amount)}" // No decimals for JPY
            Currency.CHF -> "$formattedAmount ${currency.symbol}"
        }
    }
    
    fun formatWithoutSymbol(amount: Double): String {
        val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        val formatter = DecimalFormat("#,##0.00", symbols)
        return formatter.format(amount)
    }
}
