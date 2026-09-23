# The design kit — a handbook for PugPrint's designer

## Start here
1. Install the debug app on the phone (`./gradlew installDebug`) and tap **Design gallery** at
   the bottom of the home screen. Every button, tile and banner is there in one place, and the
   "Pick a look" row at the bottom switches the theme without changing anything for real.
2. Pick the thing you want to change from the list below. Each one is a single file, and each
   has a test that tells you if something is wrong and a picture that shows you the result.
3. Run the command in the section, look at the picture, and commit it together with the change.

Everything about how PugPrint *looks* lives in one place: the `ui/design` folder (the "design
kit"). Printing, Bluetooth and picture code never change when the look changes, and the look
never has to know about them. See ADR 0007 for why.

```
ui/design/src/main/kotlin/com/example/pugprint/design/
├── theme/
│   ├── ThemeCatalog.kt   ← every theme (colours + roundness). ADD THEMES HERE.
│   ├── PugTheme.kt       ← what a theme is made of (the palette fields, with what each is for)
│   ├── Tokens.kt         ← spacing, touch sizes, tablet width
│   ├── PugPrintTheme.kt  ← turns a theme into Material (you rarely touch this)
│   └── Contrast.kt       ← the readability maths the tests use
├── components/           ← the building blocks every screen uses
│   ├── KidScreen.kt      ← the frame: back button + title + padding (and HeroTitle)
│   ├── BigButton.kt      ← Primary / Secondary / Quiet buttons
│   ├── ChoiceRow.kt      ← "pick one" tiles (Square / Tall / Wide / Whole)
│   ├── StatusBanner.kt   ← Info / Working / Problem / Success cards
│   ├── ThemePicker.kt    ← "Pick a look": one tile per theme, painted in that theme's colours
│   └── BigTextField.kt   ← one line of big typing (the "Add words" box)
└── gallery/DesignGallery.kt ← every component on one screen, used for the pictures below
```

## Add a theme (10 minutes)
1. Open `theme/ThemeCatalog.kt`. Copy the `Ocean` block, rename it, give it a new `id`
   (lowercase, no spaces, never changed once shipped), a `displayName`, twelve colours and a
   `roundness`.
2. Add it to the `all` list. That is the only registration there is.
3. Run `./gradlew :ui:design:testDebugUnitTest :ui:design:recordRoborazziDebug`.
   - The readability test tells you if a text colour is too faint for its background
     (it names the theme and the pair, e.g. `ocean: outline on background is 1.88:1, needs 3.0:1`).
     Darken the text or lighten the background until it passes.
   - A picture of your theme appears at `ui/design/screenshots/DesignGallery.<id>.png`.
4. Commit the new picture with the code. The theme shows up in "Pick a look" on the home
   screen by itself: the picker reads the catalog.

### What each colour is for
| Field | Used for |
|---|---|
| `background` | behind everything |
| `card` | banners, unselected choice tiles, second-choice buttons |
| `text` / `textSoft` | words / hints |
| `primary` / `onPrimary` | the big button and its words; the selected tile's border |
| `primarySoft` | the selected tile's fill, the "Printed!" banner |
| `secondary` / `onSecondary` | reserved for accents (stamps, drawing tools) |
| `outline` | thin lines around the preview and tiles |
| `error` / `errorSoft` | "fix this" banners |

## Add a font for stickers (20 minutes)
Fonts live in the picture code, not the kit: `core/imaging/src/main/kotlin/com/example/pugprint/imaging/FontCatalog.kt`.
1. Copy the `Blocky` block, rename it, give it a new `id` and `displayName`, and pick a
   `glyphHeight` (Blocky is 7 rows).
2. Draw every letter as rows of `#` (dot) and `.` (no dot), all the same height, any width.
   You need at least A–Z, 0–9, space and `. , ! ? ' -`; the test tells you what is missing.
3. Add it to `all`, then run `./gradlew :core:imaging:test -Dpugprint.recordGoldens=true`.
   A sample sentence appears at `core/imaging/src/test/resources/font_<id>.pbm` (any image
   viewer opens it). Commit it with the font.

## Add a stamp (10 minutes)
Stamps live next to the fonts: `core/imaging/src/main/kotlin/com/example/pugprint/imaging/StampCatalog.kt`.
1. Copy the `Heart` block, rename it, give it a new `id` and `displayName`.
2. Draw it as 16 rows of 16 `#`/`.` characters (up to 32 × 32 works). Solid shapes print best;
   the app draws a white outline around every stamp so it shows up on a photo.
3. Add it to `all`, then run `./gradlew :core:imaging:test -Dpugprint.recordGoldens=true`.
   Your stamp appears at `core/imaging/src/test/resources/stamp_<id>.pbm` and in the
   "Add stamps" picker. Commit the picture with the stamp.

## Change how big or round things are
- Corners: `roundness` on each theme (buttons, tiles and banners all follow it).
- Spacing: `PugSpacing` in `Tokens.kt`.
- Touch sizes: `PugTouch` — 64 dp for the one big thing, 56 dp for other buttons and tiles,
  48 dp minimum. Nothing tappable may go below 48 dp.
- Tablet: `PugLayout.maxContentWidth` keeps the buttons hand-sized on a wide screen.

## Change a component's look
Edit the file in `components/`; every screen picks it up. Then re-record the pictures
(`./gradlew recordRoborazziDebug`) and look at the diffs in `ui/design/screenshots/` and
`app/screenshots/` before committing.

## Rules for anything a kid can tap
- It says what it is: a label, or a `contentDescription` for a picture. TalkBack reads it.
- It is at least 48 dp tall and wide (`PugTouch.minimum`); the big things are 56 or 64 dp.
- Labels fit at the "Largest" font setting: `ChoiceRow` and `ThemePicker` shrink long words,
  and screens that can grow tall (home) scroll (`KidScreen(scrollable = true)`).
`AccessibilityAuditTest` in `app/src/test` checks the first two on every screen; the
`*_bigText` goldens show the third.

## Rules the tests enforce
- Theme ids are unique, lowercase and not blank; every theme has a name.
- Every text/background pair in the palette meets WCAG AA (4.5:1; 3:1 for outlines).
- All palette colours are opaque.
- A gallery golden exists per theme (recorded automatically).

## Add a sticker roll (needs a ruler and the printer)
Rolls live at `core/imaging/src/main/kotlin/com/example/pugprint/imaging/StickerRoll.kt`
(`StickerRollCatalog`). Copy the `SquareStandard` block (or `CircleStandard` for round labels),
give it an `id`, a `displayName` and the label's width and height in mm; for a round label add
`shape = LabelShape.CIRCLE` (width and height must match; the picture is a circle as wide as the
head minus the left and right insets, so shrink it with the insets if the printer starts too low). Print one sticker, measure the white
gaps on each side, and set `PrintPlacement`: white rows to add on top and dots to leave free on
the right (the head cannot print further left or higher than it does). Add it to `all`; it
appears under "Which stickers are in the printer?" by itself. A round roll gets the circle guide
on the crop frame and the draw sheet, and its words become a cap inside the curve, for free.

## Brush sizes
The pen sizes on "Draw a sticker" are `BrushSize` in
`core/imaging/src/main/kotlin/com/example/pugprint/imaging/Drawing.kt`, in print dots
(8 dots = 1 mm). Change a number or add a size; the screen's tiles follow the list.

## Change the words the app says
Every label and message is in `app/src/main/res/values/strings.xml`, one line each, in plain
English ("Print a photo", "Peel it off — or tap Print it again for another"). Change the words
between the tags; leave the `name="…"` part alone. Keep them short: labels shrink to fit but
a long one gets small. `./gradlew recordRoborazziDebug` shows the new words on every screen.

## See it live
Debug builds have a **Design gallery** button at the bottom of the home screen. It shows every
kit component in the theme you choose there (a preview: the kid's saved look is untouched).
Release builds never show it.