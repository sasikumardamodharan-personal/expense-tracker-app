package com.expensetracker.app

import android.app.Application
import com.expensetracker.app.data.initialization.DatabaseInitializer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ExpenseTrackerApplication : Application() {
    
    @Inject
    lateinit var databaseInitializer: DatabaseInitializer
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize database with default categories on first launch
        applicationScope.launch {
            databaseInitializer.initialize()
        }
    }
}
