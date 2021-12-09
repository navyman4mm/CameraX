/*
 * Copyright (C) 2021 Digital Tectonics
 *
 * This application is the private property of Digital Tectonics
 * Any distribution of this software is unlawful and prohibited.
 */
package com.digital_tectonics.cameraxextreme.constant

/**
 * ExposureLevel
 * Used to adjust the Exposure level based on the actual exposure value (EV) range
 *  supported by the camera.
 *
 *
 * @author Daniel Randall on 2021-12-02.
 */
enum class ExposureLevel(exposureValue: Int) {
    DARK_EV_MAX(-5),
    DARK_WHOLE_EV(-3),
    DARK_EV_STEP(-1),
    NETURAL_EV(0),
    LIGHT_EV_STEP(1),
    LIGHT_WHOLE_EV(3),
    LIGHT_EV_MAX(5)
}
