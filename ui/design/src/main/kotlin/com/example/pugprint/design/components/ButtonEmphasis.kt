package com.example.pugprint.design.components

import com.example.pugprint.design.theme.PugTouch

/** How much a button should shout. */
enum class ButtonEmphasis {
    /** The one big thing to do on this screen: filled, [PugTouch.primary] tall. */
    Primary,

    /** Another thing to do: tonal, [PugTouch.secondary] tall. */
    Secondary,

    /** A small link such as "Forget printer": plain text, still [PugTouch.minimum] tall. */
    Quiet,
}
