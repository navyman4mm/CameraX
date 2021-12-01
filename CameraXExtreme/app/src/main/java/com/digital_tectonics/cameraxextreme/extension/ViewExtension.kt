/*
 * Copyright (C) 2021 Digital Tectonics
 *
 * This application is the private property of Digital Tectonics
 * Any distribution of this software is unlawful and prohibited.
 */
package com.digital_tectonics.cameraxextreme.extension

import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener

/**
 * ViewExtension
 *
 * @author Daniel Randall on 2021-12-01.
 */

/**
 * Used to determine that a view has already been measured and then run code afterward.
 */
inline fun View.afterViewMeasured(crossinline methodBlock: () -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (measuredWidth > 0 && measuredHeight > 0) {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                methodBlock()
            }
        }
    })
}
