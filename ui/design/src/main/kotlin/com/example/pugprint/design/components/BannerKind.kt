package com.example.pugprint.design.components

/** What a [StatusBanner] is telling the kid. */
enum class BannerKind {
    /** Plain news: "HB-1234 is ready". */
    Info,

    /** Something is happening: connecting, printing. Shows a spinner or the [StatusBanner] progress. */
    Working,

    /** Something needs fixing: lid open, battery low. */
    Problem,

    /** All done: "Printed!". */
    Success,
}
