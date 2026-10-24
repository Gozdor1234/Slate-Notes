package com.gozdor.slatenotes.ui.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.outlined.CallMade
import androidx.compose.material.icons.outlined.CallReceived
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gozdor.slatenotes.data.Md
import com.gozdor.slatenotes.ui.AppViewModel
import com.gozdor.slatenotes.ui.JumpRequest
import com.gozdor.slatenotes.ui.screens.EmptyHint
import com.gozdor.slatenotes.ui.screens.NoteRow
import com.gozdor.slatenotes.ui.theme.Lucide
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height

@Composable
fun ColumnScope.RightSidebar(vm: AppViewModel) {
    Box(
        Modifier
            .weight(1f)
            .fillMaxWidth(),
    ) {
        AnimatedContent(
            targetState = vm.rightPanel,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "rightPanel",
        ) { panel ->
            when (panel) {
                0 -> BacklinksPanel(vm)
                1 -> OutgoingPanel(vm)
                2 -> TagsPanel(vm)
                else -> OutlinePanel(vm)
            }
        }
    }
    ViewSwitcherPill(
        options = listOf(
            Lucide.ArrowDownLeft to "Backlinks",
            Lucide.ArrowUpRight to "Outgoing links",
            Lucide.Tags to "Tags",
            Lucide.List to "Outline",
        ),
        selected = vm.rightPanel,
        onSelect = { vm.rightPanel = it },
    )
    Spacer(Modifier.height(10.dp))
}
