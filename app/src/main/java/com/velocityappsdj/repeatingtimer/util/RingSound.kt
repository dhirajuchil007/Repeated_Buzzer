package com.velocityappsdj.repeatingtimer.util

import com.velocityappsdj.repeatingtimer.R

enum class RingSound(val displayName: String, val resourceId: Int) {
    CHIME("Chime", R.raw.chime),
    DING("Ding", R.raw.ding),
    BELL("Bell", R.raw.bell),
    BUZZER("Buzzer", R.raw.buzzer),
    AIR_HORN("Air Horn", R.raw.air_horn),
    AIR_HORN_3X("Air Horn 4x", R.raw.air_horn_3x);

    companion object {
        fun fromDisplayName(name: String): RingSound {
            return values().find { it.displayName == name } ?: CHIME
        }

        fun getAllDisplayNames(): Array<String> {
            return values().map { it.displayName }.toTypedArray()
        }
    }
}
