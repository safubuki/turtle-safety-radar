package com.turtlesafety.radar.ime

import android.text.InputType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeFieldSafetyTest {

    @Test
    fun `password text field is skipped`() {
        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        assertTrue(ImeFieldSafety.shouldSkipInspection(inputType))
    }

    @Test
    fun `number password field is skipped`() {
        val inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        assertTrue(ImeFieldSafety.shouldSkipInspection(inputType))
    }

    @Test
    fun `normal text field is not skipped`() {
        val inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
        assertFalse(ImeFieldSafety.shouldSkipInspection(inputType))
    }
}