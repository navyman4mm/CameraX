/*
 * Copyright (C) 2021 Digital Tectonics
 *
 * This application is the private property of Digital Tectonics
 * Any distribution of this software is unlawful and prohibited.
 */
package com.digital_tectonics.cameraxextreme.extension

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.digital_tectonics.cameraxextreme.constant.IMAGE_CAPTURE_TAG
import com.digital_tectonics.cameraxextreme.model.CameraExposureData

/**
 * CameraExtension
 *
 * @author Daniel Randall on 2021-11-18.
 */

/**
 *
 * @return [ImageCapture] optional
 */
fun Context?.startCameraAndPreview(
    captureViewFinder: PreviewView,
    lifecycleOwner: LifecycleOwner,
    imageCapture: ImageCapture = ImageCapture.Builder()
        .setCaptureMode(CAPTURE_MODE_MAXIMIZE_QUALITY).build(),
): ImageCapture? {
    return if (this != null) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(captureViewFinder.surfaceProvider)
                }

            val orientationEventListener = object : OrientationEventListener(this) {
                override fun onOrientationChanged(orientation: Int) {
                    // Monitors orientation values to determine the target rotation value
                    val rotation: Int = when (orientation) {
                        in 45..134 -> Surface.ROTATION_270
                        in 135..224 -> Surface.ROTATION_180
                        in 225..314 -> Surface.ROTATION_90
                        else -> Surface.ROTATION_0
                    }

                    imageCapture.targetRotation = rotation
                }
            }
            orientationEventListener.enable()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera - Utilizing the Back Camera only
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                )
            } catch (exception: Exception) {
                Log.e(lifecycleOwner.toString(), "Use case binding failed", exception)
            }
        }, ContextCompat.getMainExecutor(this))
        // Return
        imageCapture
    } else {
        null
    }
}

@SuppressLint("RestrictedApi", "UnsafeOptInUsageError")
fun ImageCapture?.logCameraExposureData(tag: String = IMAGE_CAPTURE_TAG): CameraExposureData? {
    return if (this != null) {
        this.camera?.run {
            Log.d(tag, "Camera Information is available")
            var cameraExposureData: CameraExposureData? = null
            this.cameraInfo.exposureState.run {
                cameraExposureData = CameraExposureData(
                    this.exposureCompensationIndex,
                    this.exposureCompensationRange,
                    this.exposureCompensationStep,
                    this.isExposureCompensationSupported,
                )
                Log.d(tag, "Camera ExposureCompensationIndex: ${this.exposureCompensationIndex}")
                Log.d(tag, "Camera ExposureCompensationRange: ${this.exposureCompensationRange}")
                Log.d(tag, "Camera ExposureCompensationStep: ${this.exposureCompensationStep}")
                Log.d(
                    tag,
                    "Camera ExposureCompensationSupported: ${this.isExposureCompensationSupported}"
                )
            }
            cameraExposureData
        }
    } else {
        null
    }
}

@SuppressLint("RestrictedApi", "UnsafeOptInUsageError")
fun ImageCapture?.setCameraExposureToMax(tag: String = IMAGE_CAPTURE_TAG) {
    if (this != null) {
        val cameraExposureData = this.logCameraExposureData(tag)
        if (cameraExposureData != null && cameraExposureData.cameraExposureCompensationSupported) {
            this.camera?.cameraControl?.setExposureCompensationIndex(cameraExposureData.cameraExposureCompensationRange.upper)
            Log.d(tag, "Exposure Range set: ${cameraExposureData.cameraExposureCompensationRange.upper}")
        }
    }
}
