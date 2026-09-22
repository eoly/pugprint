package com.example.pugprint.imaging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/** The rules every stamp in [StampCatalog.all] must follow; a new stamp is checked automatically. */
class StampCatalogTest {
    @Test
    fun `ids are unique, lowercase and never blank`() {
        val ids = StampCatalog.all.map { it.id }
        assertEquals(ids.toSet().size, ids.size, "duplicate stamp ids in $ids")
        ids.forEach { assertTrue(it.isNotBlank() && it == it.lowercase() && ' ' !in it, "bad stamp id '$it'") }
    }

    @Test
    fun `lookup by id, and a removed stamp is simply absent`() {
        StampCatalog.all.forEach { assertSame(it, StampCatalog.byId(it.id)) }
        assertNull(StampCatalog.byId("dragon"))
    }

    @TestFactory
    fun `every stamp has a name, some dots, and fits the size limit`(): List<DynamicTest> =
        StampCatalog.all.map { stamp ->
            DynamicTest.dynamicTest(stamp.id) {
                assertTrue(stamp.displayName.isNotBlank(), "${stamp.id} has no name")
                val art = stamp.art
                assertTrue(
                    art.width in 1..StampCatalog.MAX_SIZE && art.height in 1..StampCatalog.MAX_SIZE,
                    "${stamp.id} is ${art.width}x${art.height}",
                )
                var dots = 0
                for (y in 0 until art.height) for (x in 0 until art.width) if (art[x, y]) dots++
                assertTrue(dots >= art.width, "${stamp.id} is nearly empty ($dots dots)")
            }
        }

    @TestFactory
    fun `every stamp matches its golden at medium size`(): List<DynamicTest> =
        StampCatalog.all.map { stamp ->
            DynamicTest.dynamicTest(stamp.id) {
                Pbm.assertMatchesGolden("stamp_${stamp.id}", StampRasterizer.render(stamp, StampSize.MEDIUM))
            }
        }

    @Test
    fun `a rendered stamp is the art scaled up`() {
        val small = StampRasterizer.render(StampCatalog.Heart, StampSize.SMALL)
        assertEquals(16 * 4, small.width)
        assertEquals(16 * 4, small.height)
        assertFalse(small.isBlack(0, 0)) // top-left art dot is empty
        assertTrue(small.isBlack(2 * 4, 1 * 4)) // art (2,1) is a dot; scaled block starts at (8,4)
        assertTrue(small.isBlack(2 * 4 + 3, 1 * 4 + 3)) // ...and fills the whole 4x4 block
    }
}
