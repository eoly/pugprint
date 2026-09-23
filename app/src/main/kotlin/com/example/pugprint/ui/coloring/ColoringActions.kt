package com.example.pugprint.ui.coloring

/** What the coloring picker can ask for; wired to the ViewModel by `ColoringRoute`. */
data class ColoringActions(
    val onBack: () -> Unit = {},
    val onHome: () -> Unit = {},
    val onPick: (pageId: String) -> Unit = {},
)
