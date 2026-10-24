package com.gozdor.slatenotes.ui.workspace

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shrinks slightly while pressed, then springs back. */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.88f else 1f, spring(dampingRatio = 0.55f, stiffness = 700f), label = "press")
    return this.graphicsLayer {
        scaleX = s
        scaleY = s
    }
}

/** White floating circle button with a soft shadow, like Obsidian's mobile header buttons. */
@Composable
fun CircleIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        interactionSource = interaction,
        modifier = Modifier
            .size(48.dp)
            .pressScale(interaction),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
        }
    }
}

/** Icon-only button for the floating navigation pill. */
@Composable
fun NavIcon(icon: ImageVector, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(52.dp)
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = false, radius = 24.dp),
                enabled = enabled,
                onClick = onClick,
            )
            .alpha(if (enabled) 1f else 0.3f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(26.dp))
    }
}

/** Outlined rounded "pill" button, used on the empty tab. Give a [width] to line several up evenly. */
@Composable
fun PillButton(label: String, icon: ImageVector? = null, width: Dp? = null, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = cs.surface,
        border = BorderStroke(1.dp, cs.outlineVariant),
        interactionSource = interaction,
        modifier = Modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
            .pressScale(interaction),
    ) {
        Row(
            Modifier.padding(start = 17.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, null, tint = cs.onSurface, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(9.dp))
            }
            Text(label, fontSize = 15.sp, color = cs.onSurface, maxLines = 1)
        }
    }
}
