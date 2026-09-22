package com.example.pugprint.ui.home

import com.example.pugprint.design.components.BannerKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Every message has its own words, and every problem says what to do next. */
class HomeMessagesTest {
    @Test
    fun `every message has distinct text`() {
        val texts = HomeMessage.entries.map { it.banner().text }
        assertEquals(texts.toSet().size, texts.size, "two messages share a string")
    }

    @Test
    fun `every message tells the kid what to do next`() {
        HomeMessage.entries.forEach { message ->
            assertNotNull(message.banner().hint, "$message has no hint")
        }
    }

    @Test
    fun `print failures are problems and a finished print is a success`() {
        assertEquals(BannerKind.Success, HomeMessage.PrintDone.banner().kind)
        listOf(
            HomeMessage.PrintNoPaper,
            HomeMessage.PrintPaperOrLid,
            HomeMessage.PrintDisconnected,
            HomeMessage.PrintFailed,
        ).forEach { assertTrue(it.banner().kind == BannerKind.Problem, "$it should be a problem") }
    }
}
