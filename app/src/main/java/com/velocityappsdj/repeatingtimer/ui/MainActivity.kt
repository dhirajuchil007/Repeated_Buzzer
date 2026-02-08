package com.velocityappsdj.repeatingtimer.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
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
import com.velocityappsdj.repeatingtimer.util.RingSound
import com.velocityappsdj.repeatingtimer.util.SharedPrefUtil
import com.velocityappsdj.repeatingtimer.util.TimeFormatUtil


class MainActivity : AppCompatActivity() {
    private val lTAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    private lateinit var units: Array<String>
    private lateinit var prefUtil: SharedPrefUtil
    
    // Store timer parameters for starting after permission is granted
    private var pendingTimerUnit: String? = null
    private var pendingTimerInterval: Long? = null
    private var pendingRingSound: String? = null
    
    // Permission launcher for notification permission
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, start the timer with pending parameters
            pendingTimerUnit?.let { unit ->
                pendingTimerInterval?.let { interval ->
                    pendingRingSound?.let { sound ->
                        startTimerService(unit, interval, sound)
                    }
                }
            }
            clearPendingTimerParams()
        } else {
            // Permission denied
            Toast.makeText(
                this,
                "Notification permission is required to show timer notifications",
                Toast.LENGTH_LONG
            ).show()
            clearPendingTimerParams()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(lTAG, "onCreate() called with: savedInstanceState = $savedInstanceState")
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        
        // Enable edge-to-edge with backwards compatibility
        enableEdgeToEdge()
        
        setContentView(binding.root)

        // Handle window insets
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            // Apply the insets as padding to the view
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            // Return CONSUMED if you don't want the window insets to keep passing down to descendant views.
            androidx.core.view.WindowInsetsCompat.CONSUMED
        }

        prefUtil = SharedPrefUtil(this)
        initAds()
        setUpPickers()
        setUpRingSoundSpinner()
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
        adView.setAdSize(adSize)

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
            binding.spinnerRingSound.isEnabled = !it
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

    private fun setUpRingSoundSpinner() {
        val ringSounds = RingSound.getAllDisplayNames()
        val adapter = ArrayAdapter(this, R.layout.spinner_item, ringSounds)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.spinnerRingSound.adapter = adapter

        // Set the saved ring sound
        val savedRingSound = prefUtil.getRingSound()
        val position = ringSounds.indexOf(savedRingSound)
        if (position >= 0) {
            binding.spinnerRingSound.setSelection(position)
        }

        // Save selection when changed
        binding.spinnerRingSound.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedSound = ringSounds[position]
                prefUtil.saveRingSound(selectedSound)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }


    private fun setClickListeners() {

        binding.buttonStart.setOnClickListener {
            val button = it as Button
            if (button.text == "start") {


                val selectedUnit = units[binding.unitPick.value]
                val selectedInterval = binding.numberPick.value
                val selectedRingSound = binding.spinnerRingSound.selectedItem.toString()

                Log.d(lTAG, " onClick" + binding.unitPick.value)
                prefUtil.saveTimerUnit(selectedUnit)
                prefUtil.saveInterval(selectedInterval.toLong())
                prefUtil.saveRingSound(selectedRingSound)

                // Check notification permission before starting timer (Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (checkNotificationPermission()) {
                        startTimerService(selectedUnit, selectedInterval.toLong(), selectedRingSound)
                    } else {
                        // Store parameters and request permission
                        pendingTimerUnit = selectedUnit
                        pendingTimerInterval = selectedInterval.toLong()
                        pendingRingSound = selectedRingSound
                        requestNotificationPermission()
                    }
                } else {
                    // No runtime permission needed for older Android versions
                    startTimerService(selectedUnit, selectedInterval.toLong(), selectedRingSound)
                }

            } else {
                stopService()
            }
        }
    }
    
    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not required for older versions
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    private fun clearPendingTimerParams() {
        pendingTimerUnit = null
        pendingTimerInterval = null
        pendingRingSound = null
    }

    private fun stopService() {
        Intent(this@MainActivity, TimerService::class.java).also {
            it.action = ACTION_STOP_SERVICE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(it)
            } else {
                startService(it)
            }
        }
    }

    private fun startTimerService(unit: String, interval: Long, ringSound: String) {
        Log.d(lTAG, "startTimerService() called with: unit = $unit, interval = $interval, ringSound = $ringSound")
        Intent(this@MainActivity, TimerService::class.java).also {
            it.action = ACTION_START_SERVICE
            it.putExtra(UNIT, unit)
            it.putExtra(INTERVAL, interval)
            it.putExtra("RING_SOUND", ringSound)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(it)
            } else {
                startService(it)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

    }
}