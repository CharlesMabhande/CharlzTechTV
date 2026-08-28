@file:OptIn(ExperimentalComposeUiApi::class)

package com.charlztech.tv.ui.util

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Alignment.Companion.CenterEnd
import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val KEY_SCROLL_STEP = 80f
private const val WHEEL_SCROLL_MULTIPLIER = 48f

private fun Modifier.keyboardLazyListScroll(
    state: LazyListState,
    scope: CoroutineScope,
    vertical: Boolean = true
): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    val delta = when (event.key) {
        Key.DirectionDown, Key.PageDown -> if (vertical) KEY_SCROLL_STEP else return@onPreviewKeyEvent false
        Key.DirectionUp, Key.PageUp -> if (vertical) -KEY_SCROLL_STEP else return@onPreviewKeyEvent false
        Key.DirectionRight -> if (!vertical) KEY_SCROLL_STEP else return@onPreviewKeyEvent false
        Key.DirectionLeft -> if (!vertical) -KEY_SCROLL_STEP else return@onPreviewKeyEvent false
        else -> return@onPreviewKeyEvent false
    }
    scope.launch { state.animateScrollBy(delta) }
    true
}

private fun Modifier.keyboardScroll(state: ScrollState, scope: CoroutineScope): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    val delta = when (event.key) {
        Key.DirectionDown, Key.PageDown -> KEY_SCROLL_STEP
        Key.DirectionUp, Key.PageUp -> -KEY_SCROLL_STEP
        else -> return@onPreviewKeyEvent false
    }
    scope.launch { state.animateScrollBy(delta) }
    true
}

private fun Modifier.wheelLazyListScroll(
    state: LazyListState,
    scope: CoroutineScope,
    vertical: Boolean = true
): Modifier = onPointerEvent(PointerEventType.Scroll) { event ->
    val delta = event.changes.fold(0f) { acc, change ->
        acc + if (vertical) change.scrollDelta.y else change.scrollDelta.x
    }
    if (delta != 0f) {
        scope.launch { state.animateScrollBy(-delta * WHEEL_SCROLL_MULTIPLIER) }
    }
}

private fun Modifier.wheelScroll(state: ScrollState, scope: CoroutineScope): Modifier = onPointerEvent(PointerEventType.Scroll) { event ->
    val deltaY = event.changes.fold(0f) { acc, change -> acc + change.scrollDelta.y }
    if (deltaY != 0f) {
        scope.launch { state.animateScrollBy(-deltaY * WHEEL_SCROLL_MULTIPLIER) }
    }
}

@Composable
fun ScrollableLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    reverseLayout: Boolean = false,
    userScrollEnabled: Boolean = true,
    showScrollbar: Boolean = true,
    content: LazyListScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    Box(modifier = modifier) {
        LazyColumn(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .focusable()
                .keyboardLazyListScroll(state, scope, vertical = true)
                .wheelLazyListScroll(state, scope, vertical = true),
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            reverseLayout = reverseLayout,
            userScrollEnabled = userScrollEnabled,
            content = content
        )
        if (showScrollbar) {
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(state),
                modifier = Modifier
                    .align(CenterEnd)
                    .fillMaxHeight()
                    .padding(end = 2.dp)
            )
        }
    }
}

@Composable
fun ScrollableLazyRow(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    reverseLayout: Boolean = false,
    userScrollEnabled: Boolean = true,
    showScrollbar: Boolean = true,
    content: LazyListScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    Box(modifier = modifier) {
        LazyRow(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .focusable()
                .keyboardLazyListScroll(state, scope, vertical = false)
                .wheelLazyListScroll(state, scope, vertical = false),
            contentPadding = contentPadding,
            horizontalArrangement = horizontalArrangement,
            reverseLayout = reverseLayout,
            userScrollEnabled = userScrollEnabled,
            content = content
        )
        if (showScrollbar) {
            HorizontalScrollbar(
                adapter = rememberScrollbarAdapter(state),
                modifier = Modifier
                    .align(BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 2.dp)
            )
        }
    }
}

@Composable
fun ScrollableLazyVerticalGrid(
    columns: GridCells,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    reverseLayout: Boolean = false,
    userScrollEnabled: Boolean = true,
    showScrollbar: Boolean = true,
    content: LazyGridScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    Box(modifier = modifier) {
        LazyVerticalGrid(
            columns = columns,
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val delta = when (event.key) {
                        Key.DirectionDown, Key.PageDown -> KEY_SCROLL_STEP
                        Key.DirectionUp, Key.PageUp -> -KEY_SCROLL_STEP
                        else -> return@onPreviewKeyEvent false
                    }
                    scope.launch { state.animateScrollBy(delta) }
                    true
                }
                .onPointerEvent(PointerEventType.Scroll) { event ->
                    val deltaY = event.changes.fold(0f) { acc, change -> acc + change.scrollDelta.y }
                    if (deltaY != 0f) {
                        scope.launch { state.animateScrollBy(-deltaY * WHEEL_SCROLL_MULTIPLIER) }
                    }
                },
            contentPadding = contentPadding,
            horizontalArrangement = horizontalArrangement,
            verticalArrangement = verticalArrangement,
            reverseLayout = reverseLayout,
            userScrollEnabled = userScrollEnabled,
            content = content
        )
        if (showScrollbar) {
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(state),
                modifier = Modifier
                    .align(CenterEnd)
                    .fillMaxHeight()
                    .padding(end = 2.dp)
            )
        }
    }
}

@Composable
fun ScrollableColumn(
    modifier: Modifier = Modifier,
    state: ScrollState = rememberScrollState(),
    showScrollbar: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state)
                .focusable()
                .keyboardScroll(state, scope)
                .wheelScroll(state, scope)
        ) {
            content()
        }
        if (showScrollbar) {
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(state),
                modifier = Modifier
                    .align(CenterEnd)
                    .fillMaxHeight()
                    .padding(end = 2.dp)
            )
        }
    }
}
