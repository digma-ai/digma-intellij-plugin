package org.digma.intellij.plugin.collections

import com.intellij.util.containers.HashSetQueue
import java.util.Queue

class SynchronizedHashSetQueue<T> : Queue<T> {

    private val delegate = HashSetQueue<T>()
    private val lock = Any()

    override val size: Int
        get() = synchronized(lock) { delegate.size }

    override fun isEmpty(): Boolean = synchronized(lock) {
        delegate.isEmpty()
    }

    override fun contains(element: T): Boolean = synchronized(lock) {
        delegate.contains(element)
    }

    override fun iterator(): MutableIterator<T> = synchronized(lock) {
        ArrayList(delegate).iterator() // Snapshot iterator
    }

    override fun remove(element: T): Boolean = synchronized(lock) {
        delegate.remove(element)
    }

    override fun add(element: T): Boolean = synchronized(lock) {
        return@synchronized element?.let {
            delegate.add(it)
        } ?: false
    }

    override fun offer(element: T): Boolean = synchronized(lock) {
        return@synchronized element?.let {
            delegate.offer(it)
        } ?: false
    }

    override fun remove(): T = synchronized(lock) {
        delegate.remove()
    }

    override fun poll(): T? = synchronized(lock) {
        delegate.poll()
    }

    override fun element(): T = synchronized(lock) {
        delegate.element()
    }

    override fun peek(): T? = synchronized(lock) {
        delegate.peek()
    }

    override fun containsAll(elements: Collection<T>): Boolean = synchronized(lock) {
        delegate.containsAll(elements)
    }

    override fun addAll(elements: Collection<T>): Boolean = synchronized(lock) {
        delegate.addAll(elements)
    }

    override fun removeAll(elements: Collection<T>): Boolean = synchronized(lock) {
        delegate.removeAll(elements)
    }

    override fun retainAll(elements: Collection<T>): Boolean = synchronized(lock) {
        delegate.retainAll(elements)
    }

    override fun clear() = synchronized(lock) {
        delegate.clear()
    }
}