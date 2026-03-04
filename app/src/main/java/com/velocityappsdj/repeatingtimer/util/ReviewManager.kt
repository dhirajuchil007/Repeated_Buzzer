package com.velocityappsdj.repeatingtimer.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.velocityappsdj.repeatingtimer.R

object ReviewManager {

    private const val DAYS_10_IN_MILLIS = 10L * 24 * 60 * 60 * 1000

    fun checkAndShowReviewDialog(context: Context, prefUtil: SharedPrefUtil) {
        if (prefUtil.isReviewDialogNeverShow()) {
            return
        }

        val stopCount = prefUtil.getBuzzerStopCount()
        
        // Check if we should show the dialog
        val shouldShow = if (stopCount == 4) {
            true // Show on the 4th time
        } else if (stopCount > 4) {
            // After the 4th time, show once every 10 days
            val lastShown = prefUtil.getReviewDialogLastShownTime()
            val now = System.currentTimeMillis()
            (now - lastShown) >= DAYS_10_IN_MILLIS
        } else {
            false
        }

        if (shouldShow) {
            showDialog(context, prefUtil)
        }
    }

    private fun showDialog(context: Context, prefUtil: SharedPrefUtil) {
        // Update last shown time as soon as we decide to show it
        prefUtil.updateReviewDialogLastShownTime(System.currentTimeMillis())

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.review_dialog_title)
            .setMessage(R.string.review_dialog_message)
            .setPositiveButton(R.string.review_dialog_positive) { dialog, _ ->
                // Redirect to Play Store
                redirectToPlayStore(context)
                // If they say sure, we assume they reviewed and we don't need to ask again
                prefUtil.setReviewDialogNeverShow(true)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.review_dialog_negative) { dialog, _ ->
                // Just dismiss, will be asked again after 10 days (if they keep stopping)
                dialog.dismiss()
            }
            .setNeutralButton(R.string.review_dialog_neutral) { dialog, _ ->
                // Never ask again
                prefUtil.setReviewDialogNeverShow(true)
                dialog.dismiss()
            }
            .setCancelable(false) // Force the user to choose an option
            .show()
    }

    private fun redirectToPlayStore(context: Context) {
        val appPackageName = context.packageName
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
        } catch (e: android.content.ActivityNotFoundException) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
        }
    }
}
