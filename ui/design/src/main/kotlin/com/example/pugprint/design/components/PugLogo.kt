package com.example.pugprint.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.example.pugprint.design.R
import com.example.pugprint.design.theme.PugLayout

/**
 * The PugPrint pug printing a sticker: the still frame of the splash animation, the same art as
 * the launcher icon. Decorative by default (no content description) because it always sits next
 * to the app's name; pass [contentDescription] when it stands alone.
 */
@Composable
fun PugLogo(
    modifier: Modifier = Modifier,
    size: Dp = PugLayout.heroLogo,
    contentDescription: String? = null,
) {
    Image(
        painter = painterResource(R.drawable.pug_logo),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
    )
}
