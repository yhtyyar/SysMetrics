package com.sysmetrics.app.di

import android.content.Context
import com.sysmetrics.app.data.repository.PreferencesRepository
import com.sysmetrics.app.data.repository.SystemMetricsRepository
import com.sysmetrics.app.data.source.PreferencesDataSource
import com.sysmetrics.app.data.source.SystemDataSource
import com.sysmetrics.app.domain.usecase.GetSystemMetricsUseCase
import com.sysmetrics.app.domain.usecase.ManageOverlayConfigUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing application-level dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSystemDataSource(): SystemDataSource {
        return SystemDataSource()
    }

    @Provides
    @Singleton
    fun providePreferencesDataSource(
        @ApplicationContext context: Context
    ): PreferencesDataSource {
        return PreferencesDataSource(context)
    }

    @Provides
    @Singleton
    fun provideSystemMetricsRepository(
        systemDataSource: SystemDataSource
    ): SystemMetricsRepository {
        return SystemMetricsRepository(systemDataSource)
    }

    @Provides
    @Singleton
    fun providePreferencesRepository(
        preferencesDataSource: PreferencesDataSource
    ): PreferencesRepository {
        return PreferencesRepository(preferencesDataSource)
    }

    @Provides
    fun provideGetSystemMetricsUseCase(
        repository: SystemMetricsRepository
    ): GetSystemMetricsUseCase {
        return GetSystemMetricsUseCase(repository)
    }

    @Provides
    fun provideManageOverlayConfigUseCase(
        repository: PreferencesRepository
    ): ManageOverlayConfigUseCase {
        return ManageOverlayConfigUseCase(repository)
    }
}
