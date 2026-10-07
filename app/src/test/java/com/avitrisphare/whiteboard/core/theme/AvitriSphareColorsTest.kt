package com.avitrisphare.whiteboard.core.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test


class AvitriSphareColorsTest {

    @Test
    fun `dark palette values are correct`() {
        val dark = AvitriSphareColors.Dark
        assertEquals(Color(0xFF0F0F0F), dark.backgroundDark)
        assertEquals(Color(0xFF3ECF8E), dark.brandGreen)
        assertEquals(Color(0xFFFAFAFA), dark.textPrimaryDark)
        assertEquals(Color(0xFF242424), dark.borderSubtle)
        // Derived properties should resolve to dark values
        assertEquals(dark.backgroundDark, dark.background)
        assertEquals(dark.textPrimaryDark, dark.textPrimary)
        assertEquals(dark.borderStandardDark, dark.borderStandard)
    }

    @Test
    fun `light palette values are correct`() {
        val light = AvitriSphareColors.Light
        assertEquals(Color(0xFFF8F9FA), light.backgroundLight)
        assertEquals(Color(0xFF3ECF8E), light.brandGreen)
        assertEquals(Color(0xFF171717), light.textPrimaryLight)
        assertEquals(Color(0xFFE9ECEF), light.borderSubtle)
        // Derived properties should resolve to light values
        assertEquals(light.backgroundLight, light.background)
        assertEquals(light.textPrimaryLight, light.textPrimary)
        assertEquals(light.borderStandardLight, light.borderStandard)
    }

    @Test
    fun `backward compatibility getters match dark theme`() {
        assertEquals(AvitriSphareColors.Dark.backgroundDark, AvitriSphareColors.BackgroundDark)
        assertEquals(AvitriSphareColors.Dark.brandGreen, AvitriSphareColors.BrandGreen)
        assertEquals(AvitriSphareColors.Dark.textPrimaryDark, AvitriSphareColors.TextPrimary)
        assertEquals(AvitriSphareColors.Dark.borderStandardDark, AvitriSphareColors.BorderStandard)
    }

    @Test
    fun `shapes match design specifications`() {
        assertEquals(CornerSize(9999.dp), AvitriSphareShapes.PillShape.topStart)
        assertEquals(CornerSize(16.dp), AvitriSphareShapes.CardShape.topStart)
        assertEquals(CornerSize(6.dp), AvitriSphareShapes.SmallShape.topStart)
    }
}

