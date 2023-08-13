package com.velocityappsdj.repeatingtimer.util

import android.content.Context
import android.content.SharedPreferences
import com.velocityappsdj.repeatingtimer.util.Constants.UNIT_SECONDS

class SharedPrefUtil(val context: Context) {
    private var PRIVATE_MODE = 0
    private val PREF_NAME = "repeatingtimerpref"
    val sharedPref: SharedPreferences = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE)

    fun saveTimerUnit(unit: String) {
        val editor = sharedPref.edit()
        editor.putString("unit", unit)
        editor.apply()
    }

    fun getTimerUnit(): String? = sharedPref.getString("unit", UNIT_SECONDS)


    fun saveInterval(interval: Long) {
        val editor = sharedPref.edit()
        editor.putLong("interval", interval)
        editor.apply()
    }

    fun getInterval(): Long = sharedPref.getLong("interval", 30L)

    fun saveSound(sound: String) {
        val editor = sharedPref.edit()
        editor.putString("sound", sound)
        editor.apply()
    }

    fun getSound(): String = sharedPref.getString("sound", "chime")!!

}