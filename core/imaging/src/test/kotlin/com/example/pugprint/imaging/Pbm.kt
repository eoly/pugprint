package com.example.pugprint.imaging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File

/** Plain-text PBM (P1) serialisation shared by the golden tests. */
object Pbm {
    fun encode(bitmap: MonoBitmap): String =
        buildString {
            append("P1\n${bitmap.width} ${bitmap.height}\n")
            for (y in 0 until bitmap.height) {
                for (x in 0 until bitmap.width) append(if (bitmap.isBlack(x, y)) '1' else '0')
                append('\n')
            }
        }

    /**
     * Asserts [bitmap] equals `src/test/resources/<name>.pbm`. Re-record after an intentional
     * change with `./gradlew :core:imaging:test -Dpugprint.recordGoldens=true` and review the diff.
     */
    fun assertMatchesGolden(
        name: String,
        bitmap: MonoBitmap,
    ) {
        val golden = File("src/test/resources/$name.pbm")
        val rendered = encode(bitmap)
        if (System.getProperty("pugprint.recordGoldens") == "true") {
            golden.parentFile.mkdirs()
            golden.writeText(rendered)
        }
        assertTrue(golden.exists(), "missing golden ${golden.path}; record with -Dpugprint.recordGoldens=true")
        assertEquals(golden.readText(), rendered)
    }
}
