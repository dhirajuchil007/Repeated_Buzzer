package com.velocityappsdj.repeatingtimer.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.velocityappsdj.repeatingtimer.R
import com.velocityappsdj.repeatingtimer.ui.MainActivity
import com.velocityappsdj.repeatingtimer.util.Constants
import com.velocityappsdj.repeatingtimer.util.Constants.ACTION_START_SERVICE
import com.velocityappsdj.repeatingtimer.util.Constants.ACTION_STOP_SERVICE
import com.velocityappsdj.repeatingtimer.util.Constants.INTERVAL
import com.velocityappsdj.repeatingtimer.util.Constants.NOTIFICATION_ID
import com.velocityappsdj.repeatingtimer.util.Constants.UNIT
import com.velocityappsdj.repeatingtimer.util.Constants.UNIT_HOURS
import com.velocityappsdj.repeatingtimer.util.Constants.UNIT_MINUTES
import com.velocityappsdj.repeatingtimer.util.Constants.UNIT_SECONDS
import com.velocityappsdj.repeatingtimer.util.SharedPrefUtil
import com.velocityappsdj.repeatingtimer.util.SoundNames
import com.velocityappsdj.repeatingtimer.util.TimeFormatUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class TimerService : LifecycleService() {
    private val TAG = "TimerService"
    lateinit var baseNotificationBuilder: NotificationCompat.Builder
    lateinit var currentNotificationBuilder: NotificationCompat.Builder
    lateinit var mediaPlayer: MediaPlayer
    lateinit var prefUtil: SharedPrefUtil

    companion object {
        var totalTimeSeconds = 0L
        val totalTimeInSecondsLiveData = MutableLiveData<Long>()
        val timeRunInMillis = MutableLiveData<Long>()
        var isRunning = false
        var isRunningLiveData = MutableLiveData<Boolean>()
        var selectedUnit: String? = UNIT_SECONDS
        var selectedInterval = 0L
    }

    override fun onCreate() {
        super.onCreate()
        prefUtil = SharedPrefUtil(this)
        postInitialValues()

    }

    private fun postInitialValues() {
        totalTimeInSecondsLiveData.postValue(0L)
        timeRunInMillis.postValue(0L)
        isRunning = false
        isRunningLiveData.postValue(isRunning)
        timeRun = 0
        timeStarted = 0
        lastSecondTimestamp = 0
        lastRecordedTime = 0L
        selectedUnit = UNIT_SECONDS
        selectedInterval = 0
    }

    private var timeStarted = 0L
    private var timeRun = 0L
    private var lastSecondTimestamp = 0L
    private var lastRecordedTime = 0L

    private fun startTimer() {
        mediaPlayer = MediaPlayer.create(this, getSoundFile());

        Log.d(TAG, "startTimer() called")
        isRunning = true
        isRunningLiveData.postValue(true)
        timeStarted = System.currentTimeMillis()
        CoroutineScope(Dispatchers.Main).launch {
            while (isRunning) {
                lastRecordedTime = System.currentTimeMillis() - timeStarted
                timeRunInMillis.postValue(timeRun + lastRecordedTime)

                if (timeRunInMillis.value!! >= lastSecondTimestamp + 1000L) {
                    totalTimeInSecondsLiveData.postValue(totalTimeInSecondsLiveData.value!! + 1)
                    lastSecondTimestamp += 1000L
                }
                delay(100L)
            }
            timeRun += lastRecordedTime
        }
    }

    private fun getSoundFile(): Int {
        return when (prefUtil.getSound()) {
            SoundNames.CHIME -> R.raw.hour_chime
            SoundNames.BUZZER -> R.raw.buzzer
            SoundNames.AIR_HORN -> R.raw.air_horn_single
            SoundNames.AIR_HORN_THRICE -> R.raw.air_horn
            SoundNames.MICROWAVE_DING -> R.raw.microwave_ding
            SoundNames.BELL -> R.raw.bell
            else -> R.raw.hour_chime
        }
    }

    private fun startForegroundService() {
        Log.d(TAG, "startForegroundService() called")
        startTimer()
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_MUTABLE
        )

        val actionIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        baseNotificationBuilder =
            NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
                .setAutoCancel(false).setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_timer_notification_new)
                .setContentTitle("Timer Running")
                .setContentText("00:00:00")
                .addAction(
                    R.drawable.ic_baseline_stop_circle_24, "Stop",
                    PendingIntent.getService(this, 2, actionIntent, FLAG_IMMUTABLE)
                )
                .setContentIntent(pendingIntent)

        currentNotificationBuilder = baseNotificationBuilder

        startForeground(Constants.NOTIFICATION_ID, baseNotificationBuilder.build())

        totalTimeInSecondsLiveData.observe(this, Observer {

            if (isRunning) {
                val notification = baseNotificationBuilder.setContentText(
                    TimeFormatUtil.getFormattedTime(
                        it
                    )
                )

                notificationManager.notify(NOTIFICATION_ID, notification.build())
            }


        })

    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannel(notificationManager: NotificationManager) {
        Log.d(
            TAG,
            "createNotificationChannel() called with: notificationManager = $notificationManager"
        )
        var channel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            Constants.NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )

        notificationManager.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(
            TAG,
            "onStartCommand() called with: intent = $intent, flags = $flags, startId = $startId"
        )

        intent?.let {
            when (it.action) {
                ACTION_START_SERVICE -> {
                    startForegroundService()

                    selectedUnit = it.getStringExtra(UNIT)
                    selectedInterval = it.getLongExtra(INTERVAL, 0L)
                    setUpBuzzer()

                }

                ACTION_STOP_SERVICE -> {

                    postInitialValues()
                    stopForeground(true)
                    stopSelf()
                }

                else -> {
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun setUpBuzzer() {
        timeRunInMillis.observe(this, Observer {
            if (it == 0L)
                return@Observer

            when (selectedUnit) {
                UNIT_SECONDS -> {
                    if (TimeUnit.MILLISECONDS.toSeconds(it) % selectedInterval == 0L)
                        mediaPlayer.start()
                }

                UNIT_MINUTES -> {
                    if (TimeUnit.MILLISECONDS.toSeconds(it) % (selectedInterval * 60) == 0L)
                        mediaPlayer.start()
                }

                UNIT_HOURS -> {
                    if (TimeUnit.MILLISECONDS.toSeconds(it) % (selectedInterval * 3600) == 0L)
                        mediaPlayer.start()
                }
            }
        })
    }
}