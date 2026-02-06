package com.expensetracker.app.di

import com.expensetracker.app.data.firebase.FirestoreCategoryRepositoryImpl
import com.expensetracker.app.data.firebase.FirestoreExpenseRepositoryImpl
import com.expensetracker.app.data.firebase.FirestoreHouseholdRepository
import com.expensetracker.app.data.repository.CategoryRepositoryImpl
import com.expensetracker.app.data.repository.ExpenseRepositoryImpl
import com.expensetracker.app.domain.repository.CategoryRepository
import com.expensetracker.app.domain.repository.ExpenseRepository
import com.expensetracker.app.domain.repository.FirestoreCategoryRepository
import com.expensetracker.app.domain.repository.FirestoreExpenseRepository
import com.expensetracker.app.domain.repository.HouseholdRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindExpenseRepository(
        expenseRepositoryImpl: ExpenseRepositoryImpl
    ): ExpenseRepository
    
    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        categoryRepositoryImpl: CategoryRepositoryImpl
    ): CategoryRepository
    
    @Binds
    @Singleton
    abstract fun bindHouseholdRepository(
        firestoreHouseholdRepository: FirestoreHouseholdRepository
    ): HouseholdRepository
    
    @Binds
    @Singleton
    abstract fun bindFirestoreExpenseRepository(
        firestoreExpenseRepositoryImpl: FirestoreExpenseRepositoryImpl
    ): FirestoreExpenseRepository
    
    @Binds
    @Singleton
    abstract fun bindFirestoreCategoryRepository(
        firestoreCategoryRepositoryImpl: FirestoreCategoryRepositoryImpl
    ): FirestoreCategoryRepository
}
