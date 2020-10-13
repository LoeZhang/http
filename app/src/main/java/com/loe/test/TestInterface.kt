package com.loe.test

import com.loe.http.Link
import com.loe.http.annotation.*
import java.io.File

interface TestInterface
{
    @DELETE("test/request.php")
    @JSON
    @Params("""
        kjs = jdhdssssssssssssssss123
        zzzzzlkjsj = hdhbbmxmxn2222
    """)
    fun go(
        @Param("名称") name: String = "大幅度发到付",
        @Header("some") age: Int = 9999999
    ): Link

    @POST("test/request.php")
    @NO_DEAL
    fun pf(
        @Param("ddd是") name: String,
        @ParamFile("文件") file: File
    ): Link

    fun back()
}