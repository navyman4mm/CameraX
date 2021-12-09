/*
 * Copyright (C) 2021 Digital Tectonics
 *
 * This application is the private property of Digital Tectonics
 * Any distribution of this software is unlawful and prohibited.
 */
package com.digital_tectonics.cameraxextreme.model

import android.annotation.SuppressLint
import android.hardware.camera2.CameraManager
import android.util.Log
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import com.digital_tectonics.cameraxextreme.constant.ExposureLevel
import com.digital_tectonics.cameraxextreme.constant.IMAGE_CAPTURE_TAG

/**
 * CameraXSetupData - Designed to provide a centralized location for critical Camera data, while
 * making the app more testable. This increases testability comes via not having the individual elements
 * held within a context.
 * TODO: Look to switch this to use val's
 *
 * @author Daniel Randall on 2021-11-19.
 */
data class CameraXSetupData(
    var imageCapture: ImageCapture? = null,
    var camera: Camera? = null,
    var cameraManager: CameraManager? = null,
) {

    @SuppressLint("UnsafeOptInUsageError")
    fun logCameraExposureData(
        tag: String = IMAGE_CAPTURE_TAG,
        willLogResults: Boolean = true
    ): CameraExposureData? {
        var cameraExposureData: CameraExposureData? = null
        camera?.cameraInfo?.exposureState?.run {
            cameraExposureData = CameraExposureData(
                this.exposureCompensationIndex,
                this.exposureCompensationRange,
                this.exposureCompensationStep,
                this.isExposureCompensationSupported,
            )
            if (willLogResults) {
                Log.d(tag, "Camera ExposureCompensationIndex: ${this.exposureCompensationIndex}")
                Log.d(tag, "Camera ExposureCompensationRange: ${this.exposureCompensationRange}")
                Log.d(tag, "Camera ExposureCompensationStep: ${this.exposureCompensationStep}")
                Log.d(tag, "Camera ExposureCompensationSupported: ${this.isExposureCompensationSupported}")
            }
        }

        return cameraExposureData
    }

    /**
     * Set the Camera Instance to the
     */
    @SuppressLint("UnsafeOptInUsageError")
    fun setCameraExposureToValue(
        exposureLevel: ExposureLevel = ExposureLevel.NETURAL_EV,
        tag: String = IMAGE_CAPTURE_TAG,
    ) {
        val cameraExposureData = this.logCameraExposureData(tag)
        if (cameraExposureData != null && cameraExposureData.cameraExposureCompensationSupported) {
            // TODO: Make this when smaller
            val exposureValue = when (exposureLevel) {
                ExposureLevel.DARK_EV_MAX -> cameraExposureData.cameraExposureCompensationRange.lower
                ExposureLevel.LIGHT_EV_MAX -> cameraExposureData.cameraExposureCompensationRange.upper
                ExposureLevel.DARK_WHOLE_EV -> {
                    cameraExposureData.cameraExposureCompensationIndex - cameraExposureData.cameraExposureCompensationStep.denominator
                }
                ExposureLevel.LIGHT_WHOLE_EV -> {
                    cameraExposureData.cameraExposureCompensationIndex + cameraExposureData.cameraExposureCompensationStep.denominator
                }
                ExposureLevel.DARK_EV_STEP -> {
                    cameraExposureData.cameraExposureCompensationIndex - cameraExposureData.cameraExposureCompensationStep.numerator
                }
                ExposureLevel.LIGHT_EV_STEP -> {
                    cameraExposureData.cameraExposureCompensationIndex + cameraExposureData.cameraExposureCompensationStep.numerator
                }
                else -> 0
            }
            this.camera?.cameraControl?.setExposureCompensationIndex(exposureValue)
            Log.d(tag, "Exposure Range set: $exposureValue")
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun logCameraDynamicDepthConfigurationsData(
        tag: String = IMAGE_CAPTURE_TAG,
        willLogResults: Boolean = true
    ) {
        camera?.cameraInfo?.run {
            val characteristics =
                cameraManager?.getCameraCharacteristics(Camera2CameraInfo.from(this).cameraId)
//            val configs: StreamConfigurationMap? =
//                characteristics?.get(CameraCharacteristics.DEPTH_AVAILABLE_DYNAMIC_DEPTH_STREAM_CONFIGURATIONS)
//            configs?.
        }
    }
}
