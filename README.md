# Slate Notes

A personal Android notes app for Markdown vaults, inspired by Obsidian's workflow. Built clean-room from Obsidian's public documentation and its open, plain-file formats (Markdown, YAML frontmatter, [JSON Canvas](https://jsoncanvas.org)). No Obsidian code was decompiled or reused.

Because a vault is just a folder of `.md` files, Slate can open an existing Obsidian vault and both apps will work on the same notes.

## Install

**Android**

1. Open the [latest release](../../releases/latest) on your phone.
2. Download `SlateNotes.apk` and open it. Allow installs from your browser if Android asks.
3. Every push to `main` builds a new release. Newer APKs install over older ones (same signing key), so your settings are kept.

**Windows**

1. Open the [latest release](../../releases/latest) on your PC and download `SlateNotes-Setup.msi`.
2. Run it. It installs for your user only, so no administrator rights are needed. The installer isn't code-signed, so Windows SmartScreen may warn first: choose **More info**, then **Run anyway**.
3. Slate Notes appears in the Start menu and on the desktop. A newer installer upgrades the old version in place.

## Project layout

- `core/`: plain Kotlin shared by both apps (vaults, index, search, links, canvas files, settings), with unit tests.
- `shared-ui/`: Compose UI and app state shared by both apps (theme, icons, editor styling, canvas, sidebar panels, help).
- `app/`: the Android app (phone layout).
- `desktop/`: the Windows app (Obsidian desktop layout), packaged as an MSI.

## What works now (Phase 1)

- Open any folder as a vault, create new vault folders, auto-detect existing Obsidian vaults
- File tree with folders, long-press to rename, delete (moves to the vault's `.trash`), or create notes/folders inside
- Editor with inline Markdown styling (headings, bold, italic, strikethrough, highlights, code, links, tags), autosave
- Inline title that renames the note file
- Formatting toolbar, and lists/checklists that continue when you press Enter
- Reading view with rendered Markdown, properties panel, tables, task lists, embedded images (`![[image.png]]`)
- Tap `[[wikilinks]]` to follow them; linking to a note that doesn't exist creates it
- Search across titles and contents, with `tag:#name` filtering
- Tags list with counts, recent notes

## Roadmap

| Phase | Features |
|-------|----------|
| 2. Linking | Backlinks and outgoing links panels, `[[` autocomplete, note embeds, update links on rename, headings/blocks |
| 3. Views | Graph view, Canvas (`.canvas`), daily notes, templates |
| 4. Polish | Themes and accent colors, command palette, bookmarks, outline, undo/redo, home screen widget |

## Building

GitHub Actions builds the APK on every push (`.github/workflows/build.yml`). Locally: `./gradlew assembleRelease` with the Android SDK installed.

The keystore in `app/signing/` is a throwaway sideloading key so updates install over each other. It is not suitable for publishing to the Play Store.
