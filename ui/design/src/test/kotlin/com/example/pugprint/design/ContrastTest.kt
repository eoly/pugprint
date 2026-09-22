package com.example.pugprint.design

import androidx.compose.ui.graphics.Color
import com.example.pugprint.design.theme.Contrast
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ContrastTest {
    @Test
    fun `black on white is 21 to 1`() {
        assertEquals(21.0, Contrast.ratio(Color.Black, Color.White), 0.01)
    }

    @Test
    fun `same colour is 1 to 1`() {
        assertEquals(1.0, Contrast.ratio(Color(0xFF8C5A2B), Color(0xFF8C5A2B)), 0.0001)
    }

    @Test
    fun `order of the two colours does not matter`() {
        assertEquals(
            Contrast.ratio(Color(0xFF8C5A2B), Color.White),
            Contrast.ratio(Color.White, Color(0xFF8C5A2B)),
            0.0001,
        )
    }

    @Test
    fun `matches the WCAG reference value for a mid grey`() {
        // #767676 on white is the classic "just passes AA" grey: 4.54:1.
        assertEquals(4.54, Contrast.ratio(Color(0xFF767676), Color.White), 0.01)
    }
}
