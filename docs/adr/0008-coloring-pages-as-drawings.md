# 8. Coloring pages are drawings in a catalog
Date: 2026-09-23 · Status: Accepted

## Context
Kids want stickers they can colour in with crayons: simple pictures with thick black outlines
and white inside. The app already has a sticker document (ADR 0007) where content is either a
*picture* (photo or a kid's `Drawing`) or a *layer* on top (caption, stamps), and every kind of
kid content lives in one catalog file enumerated by tests. A coloring page is new content that
needs a home, a file format and a way to reach the printer.

Three ways to keep the pictures were considered:
- **Bitmap assets** (PNG in `:app` resources): easy to draw in any tool, but Android-only
  (no goldens in `:core:imaging`), a fixed 384-dot size that does not follow a roll's width or a
  round label, and binary diffs nobody can review.
- **`#`/`.` pixel art** like stamps and fonts: reviewable and pure JVM, but a page needs a
  384-dot picture and a 48 × 48 glyph scaled 8× prints jagged, blocky outlines that colour badly.
- **Vector strokes** — the same `Drawing` (round-capped polylines in page fractions) a kid makes
  on the draw sheet, rasterised by `StrokeRasterizer`.

## Decision
1. **A coloring page is a `Drawing`.** `ColoringPage(id, displayName, drawing)` lives in
   `:core:imaging`; `ColoringPageCatalog.all` is the one file to add a page to (ADR 0007's
   "add a thing = add a line" rule). Pages are authored with a small `Outline` builder
   (`circle`, `ellipse`, `arc`, `path`, `loop`, `star`, `dot`) in fractions of the page, with
   `BrushSize` picking the line weight. No pixels, no PNGs, no Android.
2. **A page prints as a picture, through the drawing path.** A picked page opens on the draw
   sheet as its starting `Drawing` (Undo and Start over stop at the outline), and Next
   rasterises outline plus the kid's own lines with `StrokeRasterizer` into `DrawingHandoff`,
   exactly the way a finished drawing goes (Drawing style, straight to the preview), so
   captions, stamps, rolls and round labels work on it with no editor or printer change.
3. **Every page fits every roll.** All ink stays inside `ColoringPage.SAFE_RADIUS` of the page's
   centre, so the same page prints whole on square and round labels; the catalog test enforces
   it, along with "mostly white" (it is for colouring, not a stamp) and a golden PBM per page.

## Consequences
- Adding a page is one entry plus a recorded golden; a designer can do it from the handbook
  recipe with no picture tools, and reviewers see the outline in the PBM diff.
- Lines are as smooth as the stroke rasteriser makes them (round caps, 6/14/28-dot widths);
  curves are polylines of 48 segments, which at sticker size print as curves.
- Because a page is a `Drawing`, opening it on the draw sheet needed no format or rasteriser
  change; the sheet only learned what its starting point was.
- Pages are square (`StrokeRasterizer.SIZE`); a rectangular page for the plain roll would need a
  size on the page, deferred until someone asks.
