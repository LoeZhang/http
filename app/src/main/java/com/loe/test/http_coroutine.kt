package com.loe.test

import com.loe.http.LoeHttp
import com.loe.http.NetBean
import com.loe.http.NetFileBean
import kotlinx.coroutines.*
import java.io.File

fun link(url: String) = LoeHttp.Link(url)

suspend fun LoeHttp.Link.withGet(): NetBean
{
    return withIOContext()
    {
        syncGet()
    }
}

suspend fun LoeHttp.Link.withPost(): NetBean
{
    return withIOContext()
    {
        syncPost()
    }
}


suspend fun LoeHttp.Link.withGetFile(): NetFileBean
{
    return withIOContext()
    {
        syncGetFile()
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

inline fun NetBean.result(block: NetBean.() -> Unit): NetBean
{
    if (code != NetBean.ERROR_LINK)
    {
        block()
    }
    return this
}

inline fun NetBean.ok(block: NetBean.() -> Unit): NetBean
{
    if (success)
    {
        block()
    }
    return this
}

inline fun NetBean.error(block: NetBean.() -> Unit): NetBean
{
    if (!success)
    {
        block()
    }
    return this
}

inline fun NetFileBean.ok(block: NetFileBean.() -> Unit): NetFileBean
{
    if (success)
    {
        block()
    }
    return this
}

inline fun NetFileBean.error(block: NetFileBean.() -> Unit): NetFileBean
{
    if (!success)
    {
        block()
    }
    return this
}