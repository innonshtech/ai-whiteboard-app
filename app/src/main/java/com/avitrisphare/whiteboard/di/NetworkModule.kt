package com.avitrisphare.whiteboard.di

import com.avitrisphare.whiteboard.BuildConfig
import com.avitrisphare.whiteboard.data.remote.PanelApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun providePanelApi(client: OkHttpClient): PanelApi =
        PanelApi(client, BuildConfig.API_BASE_URL.trimEnd('/'))
}
