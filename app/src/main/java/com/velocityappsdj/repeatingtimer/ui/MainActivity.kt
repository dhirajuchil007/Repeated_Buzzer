package com.velocityappsdj.repeatingtimer.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.velocityappsdj.repeatingtimer.R
import com.velocityappsdj.repeatingtimer.databinding.ActivityMainBinding
import com.velocityappsdj.repeatingtimer.services.TimerService
import com.velocityappsdj.repeatingtimer.util.Constants.ACTION_START_SERVICE
import com.velocityappsdj.repeatingtimer.util.Constants.ACTION_STOP_SERVICE
import com.velocityappsdj.repeatingtimer.util.Constants.INTERVAL
import com.velocityappsdj.repeatingtimer.util.Constants.UNIT
import com.velocityappsdj.repeatingtimer.util.Constants.UNIT_HOURS
import com.velocityappsdj.repeatingtimer.util.Constants.UNIT_MINUTES
import com.velocityappsdj.repeatingtimer.util.Constants.UNIT_SECONDS
import com.velocityappsdj.repeatingtimer.util.SharedPrefUtil
import com.velocityappsdj.repeatingtimer.util.TimeFormatUtil


class MainActivity : AppCompatActivity() {
    private val lTAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    private lateinit var units: Array<String>
    private lateinit var prefUtil: SharedPrefUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(lTAG, "onCreate() called with: savedInstanceState = $savedInstanceState")
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefUtil = SharedPrefUtil(this)
        initAds()
        setUpPickers()
        setPrefData()
        setClickListeners()
        setUpObservers()

    }

    private fun initAds() {
        MobileAds.initialize(
            this
        ) {

        }

        val adView = AdView(this)
        adView.adUnitId = getString(R.string.ad_unit_id)
        binding.adViewContainer.addView(adView)
        loadBanner(adView)

    }

    private fun loadBanner(adView: AdView) {
        // Create an ad request.
        val adRequest: AdRequest = AdRequest.Builder()
            .build()
        val adSize: AdSize = getAdSize()
        // Step 4 - Set the adaptive ad size on the ad view.
        adView.adSize = adSize

        // Step 5 - Start loading the ad in the background.
        adView.loadAd(adRequest)
    }

    private fun getAdSize(): AdSize {
        // Step 2 - Determine the screen width (less decorations) to use for the ad width.
        val outMetrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = this.display
            display?.getRealMetrics(outMetrics)
        } else {
            @Suppress("DEPRECATION")
            val display: Display = windowManager.defaultDisplay
            @Suppress("DEPRECATION")
            display.getMetrics(outMetrics)
        }
        val widthPixels = outMetrics.widthPixels.toFloat()
        val density = outMetrics.density
        val adWidth = (widthPixels / density).toInt()

        // Step 3 - Get adaptive ad size and return for setting on the ad view.
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    }

    private fun setPrefData() {

        binding.numberPick.value = prefUtil.getInterval().toInt()
        prefUtil.getTimerUnit()?.let {
            units.forEachIndexed { index, s ->
                if (s == it) {
                    binding.unitPick.value = index
                }
            }
        }

    }

    private fun setUpObservers() {
        TimerService.totalTimeInSecondsLiveData.observe(this, {
            binding.tvTime.text = TimeFormatUtil.getFormattedTime(it)
        })

        TimerService.isRunningLiveData.observe(this, {

            binding.numberPick.isEnabled = !it
            binding.unitPick.isEnabled = !it
            binding.numberPickerGroup.visibility = if (it) View.INVISIBLE else View.VISIBLE
            binding.tvGroup.visibility = if (it) View.VISIBLE else View.INVISIBLE
            binding.buttonStart.text =
                if (it) getString(R.string.stop) else getString(R.string.start)
            var unit = TimerService.selectedUnit
            unit?.let {
                if (TimerService.selectedInterval == 1L)
                    unit = unit!!.substring(0, unit!!.length - 1)
                binding.tvUnit.text = unit
                binding.tvInterval.text = TimerService.selectedInterval.toString()
            }
        })
    }

    private fun setUpPickers() {
        units = arrayOf(UNIT_SECONDS, UNIT_MINUTES, UNIT_HOURS)
        binding.unitPick.apply {
            displayedValues = units
            minValue = 0
            maxValue = 2
        }

        binding.numberPick.apply {
            minValue = 1
            maxValue = 59
        }

    }


    private fun setClickListeners() {

        binding.buttonStart.setOnClickListener {
            val button = it as Button
            if (button.text == "start") {


                val selectedUnit = units[binding.unitPick.value]
                val selectedInterval = binding.numberPick.value

                Log.d(lTAG, " onClick" + binding.unitPick.value)
                prefUtil.saveTimerUnit(selectedUnit)
                prefUtil.saveInterval(selectedInterval.toLong())

                startTimerService(selectedUnit, selectedInterval.toLong())

            } else {
                stopService()
            }
        }
    }

    private fun stopService() {
        Intent(this@MainActivity, TimerService::class.java).also {
            it.action = ACTION_STOP_SERVICE
            startService(it)
        }
    }

    private fun startTimerService(unit: String, interval: Long) {
        Log.d(lTAG, "startTimerService() called with: unit = $unit, interval = $interval")
        Intent(this@MainActivity, TimerService::class.java).also {
            it.action = ACTION_START_SERVICE
            it.putExtra(UNIT, unit)
            it.putExtra(INTERVAL, interval)
            startService(it)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

    }
}