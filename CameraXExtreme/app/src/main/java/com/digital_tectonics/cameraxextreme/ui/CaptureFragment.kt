/*
 * Copyright (C) 2021 Digital Tectonics
 *
 * This application is the private property of Digital Tectonics
 * Any distribution of this software is unlawful and prohibited.
 */
package com.digital_tectonics.cameraxextreme.ui

import android.annotation.SuppressLint
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.digital_tectonics.cameraxextreme.R
import com.digital_tectonics.cameraxextreme.databinding.FragmentCaptureBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.net.Uri
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.digital_tectonics.cameraxextreme.constant.ExposureLevel
import com.digital_tectonics.cameraxextreme.constant.FILENAME_FORMAT
import com.digital_tectonics.cameraxextreme.constant.JPG_FILE_TAG
import com.digital_tectonics.cameraxextreme.extension.*
import com.digital_tectonics.cameraxextreme.model.CameraXSetupData
import com.digital_tectonics.cameraxextreme.viewmodel.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.*

/**
 * CaptureFragment
 *
 * @author Daniel Randall on 2021-11-16.
 */
class CaptureFragment : Fragment() {
    val TAG: String = CaptureFragment::class.java.simpleName

    private val sharedViewModel: MainViewModel by activityViewModels()
    private var binding: FragmentCaptureBinding? = null
    private var cameraSetup = CameraXSetupData()

    private var outputDirectory: File? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var isFirstResume: Boolean = true
    private var isCameraAvailable: Boolean = false

    private val FRAME_DURATION_DEFAULT = 4L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkCameraPermissions()

        outputDirectory = context?.getOutputDirectory()
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_capture, menu)
        super.onCreateOptionsMenu(menu, menuInflater)
    }

    @SuppressLint("RestrictedApi", "UnsafeOptInUsageError")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_timed_exposure -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.timed_exposure)
                    .setPositiveButton(R.string.set_exposure_time) { _, _ ->
                        sharedViewModel.cameraSetupData.value?.setExactExposureTime()
                    }
                    .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()

                true
            }
            R.id.action_exposure_max -> {
                sharedViewModel.cameraSetupData.value?.setCameraExposureToValue(ExposureLevel.LIGHT_EV_MAX)
                true
            }
            R.id.action_info -> {
                val cameraExposureData = cameraSetup.logCameraExposureData(TAG)

                cameraExposureData?.run {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.camera_exposure_data_title)
                        .setItems(this.asArray(resources)) { dialog, which ->
                            // TODO: Allow for updates or settings?
                        }
                        .setPositiveButton(R.string.exposure_step_up_) { _, _ ->
                            sharedViewModel.cameraSetupData.value?.setCameraExposureToValue(ExposureLevel.LIGHT_EV_STEP)
                        }
                        .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .setNeutralButton(R.string.exposure_step_down) { _, _ ->
                            sharedViewModel.cameraSetupData.value?.setCameraExposureToValue(ExposureLevel.DARK_EV_STEP)
                        }
                        .show()
                }

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCaptureBinding.inflate(inflater, container, false)
        if (isCameraAvailable) {
            startCameraAndPreviewRunning()
        }

        setHasOptionsMenu(true)

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the listener for capture photo fab
        binding?.captureFAB?.setOnClickListener { takePhoto() }

        sharedViewModel.cameraPermission.observe(viewLifecycleOwner, {
            Log.d(TAG, "Camera Permission: $it")
            isCameraAvailable = it
            if (isCameraAvailable) {
                startCameraAndPreviewRunning()
            }
        })

        sharedViewModel.cameraSetupData.observe(viewLifecycleOwner, {
            cameraSetup = it
            cameraSetup.logCameraExposureData(TAG)
        })
    }

    override fun onResume() {
        super.onResume()
        if (isFirstResume) {
            isFirstResume = false
        } else {
            // Check that the app still have permission to use the camera
            if (sharedViewModel.allPermissionsGranted(requireContext()) && cameraExecutor.isShutdown) {
                cameraExecutor = Executors.newSingleThreadExecutor()
                checkCameraPermissions()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // While this was set by Google to have it in the [onDestroy()] moving here maybe a better practice
        cameraExecutor.shutdown()
    }

    private fun checkCameraPermissions() {
        if (sharedViewModel.allPermissionsGranted(requireContext())) {
            isCameraAvailable = true
        } else {
            activity?.requestPermissionsFromUser()
        }
    }

    /**
     * TODO: Move to Extension file
     */
    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = cameraSetup.imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + JPG_FILE_TAG
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        cameraSetup.logCameraExposureData(TAG)

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            })
    }

    private fun startCameraAndPreviewRunning() {
        binding?.captureFAB?.isVisible = true
        binding?.captureViewFinder?.run {
            Log.d(TAG, "Capture Preview is present")
            context.startCameraAndPreview(
                this,
                this@CaptureFragment,
                { data: CameraXSetupData -> sharedViewModel.setupCameraXData(data) },
                cameraExecutor = cameraExecutor,
            )
        }
    }

    /**
     * TODO: Remove once more testing is complete
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding?.captureViewFinder?.surfaceProvider)
                }

            cameraSetup.imageCapture = ImageCapture.Builder()
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    cameraSetup.imageCapture,
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Use case binding failed", exception)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }



    private fun buildImageAnalysis(): ImageAnalysis {
        val builder = ImageAnalysis.Builder()
        val camera2InterOp = Camera2Interop.Extender(builder)
        camera2InterOp.setCaptureRequestOption(
            CaptureRequest.CONTROL_AE_MODE,
            CaptureRequest.CONTROL_AE_MODE_OFF
        )
        camera2InterOp.setCaptureRequestOption(
            CaptureRequest.CONTROL_AWB_MODE,
            CaptureRequest.CONTROL_AWB_MODE_OFF
        )
//        camera2InterOp.setCaptureRequestOption(
//            CaptureRequest.SENSOR_FRAME_DURATION,
//            FRAME_DURATION_DEFAULT
//        )
//        camera2InterOp.setCaptureRequestOption(
//            CaptureRequest.SENSOR_EXPOSURE_TIME,
//            EXPOSURE_TIME_DEFAULT
//        )
        return builder.build()
    }
}
