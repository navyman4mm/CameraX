/*
 * Copyright (C) 2021 Digital Tectonics
 *
 * This application is the private property of Digital Tectonics
 * Any distribution of this software is unlawful and prohibited.
 */
package com.digital_tectonics.cameraxextreme.model

import android.content.res.Resources
import android.util.Range
import android.util.Rational
import com.digital_tectonics.cameraxextreme.R

/**
 * CameraExposureData
 *
 * @author Daniel Randall on 2021-11-18.
 */
data class CameraExposureData(
    val cameraExposureCompensationIndex: Int = 0,
    val cameraExposureCompensationRange: Range<Int>,
    val cameraExposureCompensationStep: Rational,
    val cameraExposureCompensationSupported: Boolean,
) {
    fun asArray(resources: Resources): Array<String> {
        return arrayOf(
            resources.getString(R.string.camera_exposure_compensation_supported, cameraExposureCompensationSupported.toString()),
            resources.getString(R.string.camera_exposure_compensation_index, cameraExposureCompensationIndex.toString()),
            resources.getString(R.string.camera_exposure_compensation_range, cameraExposureCompensationRange.toString()),
            resources.getString(R.string.camera_exposure_compensation_step_size, cameraExposureCompensationStep.toString()),
        )
    }
}
