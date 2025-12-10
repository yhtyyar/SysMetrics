package com.sysmetrics.app.di

import android.content.Context
import com.sysmetrics.app.core.di.DefaultDispatcherProvider
import com.sysmetrics.app.core.di.DispatcherProvider
import com.sysmetrics.app.data.repository.PreferencesRepository
import com.sysmetrics.app.data.repository.SystemMetricsRepository
import com.sysmetrics.app.data.source.PreferencesDataSource
import com.sysmetrics.app.data.source.SystemDataSource
import com.sysmetrics.app.domain.repository.IPreferencesRepository
import com.sysmetrics.app.domain.repository.ISystemMetricsRepository
import com.sysmetrics.app.domain.usecase.GetSystemMetricsUseCase
import com.sysmetrics.app.domain.usecase.ManageOverlayConfigUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing application-level dependencies.
 * Uses interface bindings for repositories to enable easy testing and mocking.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider {
        return DefaultDispatcherProvider()
    }

    @Provides
    @Singleton
    fun provideSystemDataSource(
        dispatcherProvider: DispatcherProvider
    ): SystemDataSource {
        return SystemDataSource(dispatcherProvider)
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
    ): ISystemMetricsRepository {
        return SystemMetricsRepository(systemDataSource)
    }

    @Provides
    @Singleton
    fun providePreferencesRepository(
        preferencesDataSource: PreferencesDataSource
    ): IPreferencesRepository {
        return PreferencesRepository(preferencesDataSource)
    }

    @Provides
    fun provideGetSystemMetricsUseCase(
        repository: ISystemMetricsRepository
    ): GetSystemMetricsUseCase {
        return GetSystemMetricsUseCase(repository)
    }

    @Provides
    fun provideManageOverlayConfigUseCase(
        repository: IPreferencesRepository
    ): ManageOverlayConfigUseCase {
        return ManageOverlayConfigUseCase(repository)
    }
}
