/*
 * Copyright (C) 2021 Digital Tectonics
 *
 * This application is the private property of Digital Tectonics
 * Any distribution of this software is unlawful and prohibited.
 */
package com.digital_tectonics.cameraxextreme.extension

import android.content.Context
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
