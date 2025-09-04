package com.example.social_media
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UnreadCoordinator {

    private val _unread = MutableStateFlow(0)
    val unread: StateFlow<Int> = _unread.asStateFlow()

    fun setInitial(count: Int) {
        _unread.value = count
    }

    fun increment() {
        _unread.value = _unread.value + 1
    }

    fun decrementBy(amount: Int) {
        _unread.value = maxOf(0, _unread.value - amount)
    }

    fun decrement() {
        decrementBy(1)
    }

    fun reset() {
        _unread.value = 0
    }

    fun getCurrentCount(): Int = _unread.value
}