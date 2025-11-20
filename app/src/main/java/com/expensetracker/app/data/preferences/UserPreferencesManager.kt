package com.expensetracker.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.expensetracker.app.domain.model.Currency
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val CURRENCY = stringPreferencesKey("currency")
    }

    val selectedCurrency: Flow<Currency> = context.dataStore.data.map { preferences ->
        val currencyCode = preferences[PreferencesKeys.CURRENCY] ?: Currency.INR.code
        Currency.fromCode(currencyCode)
    }

    suspend fun setCurrency(currency: Currency) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY] = currency.code
        }
    }
}
