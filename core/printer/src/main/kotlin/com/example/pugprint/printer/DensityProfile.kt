package com.example.pugprint.printer

/** User-facing heat setting; the wire value depends on the printer, see [DensityProfile]. */
public enum class DensityLevel { LIGHT, MEDIUM, DARK }

/**
 * Which firmware table the printer was built against. The vendor app derives it from the
 * product id in the `GS g 0x69` reply: ids ≥ [FactoryType.PUBLIC_ID_THRESHOLD] are "public".
 */
public enum class FactoryType {
    PUBLIC,
    PRIVATE,
    ;

    public companion object {
        /** Product ids at or above this value are [PUBLIC]. */
        public const val PUBLIC_ID_THRESHOLD: Int = 200

        public fun fromProductId(productId: Int): FactoryType =
            if (productId >= PUBLIC_ID_THRESHOLD) PUBLIC else PRIVATE
    }
}

/**
 * `GS I 0xF0` values per [DensityLevel] for one firmware family
 * (docs/PRINTER_PROTOCOL.md § Print sequence, "Density values in the app").
 *
 * @property legacySpeed value for [PrinterCommands.speed], or `null` when the family needs none.
 */
public class DensityProfile private constructor(
    public val name: String,
    private val light: Int,
    private val medium: Int,
    private val dark: Int,
    public val legacySpeed: Int?,
) {
    public fun value(level: DensityLevel): Int =
        when (level) {
            DensityLevel.LIGHT -> light
            DensityLevel.MEDIUM -> medium
            DensityLevel.DARK -> dark
        }

    override fun toString(): String = "DensityProfile($name: $light/$medium/$dark, speed=$legacySpeed)"

    public companion object {
        /** Public-factory units such as the reference printer (product id 801). 25 is the confirmed default. */
        public val PUBLIC: DensityProfile =
            DensityProfile("public", light = 20, medium = 25, dark = 30, legacySpeed = null)

        /** Private-factory units with firmware number ≥ [NEW_FIRMWARE_THRESHOLD]. */
        public val PRIVATE_NEW: DensityProfile =
            DensityProfile("private-new", light = 12, medium = 15, dark = 18, legacySpeed = null)

        /** Private-factory units with older firmware; these also need a speed command. */
        public val PRIVATE_OLD: DensityProfile =
            DensityProfile(
                "private-old",
                light = 5,
                medium = 10,
                dark = 15,
                legacySpeed = PrinterCommands.LEGACY_SPEED,
            )

        /** Firmware number (e.g. `V1.19` → 119) from which private units use [PRIVATE_NEW]. */
        public const val NEW_FIRMWARE_THRESHOLD: Int = 119

        /** Chooses the profile the vendor app would use for a printer that reported [factory] and [firmwareNumber]. */
        public fun forPrinter(
            factory: FactoryType,
            firmwareNumber: Int,
        ): DensityProfile =
            when {
                factory == FactoryType.PUBLIC -> PUBLIC
                firmwareNumber >= NEW_FIRMWARE_THRESHOLD -> PRIVATE_NEW
                else -> PRIVATE_OLD
            }
    }
}
