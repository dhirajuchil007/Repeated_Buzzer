package com.velocityappsdj.repeatingtimer.util

import java.util.concurrent.TimeUnit

object TimeFormatUtil {
    fun getFormattedTime(timeInSeconds: Long): String {
        var seconds = timeInSeconds
        var result = ""
        val hours = TimeUnit.SECONDS.toHours(timeInSeconds)
        seconds -= TimeUnit.HOURS.toSeconds(hours)
        val minutes = TimeUnit.SECONDS.toMinutes(seconds)

        seconds -= TimeUnit.MINUTES.toSeconds(minutes)


        return "${if (hours < 10) "0" else ""}$hours:" +
                "${if (minutes < 10) "0" else ""}$minutes:" +
                "${if (seconds < 10) "0" else ""}$seconds"

    }
}