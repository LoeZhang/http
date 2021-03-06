package com.loe.test

import com.loe.http.Link
import com.loe.http.NetBean
import com.loe.http.NetFileBean
import kotlinx.coroutines.*

suspend fun Link.withGet(): NetBean
{
    return withIOContext()
    {
        syncGet()
    }
}

suspend fun Link.withGetFile(): NetFileBean
{
    return withIOContext()
    {
        syncGetFile()
    }
}

suspend fun Link.withPost(): NetBean
{
    return withIOContext()
    {
        syncPost()
    }
}

suspend fun Link.withPut(): NetBean
{
    return withIOContext()
    {
        syncPut()
    }
}

suspend fun Link.withDelete(): NetBean
{
    return withIOContext()
    {
        syncDelete()
    }
}

suspend fun <T> withMainContext(block: suspend CoroutineScope.() -> T): T
{
    return withContext(Dispatchers.IO, block)
}

suspend fun <T> withIOContext(block: suspend CoroutineScope.() -> T): T
{
    return withContext(Dispatchers.IO, block)
}

fun GlobalScope.launchMain(
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job
{
    return GlobalScope.launch(Dispatchers.Main, start, block)
}

fun CoroutineScope.launchMain(
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job
{
    return launch(Dispatchers.Main, start, block)
}