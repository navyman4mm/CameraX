/*
 * Copyright (C) 2021 Digital Tectonics
 *
 * This application is the private property of Digital Tectonics
 * Any distribution of this software is unlawful and prohibited.
 */
package com.digital_tectonics.cameraxextreme.extension

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.digital_tectonics.cameraxextreme.constant.AUTO_FOCUS_INTERVAL
import com.digital_tectonics.cameraxextreme.constant.FLOAT_HALF
import com.digital_tectonics.cameraxextreme.constant.FLOAT_ONE
import com.digital_tectonics.cameraxextreme.constant.IMAGE_CAPTURE_TAG
import com.digital_tectonics.cameraxextreme.model.CameraExposureData
import com.digital_tectonics.cameraxextreme.model.CameraXSetupData
import java.util.concurrent.TimeUnit

/**
 * CameraExtension
 *
 * @author Daniel Randall on 2021-11-18.
 */

/**
 * @param updateMethod [Lamba] with [CameraXSetupData]
 * @return [ImageCapture] optional
 */
fun Context?.startCameraAndPreview(
    captureViewFinder: PreviewView,
    lifecycleOwner: LifecycleOwner,
    updateMethod: (CameraXSetupData) -> Unit,
    imageCapture: ImageCapture = ImageCapture.Builder()
        .setCaptureMode(CAPTURE_MODE_MAXIMIZE_QUALITY).build(),
): CameraXSetupData {
    return startCameraAndPreview(
        captureViewFinder,
        lifecycleOwner,
        updateMethod,
        imageCapture,
        isAutoFocusEnabled = false,
    )
}

/**
 * @param updateMethod [Lamba] with [CameraXSetupData]
 * @return [ImageCapture] optional
 */
fun Context?.startCameraAndPreview(
    captureViewFinder: PreviewView,
    lifecycleOwner: LifecycleOwner,
    updateMethod: (CameraXSetupData) -> Unit,
    imageCapture: ImageCapture = ImageCapture.Builder()
        .setCaptureMode(CAPTURE_MODE_MAXIMIZE_QUALITY).build(),
    isAutoFocusEnabled: Boolean = true,
    autoFocusInterval: Long = AUTO_FOCUS_INTERVAL,
): CameraXSetupData {
    if (this != null) {
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
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                )

                updateMethod(
                    CameraXSetupData(
                        imageCapture,
                        camera,
                        this.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    )
                )
                // Auto Focus Testing
                if (isAutoFocusEnabled) {
                    captureViewFinder.afterViewMeasured {
                        // Generate a center point for the focus to occur
                        val autoFocusPoint =
                            SurfaceOrientedMeteringPointFactory(FLOAT_ONE, FLOAT_ONE)
                                .createPoint(FLOAT_HALF, FLOAT_HALF)
                        try {
                            camera.cameraControl.startFocusAndMetering(FocusMeteringAction.Builder(
                                autoFocusPoint,
                                FocusMeteringAction.FLAG_AF
                            ).apply {
                                // Start auto-focusing after 2 seconds
                                setAutoCancelDuration(autoFocusInterval, TimeUnit.SECONDS)
                            }.build()
                            )
                        } catch (exception: CameraInfoUnavailableException) {
                            Log.d(lifecycleOwner.toString(), "Camera not available", exception)
                        }
                    }
                }
                /* Testing: */
                camera.getResolutionData(this, lifecycleOwner.toString())
            } catch (exception: Exception) {
                Log.e(lifecycleOwner.toString(), "Use case binding failed", exception)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    return CameraXSetupData(imageCapture)
}

@SuppressLint("UnsafeOptInUsageError")
fun Camera?.getResolutionData(
    context: Context,
    tag: String = IMAGE_CAPTURE_TAG,
    isLoggingOn: Boolean = true,
): Array<out Size>? {
    return if (this != null) {
        /* Testing: */
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val characteristics =
            cameraManager.getCameraCharacteristics(Camera2CameraInfo.from(this.cameraInfo).cameraId)
        val configs: StreamConfigurationMap? =
            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

        // Testing Resolutions
        val imageAnalysisSizes = configs?.getOutputSizes(ImageFormat.YUV_420_888)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(tag, "API 23+")
            logHighRes(configs, tag)
        }

        if (isLoggingOn) {
            imageAnalysisSizes?.forEach {
                Log.d(tag, "YUV_420_888 available output size: $it")
            }

            // Preview Size Testing
            configs?.getOutputSizes(SurfaceTexture::class.java)?.forEach {
                Log.d(tag, "Preview available output size: $it")
            }
        }

        imageAnalysisSizes
    } else {
        null
    }
}

/**
 * TODO: Review to see if these need to be retained
 */
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

/**
 * TODO: Review to see if these need to be retained
 */
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

/**
 * Get a list of supported high resolution sizes, which cannot operate at full BURST_CAPTURE rate
 * i.e. capture rates at 20 FPS or less
 */
@RequiresApi(Build.VERSION_CODES.M)
private fun logHighRes(configs: StreamConfigurationMap?, tag: String = IMAGE_CAPTURE_TAG) {
    val imageData = configs?.getHighResolutionOutputSizes(ImageFormat.YUV_420_888)

    imageData?.forEach {
        Log.d(tag, "YUV_420_888 available High Resolution output size: $it")
    }
}
