# The design kit — a handbook for PugPrint's designer

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
│   └── StatusBanner.kt   ← Info / Working / Problem / Success cards
└── gallery/DesignGallery.kt ← every component on one screen, used for the pictures below
```

## Add a theme (10 minutes)
1. Open `theme/ThemeCatalog.kt`. Copy the `Ocean` block, rename it, give it a new `id`
   (lowercase, no spaces, never changed once shipped), a `displayName`, an `emoji`, twelve
   colours and a `roundness`.
2. Add it to the `all` list. That is the only registration there is.
3. Run `./gradlew :ui:design:testDebugUnitTest :ui:design:recordRoborazziDebug`.
   - The readability test tells you if a text colour is too faint for its background
     (it names the theme and the pair, e.g. `ocean: outline on background is 1.88:1, needs 3.0:1`).
     Darken the text or lighten the background until it passes.
   - A picture of your theme appears at `ui/design/screenshots/DesignGallery.<id>.png`.
4. Commit the new picture with the code.

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

## Rules the tests enforce
- Theme ids are unique, lowercase and not blank; every theme has a name and an emoji.
- Every text/background pair in the palette meets WCAG AA (4.5:1; 3:1 for outlines).
- All palette colours are opaque.
- A gallery golden exists per theme (recorded automatically).

## Coming next (Phase 5)
A theme picker on the home screen, stamps (`StampCatalog`: drop in a bitmap, add a line),
text captions with a bitmap font, and a drawing canvas — each following the same
"one catalog file, enumerated by tests" pattern.
