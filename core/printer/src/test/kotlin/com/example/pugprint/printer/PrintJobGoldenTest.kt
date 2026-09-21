package com.example.pugprint.printer

import com.example.pugprint.printer.Fixtures.hex
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Byte-exact comparison against the jobs the Phase 0 spike printed successfully on hardware
 * (the `print_job` `.vendor.hex` test resources: density 25, one row per block).
 */
class PrintJobGoldenTest {
    @ParameterizedTest
    @ValueSource(strings = ["black_48_rows", "stripe_48_rows", "widths_48_rows", "pug_arrow"])
    fun `job bytes and labels match the vendor fixture`(name: String) {
        val expected = Fixtures.vendorJob(name)
        val rows = Fixtures.pbmRows(name)

        val actual = PrintJob(rows, density = DensityProfile.PUBLIC.value(DensityLevel.MEDIUM)).writes()

        assertEquals(expected.map { it.label }, actual.map { it.label })
        expected.zip(actual).forEach { (want, got) ->
            assertEquals(hex(want.bytes), hex(got.bytes), "bytes differ at '${want.label}'")
        }
    }

    @Test
    fun `pacing follows the confirmed BLE recipe`() {
        val rows = Fixtures.pbmRows("black_48_rows")
        val writes = PrintJob(rows, density = 25).writes()
        val blocks = writes.filter { it.label.startsWith("raster block") }

        assertEquals(48, blocks.size)
        assertEquals(List(47) { 20L }, blocks.dropLast(1).map { it.pauseAfterMillis })
        assertEquals(250L, blocks.last().pauseAfterMillis)
        assertEquals(0L, writes.first().pauseAfterMillis)
        assertEquals(0L, writes.last().pauseAfterMillis)
        assertEquals(56, blocks.maxOf { it.bytes.size })
    }

    @Test
    fun `options add speed and copies in the vendor order`() {
        val rows = listOf(ByteArray(PrinterSpec.BYTES_PER_ROW))
        val profile = DensityProfile.PRIVATE_OLD
        val job =
            PrintJob(
                rows,
                density = profile.value(DensityLevel.DARK),
                options = PrintOptions(copies = 3, speed = profile.legacySpeed, feedLines = 2),
            )

        val writes = job.writes()

        assertEquals(listOf("density", "speed", "copies", "init", "raster block 1/1", "feed"), writes.map { it.label })
        assertEquals("1d 49 f0 0f", hex(writes[0].bytes))
        assertEquals("1d 49 f1 1e", hex(writes[1].bytes))
        assertEquals("1d 49 f8 03", hex(writes[2].bytes))
        assertEquals("0a 0a", hex(writes.last().bytes))
    }
}
