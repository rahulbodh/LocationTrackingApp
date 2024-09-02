package com.example.trackingapp.utils

import android.app.Application
import com.example.trackingapp.di.AppComponent
import com.example.trackingapp.di.AppModule
import com.example.trackingapp.di.DaggerAppComponent

class TrackApp : Application(){

   val appComponent: AppComponent by lazy {
       DaggerAppComponent.builder()
           .appModule(AppModule())
           .build()
   }

    override fun onCreate() {
        super.onCreate()
    }
}