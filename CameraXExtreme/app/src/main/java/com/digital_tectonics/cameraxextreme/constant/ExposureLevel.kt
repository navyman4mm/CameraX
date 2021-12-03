/*
 * Copyright (C) 2021 Digital Tectonics
 *
 * This application is the private property of Digital Tectonics
 * Any distribution of this software is unlawful and prohibited.
 */
package com.digital_tectonics.cameraxextreme.constant

/**
 * ExposureLevel
 *
 * @author Daniel Randall on 2021-12-02.
 */
enum class ExposureLevel(exposureValue: Int) {
    DARK_MAX(-5),
    DARK_STEP(-1),
    NETURAL(0),
    LIGHT_STEP(1),
    LIGHT_MAX(5)
}
