package dev.floofy.hazel.server.util

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicLong

fun createThreadFactory(name: String): ThreadFactory = object: ThreadFactory {
    private val id = AtomicLong(0)
    private val threadGroup = Thread.currentThread().threadGroup

    override fun newThread(r: Runnable): Thread {
        val t = Thread(threadGroup, r, "Hazel-$name[${id.incrementAndGet()}]")
        if (t.priority != Thread.NORM_PRIORITY)
            t.priority = Thread.NORM_PRIORITY

        return t
    }
}

