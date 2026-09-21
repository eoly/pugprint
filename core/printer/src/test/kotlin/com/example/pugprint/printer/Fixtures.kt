package com.example.pugprint.printer

/**
 * Readers for the Phase 0 golden fixtures in `src/test/resources/print_job/`
 * (regenerate with `python spike/bleak/vendor_print.py --dump <pattern>`).
 */
object Fixtures {
    /** One `# label` + hex-line pair from a `.vendor.hex` file. */
    data class Part(
        val label: String,
        val bytes: ByteArray,
    )

    fun hex(bytes: ByteArray): String = bytes.joinToString(" ") { "%02x".format(it) }

    fun parseHex(text: String): ByteArray =
        text
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .map { it.toInt(16).toByte() }
            .toByteArray()

    /** Parses a `.vendor.hex` fixture: alternating `# label` and hex lines. */
    fun vendorJob(name: String): List<Part> {
        val lines = resource("print_job/$name.vendor.hex").lines().filter { it.isNotBlank() }
        require(lines.size % 2 == 0) { "$name.vendor.hex must alternate label/hex lines" }
        return lines.chunked(2).map { (label, hexLine) ->
            require(label.startsWith("# ")) { "expected a '# label' line, got: $label" }
            Part(label.removePrefix("# ").trim(), parseHex(hexLine))
        }
    }

    /** Parses a plain (P1, ASCII) PBM into packed rows: MSB-first, `1` = black, padded to whole bytes. */
    fun pbmRows(name: String): List<ByteArray> {
        val tokens =
            resource("print_job/$name.pbm")
                .lineSequence()
                .map { it.substringBefore('#') }
                .flatMap { it.split(Regex("\\s+")).asSequence() }
                .filter { it.isNotEmpty() }
                .toList()
        require(tokens[0] == "P1") { "$name.pbm is not a P1 bitmap" }
        val width = tokens[1].toInt()
        val height = tokens[2].toInt()
        val bits = tokens.drop(3)
        require(bits.size == width * height) { "$name.pbm: expected ${width * height} pixels, got ${bits.size}" }
        val stride = (width + 7) / 8
        return List(height) { y ->
            ByteArray(stride).also { row ->
                for (x in 0 until width) {
                    if (bits[y * width + x] == "1") {
                        row[x / 8] = (row[x / 8].toInt() or (0x80 ushr (x % 8))).toByte()
                    }
                }
            }
        }
    }

    private fun resource(path: String): String =
        checkNotNull(Fixtures::class.java.classLoader.getResourceAsStream(path)) { "missing test resource $path" }
            .bufferedReader()
            .use { it.readText() }
}
