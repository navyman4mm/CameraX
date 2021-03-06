/*
 * Copyright (C) 2021 Digital Tectonics
 *
 * This application is the private property of Digital Tectonics
 * Any distribution of this software is unlawful and prohibited.
 */
package com.digital_tectonics.cameraxextreme.constant

import android.view.View

/**
 * AppConstant
 *
 * @author Daniel Randall on 2021-11-17.
 */

const val APP_PACKAGE = "com.digital_tectonics.spacetrader"
const val APP_STORE_URL = "https://play.google.com/store/apps/details?id=$APP_PACKAGE"

/**
 * Combination of all flags required to put activity into immersive mode
 */
const val FLAGS_FULLSCREEN =
    View.SYSTEM_UI_FLAG_LOW_PROFILE or
        View.SYSTEM_UI_FLAG_FULLSCREEN or
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

/**
 * Milliseconds used for UI animations
 */
const val ANIMATION_FAST_MILLIS = 50L
const val ANIMATION_SLOW_MILLIS = 100L

const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
const val JPG_FILE_TAG = ".jpg"
const val PNG_FILE_TAG = ".png"

const val IMAGE_CAPTURE_TAG = "IMAGE_CAPTURE"

/* Auto Fucus Testing */
const val FLOAT_HALF = 0.5F
const val FLOAT_ONE = 1F

const val AUTO_FOCUS_INTERVAL = 2L

