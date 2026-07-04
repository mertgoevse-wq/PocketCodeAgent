package com.pocketcodeagent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketcodeagent.ui.theme.CalmSage
import com.pocketcodeagent.ui.theme.SlateBlue
import com.pocketcodeagent.ui.theme.TextPrimary
import com.pocketcodeagent.ui.theme.TextSecondary
import com.pocketcodeagent.ui.theme.WarmCopper

// ─── PcaStatusBadge ──────────────────────────────────────────────────────────

/** Unified status badge with dot + label + optional count. */
@Composable
fun PcaStatusBadge(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    count: Int? = null,
    dotFirst: Boolean = true
) {
    val displayText = if (count != null && count > 0) "$label $count" else label

    Surface(
        color = color.copy(alpha = 0.13f),
        shape = RoundedCornerShape(5.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (dotFirst) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(color, shape = RoundedCornerShape(3.dp))
                )
            }
            Text(
                displayText,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            if (!dotFirst && count != null && count > 0) {
                Text(
                    "$count",
                    color = color,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─── PcaRiskBadge ────────────────────────────────────────────────────────────

/** Risk level badge for commands — SAFE/CAUTION/BLOCKED. */
@Composable
fun PcaRiskBadge(
    riskLabel: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            riskLabel,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

// ─── PcaEmptyState ───────────────────────────────────────────────────────────

/** Reusable empty-state placeholder. */
@Composable
/** Re-exports PanelPlaceholder for the design-system package. Use PanelPlaceholder directly. */
@Deprecated("Use PanelPlaceholder from ui.workbench instead", ReplaceWith("PanelPlaceholder"))
fun PcaEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    com.pocketcodeagent.ui.workbench.PanelPlaceholder(
        icon = icon,
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        actionLabel = actionLabel,
        onAction = onAction
    )
}

// ─── PcaSectionHeader ────────────────────────────────────────────────────────

/** Consistent section header with optional action button. */
@Composable
fun PcaSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF13131A))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = SlateBlue, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Text(subtitle, color = Color(0xFF50505C), fontSize = 10.sp, maxLines = 1)
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(
                onClick = onAction,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(actionLabel, color = SlateBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─── PcaWarningBanner ────────────────────────────────────────────────────────

/** Dismissible warning banner. */
@Composable
fun PcaWarningBanner(
    text: String,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    Surface(
        color = WarmCopper.copy(alpha = 0.10f),
        shape = RoundedCornerShape(0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = WarmCopper, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text,
                color = WarmCopper,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (onDismiss != null) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = WarmCopper, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

// ─── PcaCard ─────────────────────────────────────────────────────────────────

/** Unified card surface matching the dark premium design. */
@Composable
fun PcaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            colors = CardDefaults.cardColors(containerColor = Color(0xFF15151A)),
            shape = RoundedCornerShape(8.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), content = content)
        }
    } else {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF15151A)),
            shape = RoundedCornerShape(8.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), content = content)
        }
    }
}

// ─── PcaTouchButton ──────────────────────────────────────────────────────────

/** Button with minimum 48dp touch target for accessibility. */
@Composable
fun PcaTouchButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = SlateBlue,
    minHeight: Dp = 48.dp,
    minWidth: Dp = 48.dp
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .defaultMinSize(minWidth = minWidth, minHeight = minHeight)
    ) {
        Text(
            label,
            color = if (enabled) Color.White else Color.Gray,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─── PcaTouchIconButton ──────────────────────────────────────────────────────

/** Icon button with minimum 48dp touch target. */
@Composable
fun PcaTouchIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = TextSecondary,
    background: Color? = null
) {
    val baseModifier = modifier
        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = if (background != null) {
            baseModifier.background(background, CircleShape)
        } else baseModifier
    ) {
        Icon(icon, contentDescription, tint = if (enabled) tint else Color(0xFF3A3A44))
    }
}
