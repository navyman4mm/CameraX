/*
 * Copyright (C) 2021 Digital Tectonics
 *
 * This application is the private property of Digital Tectonics
 * Any distribution of this software is unlawful and prohibited.
 */
package com.digital_tectonics.cameraxextreme.extension

import android.content.Context
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.digital_tectonics.cameraxextreme.R
import java.io.File

/**
 * ContextExtension
 *
 * @author Daniel Randall on 2021-11-16.
 */

fun Context?.getOutputDirectory(): File? {
    return if (this != null) {
        val mediaDir = externalMediaDirs.firstOrNull()?.run {
            File(this, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        if (mediaDir != null && mediaDir.exists()) {
            mediaDir
        } else {
            filesDir
        }
    } else {
        null
    }
}
