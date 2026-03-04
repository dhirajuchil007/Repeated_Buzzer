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

    fun saveRingSound(ringSound: String) {
        val editor = sharedPref.edit()
        editor.putString("ring_sound", ringSound)
        editor.apply()
    }

    fun getRingSound(): String = sharedPref.getString("ring_sound", RingSound.CHIME.displayName) ?: RingSound.CHIME.displayName

    // Review Dialog Tracking
    fun getBuzzerStopCount(): Int = sharedPref.getInt("buzzer_stop_count", 0)

    fun incrementBuzzerStopCount() {
        val currentCount = getBuzzerStopCount()
        sharedPref.edit().putInt("buzzer_stop_count", currentCount + 1).apply()
    }

    fun getReviewDialogLastShownTime(): Long = sharedPref.getLong("review_dialog_last_shown_time", 0L)

    fun updateReviewDialogLastShownTime(timeMillis: Long) {
        sharedPref.edit().putLong("review_dialog_last_shown_time", timeMillis).apply()
    }

    fun isReviewDialogNeverShow(): Boolean = sharedPref.getBoolean("review_dialog_never_show", false)

    fun setReviewDialogNeverShow(neverShow: Boolean) {
        sharedPref.edit().putBoolean("review_dialog_never_show", neverShow).apply()
    }
}