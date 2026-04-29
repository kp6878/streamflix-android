package com.example.streamfilx_androidtv.features.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TextButton
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.streamfilx_androidtv.navigation.Screen

data class NavTab(
    val label: String,
    val route: String,
)

val TOP_NAV_TABS = listOf(
    NavTab("Home", Screen.Home.route),
    NavTab("Browse", Screen.Browse.route),
    NavTab("Search", Screen.Search.route),
    NavTab("My List", Screen.Library.route),
    NavTab("Downloads", Screen.Downloads.route),
    NavTab("Settings", Screen.Settings.route),
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopNavBar(
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Focus the currently selected tab when the nav bar is composed / re-shown
    val selectedIndex = TOP_NAV_TABS.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    val focusRequesters = remember { List(TOP_NAV_TABS.size) { FocusRequester() } }

    LaunchedEffect(currentRoute) {
        focusRequesters[selectedIndex].requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color.Black.copy(alpha = 0.95f)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // App wordmark
            Text(
                text = "STREAMFLIX",
                color = Color(0xFFE50914),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(end = 24.dp),
            )

            TOP_NAV_TABS.forEachIndexed { index, tab ->
                NavTabItem(
                    label = tab.label,
                    isSelected = currentRoute == tab.route,
                    focusRequester = focusRequesters[index],
                    onClick = { onTabSelected(tab.route) },
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NavTabItem(
    label: String,
    isSelected: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    val textColor by animateColorAsState(
        targetValue = when {
            isSelected || isFocused -> Color.White
            else -> Color.White.copy(alpha = 0.6f)
        },
        animationSpec = tween(150),
        label = "tabColor",
    )

    val pillAlpha by animateFloatAsState(
        targetValue = when {
            isSelected -> 1f
            isFocused -> 0.6f
            else -> 0f
        },
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "pillAlpha",
    )

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "tabScale",
    )

    Box(
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(20.dp))
            .background(
                Color(0xFFE50914).copy(alpha = pillAlpha * if (isSelected) 1f else 0.35f),
                RoundedCornerShape(20.dp),
            ),
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}
