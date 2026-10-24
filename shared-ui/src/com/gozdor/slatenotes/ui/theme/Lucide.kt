package com.gozdor.slatenotes.ui.theme

// Generated from lucide-static (the icon set Obsidian uses). Do not edit by hand.
//
// ISC License
// 
// Copyright (c) 2026 Lucide Icons and Contributors
// 
// Permission to use, copy, modify, and/or distribute this software for any
// purpose with or without fee is hereby granted, provided that the above
// copyright notice and this permission notice appear in all copies.
// 
// THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
// WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
// ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
// WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
// ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
// OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
// 
// ---
// 
// The following Lucide icons are derived from the Feather project:
// 
// airplay, alert-circle, alert-octagon, alert-triangle, aperture, arrow-down-circle, arrow-down-left, arrow-down-right, arrow-down, arrow-left-circle, arrow-left, arrow-right-circle, arrow-right, arrow-up-circle, arrow-up-left, arrow-up-right, arrow-up, at-sign, calendar, cast, check, chevron-down, chevron-left, chevron-right, chevron-up, chevrons-down, chevrons-left, chevrons-right, chevrons-up, circle, clipboard, clock, code, columns, command, compass, corner-down-left, corner-down-right, corner-left-down, corner-left-up, corner-right-down, corner-right-up, corner-up-left, corner-up-right, crosshair, database, divide-circle, divide-square, dollar-sign, download, external-link, feather, frown, hash, headphones, help-circle, info, italic, key, layout, life-buoy, link-2, link, loader, lock, log-in, log-out, maximize, meh, minimize, minimize-2, minus-circle, minus-square, minus, monitor, moon, more-horizontal, more-vertical, move, music, navigation-2, navigation, octagon, pause-circle, percent, plus-circle, plus-square, plus, power, radio, rss, search, server, share, shopping-bag, sidebar, smartphone, smile, square, table-2, tablet, target, terminal, trash-2, trash, triangle, tv, type, upload, x-circle, x-octagon, x-square, x, zoom-in, zoom-out
// 
// The MIT License (MIT) (for the icons listed above)
// 
// Copyright (c) 2013-present Cole Bemis
// 
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
// 
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
// 
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private fun lucide(name: String, vararg paths: String): ImageVector {
    val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
    for (d in paths) {
        b.addPath(
            pathData = addPathNodes(d),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    return b.build()
}

object Lucide {
    val PanelLeft: ImageVector by lazy { lucide("panel-left", "M5.0 3.0H19.0A2.0 2.0 0 0 1 21.0 5.0V19.0A2.0 2.0 0 0 1 19.0 21.0H5.0A2.0 2.0 0 0 1 3.0 19.0V5.0A2.0 2.0 0 0 1 5.0 3.0Z", "M9 3v18") }
    val EllipsisVertical: ImageVector by lazy { lucide("ellipsis-vertical", "M11.0 12.0a1.0 1.0 0 1 0 2.0 0a1.0 1.0 0 1 0 -2.0 0", "M11.0 5.0a1.0 1.0 0 1 0 2.0 0a1.0 1.0 0 1 0 -2.0 0", "M11.0 19.0a1.0 1.0 0 1 0 2.0 0a1.0 1.0 0 1 0 -2.0 0") }
    val ChevronLeft: ImageVector by lazy { lucide("chevron-left", "m15 18-6-6 6-6") }
    val ChevronRight: ImageVector by lazy { lucide("chevron-right", "m9 18 6-6-6-6") }
    val Search: ImageVector by lazy { lucide("search", "m21 21-4.34-4.34", "M3.0 11.0a8.0 8.0 0 1 0 16.0 0a8.0 8.0 0 1 0 -16.0 0") }
    val Plus: ImageVector by lazy { lucide("plus", "M5 12h14", "M12 5v14") }
    val Menu: ImageVector by lazy { lucide("menu", "M4 5h16", "M4 12h16", "M4 19h16") }
    val SquarePen: ImageVector by lazy { lucide("square-pen", "M12 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7", "M18.375 2.625a1 1 0 0 1 3 3l-9.013 9.014a2 2 0 0 1-.853.505l-2.873.84a.5.5 0 0 1-.62-.62l.84-2.873a2 2 0 0 1 .506-.852z") }
    val FolderPlus: ImageVector by lazy { lucide("folder-plus", "M12 10v6", "M9 13h6", "M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z") }
    val Folder: ImageVector by lazy { lucide("folder", "M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z") }
    val FileText: ImageVector by lazy { lucide("file-text", "M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z", "M14 2v5a1 1 0 0 0 1 1h5", "M10 9H8", "M16 13H8", "M16 17H8") }
    val File: ImageVector by lazy { lucide("file", "M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z", "M14 2v5a1 1 0 0 0 1 1h5") }
    val ChevronDown: ImageVector by lazy { lucide("chevron-down", "m6 9 6 6 6-6") }
    val ChevronsUpDown: ImageVector by lazy { lucide("chevrons-up-down", "m7 15 5 5 5-5", "m7 9 5-5 5 5") }
    val ChevronsDownUp: ImageVector by lazy { lucide("chevrons-down-up", "m7 20 5-5 5 5", "m7 4 5 5 5-5") }
    val Settings: ImageVector by lazy { lucide("settings", "M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915", "M9.0 12.0a3.0 3.0 0 1 0 6.0 0a3.0 3.0 0 1 0 -6.0 0") }
    val FileSearch: ImageVector by lazy { lucide("file-search", "M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z", "M14 2v5a1 1 0 0 0 1 1h5", "M9.0 14.5a2.5 2.5 0 1 0 5.0 0a2.5 2.5 0 1 0 -5.0 0", "M13.3 16.3 15 18") }
    val Terminal: ImageVector by lazy { lucide("terminal", "M12 19h8", "m4 17 6-6-6-6") }
    val Bookmark: ImageVector by lazy { lucide("bookmark", "M17 3a2 2 0 0 1 2 2v15a1 1 0 0 1-1.496.868l-4.512-2.578a2 2 0 0 0-1.984 0l-4.512 2.578A1 1 0 0 1 5 20V5a2 2 0 0 1 2-2z") }
    val BookOpen: ImageVector by lazy { lucide("book-open", "M12 5v16", "M20.001 19A2 2 0 0022 17V5a2 2 0 00-1.999-2L16 3.002A5 5 0 0012 5a5 5 0 00-4-2H4a2 2 0 00-2 2v12a2 2 0 001.999 2H8a5 5 0 014 2 5 5 0 014-2z") }
    val Pencil: ImageVector by lazy { lucide("pencil", "M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z", "m15 5 4 4") }
    val X: ImageVector by lazy { lucide("x", "M18 6 6 18", "m6 6 12 12") }
    val ArrowLeft: ImageVector by lazy { lucide("arrow-left", "m12 19-7-7 7-7", "M19 12H5") }
    val Link: ImageVector by lazy { lucide("link", "M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71", "M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71") }
    val Hash: ImageVector by lazy { lucide("hash", "M4 9L20 9", "M4 15L20 15", "M10 3L8 21", "M16 3L14 21") }
    val List: ImageVector by lazy { lucide("list", "M3 5h.01", "M3 12h.01", "M3 19h.01", "M8 5h13", "M8 12h13", "M8 19h13") }
    val ListOrdered: ImageVector by lazy { lucide("list-ordered", "M11 5h10", "M11 12h10", "M11 19h10", "M4 4h1v5", "M4 9h2", "M6.5 20H3.4c0-1 2.6-1.925 2.6-3.5a1.5 1.5 0 0 0-2.6-1.02") }
    val SquareCheck: ImageVector by lazy { lucide("square-check", "M5.0 3.0H19.0A2.0 2.0 0 0 1 21.0 5.0V19.0A2.0 2.0 0 0 1 19.0 21.0H5.0A2.0 2.0 0 0 1 3.0 19.0V5.0A2.0 2.0 0 0 1 5.0 3.0Z", "m16 9-5.5 5.5L8 12") }
    val ListIndentIncrease: ImageVector by lazy { lucide("list-indent-increase", "M21 5H11", "M21 12H11", "M21 19H11", "m3 8 4 4-4 4") }
    val ListIndentDecrease: ImageVector by lazy { lucide("list-indent-decrease", "M21 5H11", "M21 12H11", "M21 19H11", "m7 8-4 4 4 4") }
    val Bold: ImageVector by lazy { lucide("bold", "M6 12h9a4 4 0 0 1 0 8H7a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1h7a4 4 0 0 1 0 8") }
    val Italic: ImageVector by lazy { lucide("italic", "M19 4L10 4", "M14 20L5 20", "M15 4L9 20") }
    val Strikethrough: ImageVector by lazy { lucide("strikethrough", "M16 4H9a3 3 0 0 0-2.83 4", "M14 12a4 4 0 0 1 0 8H6", "M4 12L20 12") }
    val TextQuote: ImageVector by lazy { lucide("text-quote", "M17 5H3", "M21 12H8", "M21 19H8", "M3 12v7") }
    val Code: ImageVector by lazy { lucide("code", "m16 18 6-6-6-6", "m8 6-6 6 6 6") }
    val Heading: ImageVector by lazy { lucide("heading", "M6 12h12", "M6 20V4", "M18 20V4") }
    val KeyboardOff: ImageVector by lazy { lucide("keyboard-off", "M 20 4 A2 2 0 0 1 22 6", "M 22 6 L 22 16.41", "M 7 16 L 16 16", "M 9.69 4 L 20 4", "M14 8h.01", "M18 8h.01", "m2 2 20 20", "M20 20H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2", "M6 8h.01", "M8 12h.01") }
    val Tags: ImageVector by lazy { lucide("tags", "M13.172 2a2 2 0 0 1 1.414.586l6.71 6.71a2.4 2.4 0 0 1 0 3.408l-4.592 4.592a2.4 2.4 0 0 1-3.408 0l-6.71-6.71A2 2 0 0 1 6 9.172V3a1 1 0 0 1 1-1z", "M2 7v6.172a2 2 0 0 0 .586 1.414l6.71 6.71a2.4 2.4 0 0 0 3.191.193", "M10.0 6.5a0.5 0.5 0 1 0 1.0 0a0.5 0.5 0 1 0 -1.0 0") }
    val CircleQuestionMark: ImageVector by lazy { lucide("circle-question-mark", "M2.0 12.0a10.0 10.0 0 1 0 20.0 0a10.0 10.0 0 1 0 -20.0 0", "M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3", "M12 17h.01") }
    val Trash: ImageVector by lazy { lucide("trash", "M10 11v6", "M14 11v6", "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6", "M3 6h18", "M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2") }
    val ArrowDownLeft: ImageVector by lazy { lucide("arrow-down-left", "M17 7 7 17", "M17 17H7V7") }
    val ArrowUpRight: ImageVector by lazy { lucide("arrow-up-right", "M7 7h10v10", "M7 17 17 7") }
    val FolderInput: ImageVector by lazy { lucide("folder-input", "M2 9V5a2 2 0 0 1 2-2h3.9a2 2 0 0 1 1.69.9l.81 1.2a2 2 0 0 0 1.67.9H20a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2v-1", "M2 13h10", "m9 16 3-3-3-3") }
    val House: ImageVector by lazy { lucide("house", "M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8", "M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z") }
    val LayoutList: ImageVector by lazy { lucide("layout-list", "M4.0 3.0H9.0A1.0 1.0 0 0 1 10.0 4.0V9.0A1.0 1.0 0 0 1 9.0 10.0H4.0A1.0 1.0 0 0 1 3.0 9.0V4.0A1.0 1.0 0 0 1 4.0 3.0Z", "M4.0 14.0H9.0A1.0 1.0 0 0 1 10.0 15.0V20.0A1.0 1.0 0 0 1 9.0 21.0H4.0A1.0 1.0 0 0 1 3.0 20.0V15.0A1.0 1.0 0 0 1 4.0 14.0Z", "M14 4h7", "M14 9h7", "M14 15h7", "M14 20h7") }
    val LayoutDashboard: ImageVector by lazy { lucide("layout-dashboard", "M4.0 3.0H9.0A1.0 1.0 0 0 1 10.0 4.0V11.0A1.0 1.0 0 0 1 9.0 12.0H4.0A1.0 1.0 0 0 1 3.0 11.0V4.0A1.0 1.0 0 0 1 4.0 3.0Z", "M15.0 3.0H20.0A1.0 1.0 0 0 1 21.0 4.0V7.0A1.0 1.0 0 0 1 20.0 8.0H15.0A1.0 1.0 0 0 1 14.0 7.0V4.0A1.0 1.0 0 0 1 15.0 3.0Z", "M15.0 12.0H20.0A1.0 1.0 0 0 1 21.0 13.0V20.0A1.0 1.0 0 0 1 20.0 21.0H15.0A1.0 1.0 0 0 1 14.0 20.0V13.0A1.0 1.0 0 0 1 15.0 12.0Z", "M4.0 16.0H9.0A1.0 1.0 0 0 1 10.0 17.0V20.0A1.0 1.0 0 0 1 9.0 21.0H4.0A1.0 1.0 0 0 1 3.0 20.0V17.0A1.0 1.0 0 0 1 4.0 16.0Z") }
    val Image: ImageVector by lazy { lucide("image", "M5.0 3.0H19.0A2.0 2.0 0 0 1 21.0 5.0V19.0A2.0 2.0 0 0 1 19.0 21.0H5.0A2.0 2.0 0 0 1 3.0 19.0V5.0A2.0 2.0 0 0 1 5.0 3.0Z", "M7.0 9.0a2.0 2.0 0 1 0 4.0 0a2.0 2.0 0 1 0 -4.0 0", "m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21") }
    val Palette: ImageVector by lazy { lucide("palette", "M12 22a1 1 0 0 1 0-20 10 9 0 0 1 10 9 5 5 0 0 1-5 5h-2.25a1.75 1.75 0 0 0-1.4 2.8l.3.4a1.75 1.75 0 0 1-1.4 2.8z", "M13.0 6.5a0.5 0.5 0 1 0 1.0 0a0.5 0.5 0 1 0 -1.0 0", "M17.0 10.5a0.5 0.5 0 1 0 1.0 0a0.5 0.5 0 1 0 -1.0 0", "M6.0 12.5a0.5 0.5 0 1 0 1.0 0a0.5 0.5 0 1 0 -1.0 0", "M8.0 7.5a0.5 0.5 0 1 0 1.0 0a0.5 0.5 0 1 0 -1.0 0") }
    val Maximize: ImageVector by lazy { lucide("maximize", "M8 3H5a2 2 0 0 0-2 2v3", "M21 8V5a2 2 0 0 0-2-2h-3", "M3 16v3a2 2 0 0 0 2 2h3", "M16 21h3a2 2 0 0 0 2-2v-3") }
    val Spline: ImageVector by lazy { lucide("spline", "M17.0 5.0a2.0 2.0 0 1 0 4.0 0a2.0 2.0 0 1 0 -4.0 0", "M3.0 19.0a2.0 2.0 0 1 0 4.0 0a2.0 2.0 0 1 0 -4.0 0", "M5 17A12 12 0 0 1 17 5") }
    val Type: ImageVector by lazy { lucide("type", "M12 4v16", "M4 7V5a1 1 0 0 1 1-1h14a1 1 0 0 1 1 1v2", "M9 20h6") }
    val FilePlus: ImageVector by lazy { lucide("file-plus", "M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z", "M14 2v5a1 1 0 0 0 1 1h5", "M9 15h6", "M12 18v-6") }
    val ExternalLink: ImageVector by lazy { lucide("external-link", "M15 3h6v6", "M10 14 21 3", "M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6") }
    val Group: ImageVector by lazy { lucide("group", "M3 7V5c0-1.1.9-2 2-2h2", "M17 3h2c1.1 0 2 .9 2 2v2", "M21 17v2c0 1.1-.9 2-2 2h-2", "M7 21H5c-1.1 0-2-.9-2-2v-2", "M8.0 7.0H13.0A1.0 1.0 0 0 1 14.0 8.0V11.0A1.0 1.0 0 0 1 13.0 12.0H8.0A1.0 1.0 0 0 1 7.0 11.0V8.0A1.0 1.0 0 0 1 8.0 7.0Z", "M11.0 12.0H16.0A1.0 1.0 0 0 1 17.0 13.0V16.0A1.0 1.0 0 0 1 16.0 17.0H11.0A1.0 1.0 0 0 1 10.0 16.0V13.0A1.0 1.0 0 0 1 11.0 12.0Z") }
}
