/*
 * Copyright (C) 2021 Digital Tectonics
 *
 * This application is the private property of Digital Tectonics
 * Any distribution of this software is unlawful and prohibited.
 */
package com.digital_tectonics.cameraxextreme

import android.Manifest
import android.content.Context
import android.graphics.ImageFormat
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.*
import org.junit.runner.RunWith
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * CameraPreviewTest - Handles ensuring that the CameraX Preview setup will work in general
 *
 * NOTES:
 * 1.) Double Bangs (!!) are used to ensure the Test crash if it would have received a null value
 *
 * @author Daniel Randall on 2021-11-22.
 */
@RunWith(AndroidJUnit4::class)
class CameraPreviewTest : LifecycleOwner, ImageReader.OnImageAvailableListener,
    Consumer<SurfaceRequest.Result> {

    val TAG = CameraPreviewTest::class.java.simpleName

    @get:Rule
    val cameraAccess = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    private val IMAGEREADR_STANDARD_WIDTH = 1920
    private val IMAGEREADR_STANDARD_HEIGHT = 1080
    private val IMAGEREADR_STANDARD_MAX_IMAGES = 30

    private var registry: LifecycleRegistry? = null
    private val thread = HandlerThread(TAG).also { it.start() }
    private var executor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    /**
     * @implNote In checkPreviewUseCase, ImageReader will provide a Surface for camera preview test.
     *  When each ImageProxy is acquired, the AtomicInteger will be incremented.
     *  By doing so we can ensure the camera binding is working as expected.
     */
    private val imageReader = ImageReader.newInstance(
        IMAGEREADR_STANDARD_WIDTH,
        IMAGEREADR_STANDARD_HEIGHT,
        ImageFormat.YUV_420_888,
        IMAGEREADR_STANDARD_MAX_IMAGES,
    )
    private val count = AtomicInteger(0)

    /**
     * @implNote We can't use the main executor since it is reserved for the test framework.
     */
    @Before
    fun setup() {
        val context: Context = ApplicationProvider.getApplicationContext()
        Assert.assertNotNull(context)
        cameraProvider = ProcessCameraProvider.getInstance(context).get()
        Assert.assertNotNull(cameraProvider)
    }

    @Before
    fun setupImageReader() {
        imageReader.setOnImageAvailableListener(this, Handler(thread.looper))
    }

    @UiThreadTest
    @Before
    fun markCreated() {
        registry = LifecycleRegistry(this).also {
            it.markState(Lifecycle.State.INITIALIZED)
            it.markState(Lifecycle.State.CREATED)
        }
    }

    @UiThreadTest
    @After
    fun teardown() {
        cameraProvider?.unbindAll()
        executor?.shutdown()
    }

    @After
    fun teardownImageReader() {
        imageReader.close()
        thread.quit()
    }

    @UiThreadTest
    @After
    fun markDestroyed() {
        registry?.markState(Lifecycle.State.DESTROYED)
    }

    override fun onImageAvailable(reader: ImageReader) {
        reader.acquireNextImage().use { image ->
            Log.i(TAG, String.format("Image Number & Image: %d %s", count.getAndIncrement(), image))
        }
    }

    /**
     * @see ProcessCameraProvider.bindToLifecycle
     */
    override fun getLifecycle() = registry!!

    /**
     * @see SurfaceRequest.provideSurface
     */
    override fun accept(result: SurfaceRequest.Result) {
        when (result.resultCode) {
            SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY -> {
                Log.i(TAG, result.toString())
            }
            SurfaceRequest.Result.RESULT_REQUEST_CANCELLED, SurfaceRequest.Result.RESULT_INVALID_SURFACE, SurfaceRequest.Result.RESULT_SURFACE_ALREADY_PROVIDED, SurfaceRequest.Result.RESULT_WILL_NOT_PROVIDE_SURFACE -> {
                Log.e(TAG, result.toString())
            }
        }
    }

    @UiThreadTest
    @Test
    fun checkBackCameraPreviewUseCase() {
        // Setup Lifecycle owner state
        registry?.markState(Lifecycle.State.STARTED)

        // fit the preview size to ImageReader
        val preview = Preview.Builder()
            .setTargetResolution(Size(imageReader.width, imageReader.height))
            .setTargetRotation(Surface.ROTATION_90)
            .build()

        // Request camera binding
        cameraProvider!!.unbindAll()
        val camera = cameraProvider!!.bindToLifecycle(
            this,
            setupWhichCamera(),
            preview,
        )
        // Check the Camera has been provided
        Assert.assertNotNull(camera)
        preview.setSurfaceProvider(executor!!, { request: SurfaceRequest ->
            Log.i(TAG, String.format("Providing: %s", imageReader.surface))
            request.provideSurface(imageReader.surface, executor!!, this)
        })

        repeatCheckAtInterval()
        Assert.assertNotEquals(0, count.get().toLong())
    }

    /**
     * Checks if the selected camera is available
     */
    private fun setupWhichCamera(selectedCamera: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA): CameraSelector {
        Assert.assertTrue(cameraProvider!!.hasCamera(selectedCamera))
        return CameraSelector.Builder()
            .requireLensFacing(
                if (selectedCamera == CameraSelector.DEFAULT_BACK_CAMERA) {
                    CameraSelector.LENS_FACING_BACK
                } else {
                    CameraSelector.LENS_FACING_FRONT
                }
            )
            .build()
    }

    /**
     * Delay until onImageAvailable is invoked.
     * Retry several times
     * @param timeInterval [Long] default - 500 milliseconds apart
     */
    private fun repeatCheckAtInterval(repeatAttempts: Int = 5, timeInterval: Long = 500L) {
        for (repeat in repeatAttempts downTo 0) {
            Thread.sleep(timeInterval)
            Log.i(TAG, String.format("Image Count: %d", count.get()))
            if (count.get() > 0) {
                return
            }
        }
    }
}
