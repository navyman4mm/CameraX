/*
 * Copyright (C) 2021 Digital Tectonics
 *
 * This application is the private property of Digital Tectonics
 * Any distribution of this software is unlawful and prohibited.
 */
package com.digital_tectonics.cameraxextreme.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.ImageCapture
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.digital_tectonics.cameraxextreme.R
import com.digital_tectonics.cameraxextreme.databinding.FragmentCaptureBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.fragment.app.activityViewModels
import com.digital_tectonics.cameraxextreme.extension.getOutputDirectory
import com.digital_tectonics.cameraxextreme.extension.requestPermissionsFromUser
import com.digital_tectonics.cameraxextreme.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.fragment_capture.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * CaptureFragment
 *
 * @author Daniel Randall on 2021-11-16.
 */
class CaptureFragment : Fragment(R.layout.fragment_capture) {
    val TAG: String = CaptureFragment::class.java.simpleName

    private val sharedViewModel: MainViewModel by activityViewModels()
    private var binding: FragmentCaptureBinding? = null
    private var imageCapture: ImageCapture? = null

    private var outputDirectory: File? = null
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request camera permissions
        if (sharedViewModel.allPermissionsGranted(requireContext())) {
            startCamera()
        } else {
            activity?.requestPermissionsFromUser()
        }

        outputDirectory = context?.getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCaptureBinding.inflate(inflater, container, false)
        // Set up the listener for take photo button
        binding?.captureCameraButton?.setOnClickListener { takePhoto() }

        sharedViewModel.cameraPermission.observe(viewLifecycleOwner, {
            if (it) {
                startCamera()
            }
        })

        return binding?.root
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
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

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
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
                    this, cameraSelector, preview, imageCapture)
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}
