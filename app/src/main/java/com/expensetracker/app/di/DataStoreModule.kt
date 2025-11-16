package com.expensetracker.app.di

import android.content.Context
import com.expensetracker.app.data.local.datastore.InitializationPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    
    @Provides
    @Singleton
    fun provideInitializationPreferences(
        @ApplicationContext context: Context
    ): InitializationPreferences {
        return InitializationPreferences(context)
    }
}
