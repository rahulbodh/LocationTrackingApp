package com.example.trackingapp.di

import com.example.trackingapp.Retrofit.RoutesApi
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
class AppModule {

    @Singleton
    @Provides
   fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    @Singleton
    @Provides
    fun provideRetrofit(client : OkHttpClient) : Retrofit{
        return Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/api/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Singleton
    @Provides
    fun providesRouteApi(retrofit: Retrofit) : RoutesApi {
        return retrofit.create(RoutesApi::class.java)
    }

}