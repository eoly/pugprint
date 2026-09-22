package com.example.pugprint.design.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.pugprint.design.components.BannerKind
import com.example.pugprint.design.components.BigButton
import com.example.pugprint.design.components.BigTextField
import com.example.pugprint.design.components.ButtonEmphasis
import com.example.pugprint.design.components.ChoiceRow
import com.example.pugprint.design.components.HeroTitle
import com.example.pugprint.design.components.KidScreen
import com.example.pugprint.design.components.StatusBanner
import com.example.pugprint.design.components.ThemePicker
import com.example.pugprint.design.theme.LocalPugTheme
import com.example.pugprint.design.theme.PugPrintTheme
import com.example.pugprint.design.theme.PugSpacing
import com.example.pugprint.design.theme.ThemeCatalog

/**
 * Every kit component on one screen, in the current theme. The per-theme goldens capture this,
 * so a new theme (or a changed component) shows up as a picture in the first PR.
 */
@Composable
fun DesignGallery(modifier: Modifier = Modifier) {
    val theme = LocalPugTheme.current
    var choice by remember { mutableStateOf("Square") }
    KidScreen(modifier = modifier, title = theme.displayName, onBack = {}, onHome = {}) {
        Column(verticalArrangement = Arrangement.spacedBy(PugSpacing.medium), modifier = Modifier.fillMaxWidth()) {
            HeroTitle("PugPrint")
            Text("Body text and a hint below it.", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Pinch to zoom, drag to move",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BigButton(text = "Print a photo", onClick = {})
            BigTextField(value = "", onValueChange = {}, placeholder = "Write something…")
            BigButton(text = "Print", onClick = {}, enabled = false)
            BigButton(text = "Print a test page", onClick = {}, emphasis = ButtonEmphasis.Secondary)
            Row { BigButton(text = "Forget printer", onClick = {}, emphasis = ButtonEmphasis.Quiet) }
            ChoiceRow(
                options = listOf("Square", "Tall", "Wide", "Whole"),
                selected = choice,
                onSelect = { choice = it },
                label = { it },
            )
            StatusBanner(BannerKind.Info, "HB-1234 is ready")
            StatusBanner(BannerKind.Working, "Printing…", progress = 0.4f)
            StatusBanner(BannerKind.Problem, "Close the lid and check the paper", hint = "Then tap Print again")
            StatusBanner(BannerKind.Success, "Printed!")
            ThemePicker(themes = ThemeCatalog.all, selectedId = theme.id, onSelect = {})
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun DesignGalleryPreview() {
    PugPrintTheme(ThemeCatalog.default) { DesignGallery() }
}
