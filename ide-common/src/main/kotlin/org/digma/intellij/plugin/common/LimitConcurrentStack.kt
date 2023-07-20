package org.digma.intellij.plugin.common

import java.util.Deque
import java.util.concurrent.ConcurrentLinkedDeque

class LimitConcurrentStack<E>(val maxElements: Int) : Deque<E> {

    protected val impl: ConcurrentLinkedDeque<E> = ConcurrentLinkedDeque()

    override fun push(element: E) {
        if (impl.size >= maxElements) impl.pollLast()
        impl.push(element)
    }

    // -----------------------
    // delegate
    // -----------------------

    override fun peek(): E {
        return impl.peek()
    }

    override fun pop(): E {
        return impl.pop()
    }

    override val size: Int
        get() = impl.size

    override fun clear() {
        impl.clear()
    }

    override fun iterator(): MutableIterator<E> {
        return impl.iterator()
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        return impl.retainAll(elements)
    }

    override fun isEmpty(): Boolean {
        return impl.isEmpty()
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        return impl.containsAll(elements)
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        return impl.removeAll(elements)
    }

    override fun equals(other: Any?): Boolean {
        if (other is Deque<*>) {
            return impl.equals(other)
        }
        return false
    }

    override fun hashCode(): Int {
        return impl.hashCode()
    }

    override fun toString(): String {
        return impl.toString()
    }

    // -----------------------
    // unsupported ones
    // -----------------------
    private fun unsupportedBool(): Boolean {
        throw UnsupportedOperationException("unsupported")
    }

    private fun unsupportedElement(): E {
        throw UnsupportedOperationException("unsupported")
    }

    override fun add(element: E): Boolean {
        return unsupportedBool()
    }

    override fun addAll(elements: Collection<E>): Boolean {
        return unsupportedBool()
    }

    override fun remove(): E {
        return unsupportedElement()
    }

    override fun remove(element: E): Boolean {
        return unsupportedBool()
    }

    override fun contains(element: E): Boolean {
        return unsupportedBool()
    }

    override fun offer(e: E): Boolean {
        return unsupportedBool()
    }

    override fun poll(): E {
        return unsupportedElement()
    }

    override fun element(): E {
        return unsupportedElement()
    }

    override fun addFirst(e: E) {
        unsupportedElement()
    }

    override fun addLast(e: E) {
        unsupportedElement()
    }

    override fun offerFirst(e: E): Boolean {
        return unsupportedBool()
    }

    override fun offerLast(e: E): Boolean {
        return unsupportedBool()
    }

    override fun removeFirst(): E {
        return unsupportedElement()
    }

    override fun removeLast(): E {
        return unsupportedElement()
    }

    override fun pollFirst(): E {
        return unsupportedElement()
    }

    override fun pollLast(): E {
        return unsupportedElement()
    }

    override fun getFirst(): E {
        return unsupportedElement()
    }

    override fun getLast(): E {
        return unsupportedElement()
    }

    override fun peekFirst(): E {
        return unsupportedElement()
    }

    override fun peekLast(): E {
        return unsupportedElement()
    }

    override fun removeFirstOccurrence(o: Any?): Boolean {
        return unsupportedBool()
    }

    override fun removeLastOccurrence(o: Any?): Boolean {
        return unsupportedBool()
    }

    override fun descendingIterator(): MutableIterator<E> {
        throw UnsupportedOperationException()
    }

}
