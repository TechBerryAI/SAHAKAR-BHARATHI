package com.example.sahakarbharati.data

import com.example.sahakarbharati.data.repository.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren

/**
 * Drops user-specific ViewModel state when the signed-in account changes.
 * A request keeps the epoch it started with and must not write after that epoch moves.
 */
class SessionWatch(
    private val session: SessionStore,
    private val scope: CoroutineScope,
    private val cancelWork: Boolean = true,
    private val onChange: () -> Unit,
) {
    private val listener: (Int) -> Unit = {
        if (cancelWork) scope.coroutineContext.cancelChildren()
        onChange()
    }

    init {
        session.addEpochListener(listener)
    }

    fun close() {
        session.removeEpochListener(listener)
    }

    fun stamp(): Int = session.epoch()

    fun still(stamp: Int): Boolean = stamp == session.epoch()
}
