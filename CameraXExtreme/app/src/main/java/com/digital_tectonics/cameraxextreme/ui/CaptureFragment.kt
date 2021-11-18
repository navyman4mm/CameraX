/*
 * Copyright (C) 2021 Digital Tectonics
 *
 * This application is the private property of Digital Tectonics
 * Any distribution of this software is unlawful and prohibited.
 */
package com.digital_tectonics.cameraxextreme.ui

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
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import com.digital_tectonics.cameraxextreme.constant.FILENAME_FORMAT
import com.digital_tectonics.cameraxextreme.constant.JPG_FILE_TAG
import com.digital_tectonics.cameraxextreme.extension.getOutputDirectory
import com.digital_tectonics.cameraxextreme.extension.logCameraExposureData
import com.digital_tectonics.cameraxextreme.extension.requestPermissionsFromUser
import com.digital_tectonics.cameraxextreme.extension.startCameraAndPreview
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
    private var imageCapture: ImageCapture? = null

    private var outputDirectory: File? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var isFirstResume: Boolean = true
    private var isCameraAvailable: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkCameraPermissions()

        outputDirectory = context?.getOutputDirectory()
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_capture, menu)
        super.onCreateOptionsMenu(menu, menuInflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_info -> {
                val cameraExposureData = imageCapture.logCameraExposureData(TAG)

                cameraExposureData?.run {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.camera_exposure_data_title)
                        .setItems(this.asArray(resources)) { dialog, which ->
                            // TODO: Allow for updates or settings?
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
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + JPG_FILE_TAG
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.logCameraExposureData(TAG)

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
            imageCapture = context.startCameraAndPreview(this, this@CaptureFragment)
            if (imageCapture == null) {
                Log.d(TAG, "Sad face - This is temp")
                startCamera()
            }
            imageCapture.logCameraExposureData(TAG)
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

            imageCapture = ImageCapture.Builder()
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
                    imageCapture,
                )
            } catch (exception: Exception) {
                Log.e(TAG, "Use case binding failed", exception)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }
}
