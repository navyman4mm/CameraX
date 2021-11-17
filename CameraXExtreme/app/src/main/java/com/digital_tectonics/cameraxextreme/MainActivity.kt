/*
 * Copyright (C) 2021 Digital Tectonics
 *
 * This application is the private property of Digital Tectonics
 * Any distribution of this software is unlawful and prohibited.
 */
package com.digital_tectonics.cameraxextreme

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.digital_tectonics.cameraxextreme.databinding.ActivityMainBinding
import android.util.Log
import androidx.activity.viewModels
import com.digital_tectonics.cameraxextreme.extension.requestPermissionsFromUser
import com.digital_tectonics.cameraxextreme.viewmodel.MainViewModel

/**
 * MainActivity
 *
 * @author Daniel Randall on 2021-11-16.
 */
class MainActivity : AppCompatActivity() {
    val TAG: String = MainActivity::class.java.simpleName

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val sharedViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            Log.d(TAG, "FAB tapped")
//            Snackbar.make(view, R.string.photo_taken_action, Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
            || super.onSupportNavigateUp()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (sharedViewModel.allPermissionsGranted(baseContext)) {
                sharedViewModel.setCameraPermissionState(true)
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    companion object {
        const val REQUEST_CODE_PERMISSIONS = 10
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}
