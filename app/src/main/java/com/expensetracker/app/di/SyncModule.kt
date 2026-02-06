package com.expensetracker.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SyncCoroutineScope

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {
    
    @Provides
    @Singleton
    @SyncCoroutineScope
    fun provideSyncCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
