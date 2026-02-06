package com.expensetracker.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.householdDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "household_preferences"
)

/**
 * Manages household-related preferences using DataStore
 */
@Singleton
class HouseholdPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private object PreferencesKeys {
        val HOUSEHOLD_ID = stringPreferencesKey("household_id")
        val HOUSEHOLD_NAME = stringPreferencesKey("household_name")
        val IS_HOUSEHOLD_SETUP_COMPLETE = booleanPreferencesKey("is_household_setup_complete")
        val MIGRATION_COMPLETE = booleanPreferencesKey("migration_complete")
    }
    
    /**
     * Flow of current household ID
     */
    val householdId: Flow<String?> = context.householdDataStore.data.map { preferences ->
        preferences[PreferencesKeys.HOUSEHOLD_ID]
    }
    
    /**
     * Flow of current household name
     */
    val householdName: Flow<String?> = context.householdDataStore.data.map { preferences ->
        preferences[PreferencesKeys.HOUSEHOLD_NAME]
    }
    
    /**
     * Flow indicating if household setup is complete
     */
    val isHouseholdSetupComplete: Flow<Boolean> = context.householdDataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_HOUSEHOLD_SETUP_COMPLETE] ?: false
    }
    
    /**
     * Flow indicating if data migration to Firestore is complete
     */
    val isMigrationComplete: Flow<Boolean> = context.householdDataStore.data.map { preferences ->
        preferences[PreferencesKeys.MIGRATION_COMPLETE] ?: false
    }
    
    /**
     * Save household information
     */
    suspend fun setHousehold(householdId: String, householdName: String) {
        context.householdDataStore.edit { preferences ->
            preferences[PreferencesKeys.HOUSEHOLD_ID] = householdId
            preferences[PreferencesKeys.HOUSEHOLD_NAME] = householdName
            preferences[PreferencesKeys.IS_HOUSEHOLD_SETUP_COMPLETE] = true
        }
    }
    
    /**
     * Mark household setup as complete
     */
    suspend fun setHouseholdSetupComplete(complete: Boolean) {
        context.householdDataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_HOUSEHOLD_SETUP_COMPLETE] = complete
        }
    }
    
    /**
     * Mark migration as complete
     */
    suspend fun setMigrationComplete(complete: Boolean) {
        context.householdDataStore.edit { preferences ->
            preferences[PreferencesKeys.MIGRATION_COMPLETE] = complete
        }
    }
    
    /**
     * Clear all household preferences (e.g., when leaving household)
     */
    suspend fun clearHouseholdData() {
        context.householdDataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.HOUSEHOLD_ID)
            preferences.remove(PreferencesKeys.HOUSEHOLD_NAME)
            preferences.remove(PreferencesKeys.IS_HOUSEHOLD_SETUP_COMPLETE)
            preferences.remove(PreferencesKeys.MIGRATION_COMPLETE)
        }
    }
    
    /**
     * Get household ID synchronously (for use in non-suspend contexts)
     */
    suspend fun getHouseholdId(): String? {
        var householdId: String? = null
        context.householdDataStore.edit { preferences ->
            householdId = preferences[PreferencesKeys.HOUSEHOLD_ID]
        }
        return householdId
    }
}
