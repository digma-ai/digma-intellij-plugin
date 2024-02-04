package org.digma.intellij.plugin.common

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Computable
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier


fun executeCatching(runnable: Runnable, onException: Consumer<Throwable>) {
    try {
        runnable.run()
    } catch (e: Throwable) {
        onException.accept(e)
    }
}


fun executeCatching(runnable: Runnable, onException: Consumer<Throwable>, onFinally: Runnable) {
    try {
        runnable.run()
    } catch (e: Throwable) {
        onException.accept(e)
    } finally {
        onFinally.run()
    }
}


fun <T> executeCatching(computable: Computable<T>, onException: Function<Throwable, T>): T {
    return try {
        computable.compute()
    } catch (e: Throwable) {
        onException.apply(e)
    }
}

fun <T> executeCatching(computable: Computable<T>, onException: Function<Throwable, T>, onFinally: Supplier<T>): T {
    return try {
        computable.compute()
    } catch (e: Throwable) {
        onException.apply(e)
    } finally {
        onFinally.get()
    }
}


fun executeCatchingIgnorePCE(runnable: Runnable, onException: Consumer<Throwable>, onFinally: Runnable? = null) {
    try {
        runnable.run()
    } catch (e: ProcessCanceledException) {
        throw e
    } catch (e: Throwable) {
        onException.accept(e)
    } finally {
        onFinally?.run()
    }
}

fun <T> executeCatchingWithResultIgnorePCE(computable: Computable<T>, onException: Function<Throwable, T>, onFinally: Runnable? = null): T {
    return try {
        computable.compute()
    } catch (e: ProcessCanceledException) {
        throw e
    } catch (e: Throwable) {
        onException.apply(e)
    } finally {
        onFinally?.run()
    }
}