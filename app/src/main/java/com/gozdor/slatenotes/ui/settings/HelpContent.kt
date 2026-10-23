package com.gozdor.slatenotes.ui.settings

/** One paragraph of help: plain text, or a bullet list when [bullets] is set. */
data class HelpBlock(val text: String? = null, val bullets: List<String> = emptyList(), val heading: String? = null)

data class HelpTopic(val title: String, val summary: String, val blocks: List<HelpBlock>)

/** Help & tips, written for what Slate Notes actually does today. Keep this in sync when features change. */
val HelpTopics: List<HelpTopic> = listOf(
    HelpTopic(
        "Getting started",
        "The basics in one minute",
        listOf(
            HelpBlock("Slate keeps every note as a plain Markdown (.md) file inside a folder called a vault. There is no database and no account, so your notes are always just files you own."),
            HelpBlock(
                heading = "Your first note",
                bullets = listOf(
                    "Tap the + in a circle at the bottom to find a note or type a new name and create it.",
                    "Or open the menu (☰ at the bottom right) and choose New note.",
                    "Type a title at the top. The title is the file name, so renaming the title renames the file.",
                    "Everything saves automatically about half a second after you stop typing.",
                ),
            ),
            HelpBlock(
                heading = "Getting around",
                bullets = listOf(
                    "Left sidebar: your files, search and bookmarks.",
                    "Right sidebar: backlinks, outgoing links, tags and the outline of the open note.",
                    "Bottom bar: back, forward, find/create note, tabs, and the menu.",
                ),
            ),
        ),
    ),
    HelpTopic(
        "Vaults",
        "Folders that hold your notes",
        listOf(
            HelpBlock("A vault is any folder on your phone. Slate reads and writes the Markdown files inside it and ignores hidden folders such as .obsidian and .trash."),
            HelpBlock(
                heading = "Open or create a vault",
                bullets = listOf(
                    "Tap the vault name at the bottom of the left sidebar, or menu ▸ Open another vault.",
                    "Choose \"Open or create a vault folder\", browse to a folder, and tap Use as vault.",
                    "Use the folder+ icon in the browser to make a brand-new folder first.",
                    "Existing Obsidian vaults on your phone are found automatically and listed for one-tap opening.",
                ),
            ),
            HelpBlock("Long-press a vault in the list to remove it from Slate's list. That never deletes any files."),
            HelpBlock("Slate needs Android's \"All files access\" so it can work with a folder anywhere on your phone, the same files other apps can see."),
        ),
    ),
    HelpTopic(
        "Notes, folders and files",
        "Create, rename, move to trash",
        listOf(
            HelpBlock(
                bullets = listOf(
                    "Open the left sidebar and pick the Files tab to see your vault's folders and notes.",
                    "Tap a folder to expand it. The collapse button at the top folds everything back up.",
                    "Long-press a note or folder and let go for its menu: New note, New folder, Open in new tab, Bookmark, Move to, Rename and Delete.",
                    "Use the buttons at the top of Files to create a note or folder in the vault's top level.",
                ),
            ),
            HelpBlock(
                heading = "Moving notes and folders",
                bullets = listOf(
                    "Drag and drop: long-press a note or folder until your phone buzzes, then drag it without letting go. Drop it on a folder to move it inside, or on empty space to move it to the vault's top level.",
                    "Hover over a closed folder for a moment while dragging and it opens, so you can drop into subfolders.",
                    "Near the top or bottom of the list, the list scrolls by itself while you drag.",
                    "Or use Move to… from the long-press menu, the note's ⋮ menu, or the command palette, and pick a folder from the list.",
                    "A folder can't be moved into itself, and a move is refused if the destination already has something with the same name.",
                ),
            ),
            HelpBlock(
                heading = "Where new notes go",
                text = "By default new notes are created in the vault's top folder. In Settings ▸ Files and links you can switch to \"same folder as current note\".",
            ),
            HelpBlock(
                heading = "Deleting",
                text = "Deleted files move to the vault's .trash folder, so you can get them back with a file manager. Settings ▸ Files and links can change this to delete permanently, and can turn off the confirmation prompt.",
            ),
            HelpBlock("Files other than Markdown (images, PDFs and so on) show in the list but can't be opened in Slate yet. Images can still be embedded in notes."),
        ),
    ),
    HelpTopic(
        "Editing and formatting",
        "Markdown, the toolbar, lists",
        listOf(
            HelpBlock("Slate uses standard Markdown. Syntax is styled as you type: headings grow, bold turns bold, and the symbols fade so the text stays readable."),
            HelpBlock(
                heading = "Common syntax",
                bullets = listOf(
                    "# Heading 1, ## Heading 2, ### Heading 3",
                    "**bold**, *italic*, ~~strikethrough~~, ==highlight==",
                    "- bullet, 1. numbered, - [ ] task, - [x] done task",
                    "> quote, `inline code`, and ``` on its own line to start and end a code block",
                    "[[Note name]] links to another note, #tag adds a tag",
                ),
            ),
            HelpBlock(
                heading = "Toolbar",
                text = "While the keyboard is open, a toolbar sits above it. Swipe it sideways for more buttons: link, tag, checklist, bullets, numbers, indent, heading, bold, italic, strikethrough, quote, code, the command palette, and hide keyboard. With text selected, bold, italic and similar buttons wrap the selection; tap again to unwrap.",
            ),
            HelpBlock(
                heading = "Lists continue automatically",
                text = "Press Enter at the end of a list item and the next bullet, number or checkbox is added for you. Press Enter on an empty item to end the list. You can turn this off in Settings ▸ Editor.",
            ),
        ),
    ),
    HelpTopic(
        "Reading view",
        "See the rendered note",
        listOf(
            HelpBlock("Tap the book icon in the top bar to switch the open note to reading view; tap the pencil to go back to editing. Each tab remembers its own mode."),
            HelpBlock(
                bullets = listOf(
                    "Links and tags are tappable. Tapping a tag opens search for that tag.",
                    "Properties (the YAML block at the top of a note) show as a neat panel.",
                    "Tables, task lists and embedded images (![[photo.png]]) are rendered.",
                    "To open every note in reading view by default, use Settings ▸ Editor ▸ Default view for new tabs.",
                ),
            ),
        ),
    ),
    HelpTopic(
        "Links and backlinks",
        "Connect notes together",
        listOf(
            HelpBlock("Type [[ and a note's name, then ]] to link to it. Use the link button in the toolbar to wrap the selection in brackets."),
            HelpBlock(
                bullets = listOf(
                    "[[Note]] links by name. [[Folder/Note]] links to a specific path.",
                    "[[Note|shown text]] changes the text displayed in reading view.",
                    "[[Note#Heading]] points at a heading inside a note.",
                    "Linking to a note that doesn't exist yet is fine. Tap the link and Slate creates it.",
                ),
            ),
            HelpBlock(
                heading = "Backlinks and outgoing links",
                text = "Open the right sidebar. Backlinks lists every note that links to the open note. Outgoing links lists the links inside the open note, including ones whose notes haven't been created yet (tap one to create it).",
            ),
            HelpBlock("Renaming a note does not update links in other notes yet. That is planned."),
        ),
    ),
    HelpTopic(
        "Tags and properties",
        "Organize with #tags and YAML",
        listOf(
            HelpBlock("Add a tag anywhere in a note by typing #tag. Tags can be nested with slashes, like #project/home. A tag needs at least one letter, so #2024 is not a tag but #y2024 is."),
            HelpBlock(
                heading = "Properties",
                text = "Put a YAML block between two lines of --- at the very top of a note. A tags property (tags: [one, two] or a list of - items) counts as tags too, and aliases lets a note be found or linked by another name.",
            ),
            HelpBlock("See every tag and how many notes use it in the right sidebar's Tags tab. Tap a tag to search for it."),
        ),
    ),
    HelpTopic(
        "Search and quick switcher",
        "Find anything fast",
        listOf(
            HelpBlock(
                heading = "Quick switcher",
                text = "Tap the + in a circle on the bottom bar. Start typing a note name; letters don't need to be next to each other (\"mtg nts\" finds \"Meeting notes\"). If nothing matches exactly, the last row creates a new note with that name. With an empty box it shows recently opened notes.",
            ),
            HelpBlock(
                heading = "Search",
                text = "Left sidebar ▸ Search looks through titles and the full text of every note. All words must appear. Type tag:#name to show only notes with that tag (nested tags included). Results show a snippet of where the match is.",
            ),
        ),
    ),
    HelpTopic(
        "Command palette and gestures",
        "Every action in one list",
        listOf(
            HelpBlock("The command palette lists everything Slate can do: create, rename, delete, bookmark, toggle reading view, insert the date or time, show sidebar panels, manage tabs, and more. Type to filter."),
            HelpBlock(
                heading = "Ways to open it",
                bullets = listOf(
                    "Pull down from the top of a note.",
                    "Menu (☰) ▸ Command palette.",
                    "The terminal icon on the editing toolbar.",
                ),
            ),
            HelpBlock(
                heading = "Gestures",
                bullets = listOf(
                    "Swipe in from the middle of the left or right edge to open a sidebar. Swipe it back or tap outside to close.",
                    "If your phone uses gesture navigation, Android reserves most of each edge for its Back gesture. Slate claims a strip in the middle of each edge; the sidebar buttons in the top bar always work too.",
                    "Long-press the tab counter to open a new tab right away.",
                ),
            ),
        ),
    ),
    HelpTopic(
        "Tabs and history",
        "Keep several notes open",
        listOf(
            HelpBlock(
                bullets = listOf(
                    "The number in a box on the bottom bar is how many tabs are open. Tap it to see them, switch, close, or add a new one.",
                    "Long-press a note in Files and choose Open in new tab.",
                    "Each tab has its own history. Use the back and forward arrows on the bottom bar, or Android's back button.",
                    "Open tabs are remembered when you leave and come back.",
                ),
            ),
        ),
    ),
    HelpTopic(
        "Bookmarks",
        "Pin the notes you use most",
        listOf(
            HelpBlock("Bookmark a note from its ⋮ menu in the top bar, by long-pressing it in Files, or with the command palette. Find them in the left sidebar's Bookmarks tab, where the × removes one."),
            HelpBlock("Bookmarks are stored by Slate itself, separately from Obsidian's own bookmarks."),
        ),
    ),
    HelpTopic(
        "Using Slate with Obsidian and sync",
        "Same files, different apps",
        listOf(
            HelpBlock("Because a vault is just a folder of Markdown files, Slate and Obsidian can open the same vault. Slate doesn't change anything inside the .obsidian folder."),
            HelpBlock(
                bullets = listOf(
                    "To sync between devices, use a tool that syncs folders, such as Syncthing, or a cloud drive app that keeps a real local folder.",
                    "Slate rescans the vault every time you come back to the app, so changes made elsewhere show up.",
                    "Avoid editing the same note in two apps at the same moment; the last save wins.",
                ),
            ),
        ),
    ),
    HelpTopic(
        "Settings",
        "What each option does",
        listOf(
            HelpBlock(
                bullets = listOf(
                    "Editor: default view for new tabs, font size, inline title, list continuation.",
                    "Files and links: where new notes go, delete to .trash or permanently, confirm before deleting.",
                    "Appearance: light, dark or follow the system, and the accent color used for links, tags and buttons.",
                ),
            ),
        ),
    ),
)
