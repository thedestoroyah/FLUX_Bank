package com.flux.bank.ui.common

/** Bọc giá trị phát qua LiveData để chỉ được xử lý một lần (tránh lặp lại khi xoay màn hình). */
class Event<out T>(private val content: T) {
    private var handled = false

    fun consume(): T? = if (handled) null else content.also { handled = true }

    fun peek(): T = content
}
