package com.tunjid.treenav.compose.navigation3.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * A [NavEntryDecorator] that sets a [LocalLifecycleOwner] for each entry that sets the maximum
 * [Lifecycle.State] of that entry to [Lifecycle.State.CREATED] once that entry leaves the
 * [entries]. Entries that are on the back stack are unchanged (they can go to
 * [Lifecycle.State.RESUMED]).
 *
 * @param entries the current back stack of [NavEntry] instances.
 */
@Composable
internal fun <T : Any> rememberBackStackAwareLifecycleNavEntryDecorator(
    entries: List<NavEntry<T>>,
): NavEntryDecorator<T> {
    val updatedEntries by rememberUpdatedState(entries)
    return NavEntryDecorator { entry ->
        val isInBackStack = updatedEntries.fastAnyOrAny { it.contentKey == entry.contentKey }
        val maxLifecycle = if (isInBackStack) Lifecycle.State.RESUMED else Lifecycle.State.CREATED
        val owner = rememberLifecycleOwner(maxLifecycle = maxLifecycle)
        CompositionLocalProvider(LocalLifecycleOwner provides owner) { entry.Content() }
    }
}

internal fun <T> List<T>.fastAnyOrAny(predicate: (T) -> Boolean): Boolean =
    if (this is RandomAccess) {
        this.fastAny(predicate)
    } else {
        @Suppress("ListIterator") this.any(predicate)
    }

/** Helpers copied from compose:ui:ui-util to prevent adding dep on ui-util */
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
private inline fun <T> List<T>.fastForEach(action: (T) -> Unit) {
    contract { callsInPlace(action) }
    for (index in indices) {
        val item = get(index)
        action(item)
    }
}

@Suppress("BanInlineOptIn") // Treat Kotlin Contracts as non-experimental.
@OptIn(ExperimentalContracts::class)
private inline fun <T> List<T>.fastAny(predicate: (T) -> Boolean): Boolean {
    contract { callsInPlace(predicate) }
    fastForEach { if (predicate(it)) return true }
    return false
}
