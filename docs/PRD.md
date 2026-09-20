# PugPrint — Product Requirements

## Goal
Let kids (~8–14) and a parent print photos and their own drawings to the Hello Blink
mini printer, entirely offline, with a delightful and simple UI.

## Non-goals
No cloud sync, sharing to social, user accounts, ads, in-app purchases, or telemetry.

## Personas
- **Maya, 10** — wants to print her drawings and selfies as stickers. Needs big buttons,
  forgiving flows, instant preview.
- **Parent/admin** — installs the app, pairs the printer once, trusts it collects no data.

## MVP features
1. Scan for & connect to the printer (Companion Device Manager pairing).
2. Pick a photo (Android Photo Picker).
3. Crop / rotate.
4. Dither preview (Floyd–Steinberg / threshold) at 384 px width.
5. Print, with progress + clear error states (out of paper, low battery, disconnected).

## Later
Text captions, sticker/emoji stamps, freehand drawing canvas, templates, print queue,
density/energy slider, multiple saved printers.

## Success criteria
A 10-year-old can go from "open app" to "printed sticker" in under 60 seconds without help.
