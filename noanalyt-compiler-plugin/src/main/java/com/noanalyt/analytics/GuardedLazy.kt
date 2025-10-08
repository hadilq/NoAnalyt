package com.noanalyt.analytics

import kotlin.reflect.KProperty

internal object UNINITIALIZED_VALUE

class GuardedLazy<out T>(initializer: () -> T) {
    private var _value: Any? = UNINITIALIZED_VALUE
    private var _initializer: (() -> T)? = initializer

    fun value(name: String): T {
        if (_value === UNINITIALIZED_VALUE) {
            try {
                _value = _initializer!!()
                _initializer = null
            } catch (e: Throwable) {
                throw java.lang.IllegalStateException("Error initializing $name", e)
            }
        }
        @Suppress("UNCHECKED_CAST")
        return _value as T
    }
}

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> GuardedLazy<T>.getValue(thisRef: Any?, property: KProperty<*>) =
    value(property.name)

fun <T> guardedLazy(initializer: () -> T) = GuardedLazy<T>(initializer)
