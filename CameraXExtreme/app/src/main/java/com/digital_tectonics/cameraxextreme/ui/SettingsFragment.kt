/*
 * Copyright (C) 2021 Digital Tectonics
 *
 * This application is the private property of Digital Tectonics
 * Any distribution of this software is unlawful and prohibited.
 */
package com.digital_tectonics.cameraxextreme.ui

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat
import androidx.camera.camera2.internal.compat.CameraManagerCompat
import androidx.fragment.app.activityViewModels
import com.digital_tectonics.cameraxextreme.R
import com.digital_tectonics.cameraxextreme.databinding.FragmentSettingsBinding
import com.digital_tectonics.cameraxextreme.extension.requestPermissionsFromUser
import com.digital_tectonics.cameraxextreme.viewmodel.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * SettingsFragment
 *
 * @author Daniel Randall on 2021-11-16.
 */
class SettingsFragment : Fragment() {
    val TAG: String = SettingsFragment::class.java.simpleName

    private val sharedViewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentSettingsBinding? = null

    // This property is only valid between [onCreateView] and [onDestroyView]
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.settingsRawSupportTestButton.setOnClickListener {
            if (sharedViewModel.allPermissionsGranted(requireContext())) {
                checkCamerasForRAWSupport()
            } else {
                activity?.requestPermissionsFromUser()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("ServiceCast", "RestrictedApi")
    private fun checkCamerasForRAWSupport() {
        val cameraManager: CameraManager = activity?.applicationContext?.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        var tempCameraCharacteristics: CameraCharacteristics?
        for (cameraId in cameraManager.cameraIdList) {
            tempCameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.raw_test_title, cameraId))
                .setMessage("Supports RAW: ${tempCameraCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)}")
                .show()
        }
    }
}
