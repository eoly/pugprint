package com.example.pugprint.printer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class DensityProfileTest {
    @Test
    fun `reference unit (public, id 801, fw 101) uses 20 25 30 and no speed command`() {
        val profile = DensityProfile.forPrinter(FactoryType.fromProductId(801), firmwareNumber = 101)

        assertSame(DensityProfile.PUBLIC, profile)
        assertEquals(20, profile.value(DensityLevel.LIGHT))
        assertEquals(25, profile.value(DensityLevel.MEDIUM))
        assertEquals(30, profile.value(DensityLevel.DARK))
        assertNull(profile.legacySpeed)
    }

    @Test
    fun `private units split on firmware 119`() {
        val old = DensityProfile.forPrinter(FactoryType.PRIVATE, firmwareNumber = 118)
        val new = DensityProfile.forPrinter(FactoryType.PRIVATE, firmwareNumber = 119)

        assertSame(DensityProfile.PRIVATE_OLD, old)
        assertEquals(listOf(5, 10, 15), DensityLevel.entries.map(old::value))
        assertEquals(0x1E, old.legacySpeed)

        assertSame(DensityProfile.PRIVATE_NEW, new)
        assertEquals(listOf(12, 15, 18), DensityLevel.entries.map(new::value))
        assertNull(new.legacySpeed)
    }

    @Test
    fun `factory type is public from product id 200`() {
        assertEquals(FactoryType.PRIVATE, FactoryType.fromProductId(199))
        assertEquals(FactoryType.PUBLIC, FactoryType.fromProductId(200))
    }
}
