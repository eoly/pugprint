package com.example.pugprint.ui.home

/** What the home screen can ask for; wired to the ViewModel by `HomeRoute`. */
data class HomeActions(
    val onConnect: () -> Unit = {},
    val onPickPhoto: () -> Unit = {},
    val onPrintTestPage: () -> Unit = {},
    val onPrintAgain: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onForget: () -> Unit = {},
    val onMessageShown: () -> Unit = {},
    val onThemeSelected: (themeId: String) -> Unit = {},
)
