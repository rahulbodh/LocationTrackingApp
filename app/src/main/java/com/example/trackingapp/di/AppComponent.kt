package com.example.trackingapp.di

import com.example.trackingapp.ui.MainActivity
import com.example.trackingapp.ui.MapsActivity
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {

    fun inject(mainActivity: MainActivity)

    fun inject(mapsActivity: MapsActivity)


}