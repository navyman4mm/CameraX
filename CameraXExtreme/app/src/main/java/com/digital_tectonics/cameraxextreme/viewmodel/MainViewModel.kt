/*
 * Copyright (C) 2021 Digital Tectonics
 *
 * This application is the private property of Digital Tectonics
 * Any distribution of this software is unlawful and prohibited.
 */
package com.digital_tectonics.cameraxextreme.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.digital_tectonics.cameraxextreme.model.CameraXSetupData

/**
 * MainViewModel
 *
 * @author Daniel Randall on 2021-11-16.
 */
class MainViewModel : ViewModel() {

    private val _cameraPermission: MutableLiveData<Boolean> = MutableLiveData()
    val cameraPermission: LiveData<Boolean> get() = _cameraPermission
    private val _cameraSetupData: MutableLiveData<CameraXSetupData> = MutableLiveData(CameraXSetupData())
    val cameraSetupData: LiveData<CameraXSetupData> get() = _cameraSetupData

    fun allPermissionsGranted(context: Context) = REQUIRED_PERMISSIONS.all { item ->
        ContextCompat.checkSelfPermission(context, item) == PackageManager.PERMISSION_GRANTED
    }

    fun setCameraPermissionState(hasPermission: Boolean = false) {
        _cameraPermission.postValue(hasPermission)
    }

    fun setupCameraXData(cameraXSetupData: CameraXSetupData) {
        _cameraSetupData.postValue(cameraXSetupData)
    }

    companion object {
        // TODO: Review having this here
        val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
