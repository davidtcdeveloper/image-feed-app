package com.example.imagefeed.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() {
        baselineProfileRule.collect(
            packageName = "com.example.imagefeed.android",
            includeInStartupProfile = true,
        ) {
            // Cold start
            pressHome()
            startActivityAndWait()

            // Wait for content to load
            device.wait(Until.hasObject(By.pkg("com.example.imagefeed.android")), 5000)
            device.waitForIdle()

            // Scroll feed list
            val scrollable = device.findObject(By.scrollable(true))
            scrollable?.setGestureMargin(device.displayWidth / 5)
            scrollable?.fling(Direction.DOWN)
            device.waitForIdle()
            scrollable?.fling(Direction.UP)
            device.waitForIdle()

            // Navigate to Collections tab
            val collectionsTab = device.findObject(By.text("Collections"))
            collectionsTab?.click()
            device.waitForIdle()

            // Navigate to Search tab
            val searchTab = device.findObject(By.text("Search"))
            searchTab?.click()
            device.waitForIdle()

            // Return to Photos tab
            val photosTab = device.findObject(By.text("Photos"))
            photosTab?.click()
            device.waitForIdle()
        }
    }
}
