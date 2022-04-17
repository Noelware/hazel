package dev.floofy.hazel.core

import kotlinx.atomicfu.atomic
import java.util.concurrent.ThreadFactory

fun createThreadFactory(name: String): ThreadFactory = object: ThreadFactory {
    private val id = atomic(0L)
    private val threadGroup = Thread.currentThread().threadGroup

    override fun newThread(r: Runnable): Thread {
        val t = Thread(threadGroup, r, "Hazel-$name[${id.incrementAndGet()}]")
        if (t.priority != Thread.NORM_PRIORITY)
            t.priority = Thread.NORM_PRIORITY

        return t
    }
}
