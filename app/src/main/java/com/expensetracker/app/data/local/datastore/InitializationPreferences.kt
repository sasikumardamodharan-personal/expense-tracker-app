package com.expensetracker.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "initialization_prefs")

@Singleton
class InitializationPreferences @Inject constructor(
    private val context: Context
) {
    private val dataStore = context.dataStore
    
    companion object {
        private val IS_DATABASE_INITIALIZED = booleanPreferencesKey("is_database_initialized")
    }
    
    val isDatabaseInitialized: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_DATABASE_INITIALIZED] ?: false
    }
    
    suspend fun setDatabaseInitialized(initialized: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_DATABASE_INITIALIZED] = initialized
        }
    }
}
