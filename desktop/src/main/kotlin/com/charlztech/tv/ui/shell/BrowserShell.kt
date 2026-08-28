package com.charlztech.tv.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import com.charlztech.tv.ui.util.ScrollableColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.charlztech.tv.resources.Strings
import com.charlztech.tv.ui.theme.AppColors
import com.charlztech.tv.ui.util.Responsive
import com.charlztech.tv.ui.util.appKeyboardShortcuts

enum class BrowserTab(val label: String, val icon: ImageVector) {
    Home(Strings.navHome, Icons.Default.Home),
    Providers(Strings.navProviders, Icons.Default.Tv),
    Search(Strings.navSearch, Icons.Default.Search),
    Favorites(Strings.navFavorites, Icons.Default.Favorite)
}

@Composable
fun BrowserShell(
    selectedTab: BrowserTab,
    onTabSelected: (BrowserTab) -> Unit,
    addressBarText: String,
    onAddressBarChange: (String) -> Unit,
    onAddressBarSubmit: () -> Unit,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onAbout: () -> Unit,
    onToggleWindowFullscreen: () -> Unit,
    isWindowFullscreen: Boolean = false,
    isSidebarCollapsed: Boolean,
    onToggleSidebar: () -> Unit,
    statusText: String? = null,
    isVideoFullscreen: Boolean = false,
    onToggleVideoFullscreen: (() -> Unit)? = null,
    onExitVideoFullscreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (isVideoFullscreen) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .appKeyboardShortcuts(
                    onBack = onBack,
                    onRefresh = onRefresh,
                    onToggleWindowFullscreen = onToggleWindowFullscreen,
                    onToggleVideoFullscreen = onToggleVideoFullscreen,
                    onExitVideoFullscreen = onExitVideoFullscreen,
                    isVideoFullscreen = true
                )
        ) {
            content()
            VideoFullscreenOverlay(
                onExit = { onExitVideoFullscreen?.invoke() },
                onToggleWindowFullscreen = onToggleWindowFullscreen
            )
        }
        return
    }
    val sidebarWidth = Responsive.sidebarWidth(isSidebarCollapsed)
    val compact = Responsive.useCompactSidebar() || isSidebarCollapsed

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.ChromeBackground)
            .appKeyboardShortcuts(
                onBack = onBack,
                onRefresh = onRefresh,
                onToggleWindowFullscreen = onToggleWindowFullscreen,
                onToggleVideoFullscreen = onToggleVideoFullscreen,
                onExitVideoFullscreen = onExitVideoFullscreen,
                isVideoFullscreen = false
            )
    ) {
        BrowserToolbar(
            addressBarText = addressBarText,
            onAddressBarChange = onAddressBarChange,
            onAddressBarSubmit = onAddressBarSubmit,
            canGoBack = canGoBack,
            canGoForward = canGoForward,
            onBack = onBack,
            onForward = onForward,
            onRefresh = onRefresh,
            onToggleSidebar = onToggleSidebar,
            onToggleWindowFullscreen = onToggleWindowFullscreen,
            isWindowFullscreen = isWindowFullscreen,
            onToggleVideoFullscreen = onToggleVideoFullscreen,
            statusText = statusText
        )
        HorizontalDivider(color = AppColors.Border)
        Row(Modifier.fillMaxSize()) {
            BrowserSidebar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                onAbout = onAbout,
                collapsed = compact,
                width = sidebarWidth
            )
            VerticalDivider(color = AppColors.Border, modifier = Modifier.fillMaxHeight())
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(AppColors.ContentBackground)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun BoxScope.VideoFullscreenOverlay(
    onExit: () -> Unit,
    onToggleWindowFullscreen: () -> Unit
) {
    Row(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onExit,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.65f)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                Icons.Default.FullscreenExit,
                contentDescription = Strings.exitFullscreen,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("${Strings.exitFullscreen} (Esc)", color = Color.White)
        }
        IconButton(
            onClick = onToggleWindowFullscreen,
            modifier = Modifier.background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
        ) {
            Icon(Icons.Default.Fullscreen, contentDescription = Strings.fullscreen, tint = Color.White)
        }
    }
}

@Composable
private fun BrowserToolbar(
    addressBarText: String,
    onAddressBarChange: (String) -> Unit,
    onAddressBarSubmit: () -> Unit,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onToggleSidebar: () -> Unit,
    onToggleWindowFullscreen: () -> Unit,
    isWindowFullscreen: Boolean,
    onToggleVideoFullscreen: (() -> Unit)?,
    statusText: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.ToolbarBackground)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggleSidebar, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = AppColors.TextPrimary)
        }
        IconButton(onClick = onBack, enabled = canGoBack, modifier = Modifier.size(36.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppColors.TextPrimary)
        }
        IconButton(onClick = onForward, enabled = canGoForward, modifier = Modifier.size(36.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", tint = AppColors.TextPrimary)
        }
        IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Refresh, contentDescription = Strings.refresh, tint = AppColors.TextPrimary)
        }
        Spacer(Modifier.width(4.dp))
        OutlinedTextField(
            value = addressBarText,
            onValueChange = onAddressBarChange,
            modifier = Modifier.weight(1f).height(44.dp),
            placeholder = { Text(Strings.searchHint, color = AppColors.TextSecondary) },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AppColors.AddressBarBackground,
                unfocusedContainerColor = AppColors.AddressBarBackground,
                focusedBorderColor = AppColors.Primary,
                unfocusedBorderColor = AppColors.Border,
                cursorColor = AppColors.Primary,
                focusedTextColor = AppColors.TextPrimary,
                unfocusedTextColor = AppColors.TextPrimary
            ),
            trailingIcon = {
                IconButton(onClick = onAddressBarSubmit) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = AppColors.Primary)
                }
            }
        )
        if (!statusText.isNullOrBlank()) {
            Spacer(Modifier.width(8.dp))
            Text(
                statusText,
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 160.dp)
            )
        }
        if (onToggleVideoFullscreen != null) {
            Button(
                onClick = onToggleVideoFullscreen,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary.copy(alpha = 0.12f),
                    contentColor = AppColors.Primary
                ),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Default.Fullscreen, contentDescription = Strings.fullscreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(Strings.fullscreen, style = MaterialTheme.typography.labelMedium)
            }
        }
        IconButton(onClick = onToggleWindowFullscreen, modifier = Modifier.size(36.dp)) {
            Icon(
                if (isWindowFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = if (isWindowFullscreen) "Exit fullscreen (F11)" else "Fullscreen (F11)",
                tint = AppColors.TextPrimary
            )
        }
    }
}

@Composable
private fun BrowserSidebar(
    selectedTab: BrowserTab,
    onTabSelected: (BrowserTab) -> Unit,
    onAbout: () -> Unit,
    collapsed: Boolean,
    width: androidx.compose.ui.unit.Dp
) {
    ScrollableColumn(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(AppColors.SidebarBackground)
            .padding(vertical = 12.dp, horizontal = if (collapsed) 6.dp else 8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (!collapsed) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(8.dp), color = AppColors.Primary, modifier = Modifier.size(32.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("CT", color = AppColors.OnPrimary, style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(Strings.appName, style = MaterialTheme.typography.titleSmall, color = AppColors.TextPrimary)
                    Text("Windows", style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary)
                }
            }
            HorizontalDivider(color = AppColors.Border, modifier = Modifier.padding(vertical = 8.dp))
        }
        BrowserTab.entries.forEach { tab ->
            SidebarItem(
                label = tab.label,
                icon = tab.icon,
                selected = selectedTab == tab,
                collapsed = collapsed,
                onClick = { onTabSelected(tab) }
            )
        }
        Spacer(Modifier.height(24.dp))
        SidebarItem(
            label = Strings.about,
            icon = Icons.Default.Info,
            selected = false,
            collapsed = collapsed,
            onClick = onAbout
        )
        }
    }
}

@Composable
private fun SidebarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    collapsed: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) AppColors.Primary.copy(alpha = 0.12f) else AppColors.SidebarBackground
    val tint = if (selected) AppColors.Primary else AppColors.TextPrimary
    if (collapsed) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = label, tint = tint)
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(bg)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = tint)
        }
    }
}

private val Color = androidx.compose.ui.graphics.Color
