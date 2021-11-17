/*
 * Copyright (C) 2021 Digital Tectonics
 *
 * This application is the private property of Digital Tectonics
 * Any distribution of this software is unlawful and prohibited.
 */
package com.digital_tectonics.cameraxextreme.extension

import android.app.Activity
import androidx.annotation.IntRange
import androidx.core.app.ActivityCompat
import com.digital_tectonics.cameraxextreme.viewmodel.MainViewModel

/**
 * ActivityExtension
 *
 * @author Daniel Randall on 2021-11-16.
 */

const val REQUEST_CODE_PERMISSIONS = 10

/**
 * Request Permission (Defaulted to Camera permission)
 */
fun Activity?.requestPermissionsFromUser(
    permissions: Array<String> = MainViewModel.REQUIRED_PERMISSIONS,
    @IntRange(from = 0) requestCode: Int = REQUEST_CODE_PERMISSIONS,
) {
    this?.run {
        ActivityCompat.requestPermissions(this, permissions, requestCode)
    }
}
