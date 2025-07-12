package com.dergoogler.mmrl.wx.datastore.di

import com.dergoogler.mmrl.wx.datastore.provider.DataStoreProvider
import com.dergoogler.mmrl.wx.datastore.UserPreferencesSerializer
import com.dergoogler.mmrl.wx.datastore.model.UserPreferences
import android.content.Context
import androidx.datastore.core.DataStore
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
    fun providesUserPreferencesDataStore(
        @ApplicationContext context: Context,
        userPreferencesSerializer: UserPreferencesSerializer,
    ): DataStore<UserPreferences> =
        DataStoreProvider().providesUserPreferencesDataStore(context, userPreferencesSerializer)
}