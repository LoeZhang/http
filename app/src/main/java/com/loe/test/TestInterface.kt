package com.loe.test

import com.loe.http.Link
import com.loe.http.annotation.*
import org.json.JSONObject
import java.io.File

interface TestInterface {
    @DELETE("test/request.php")
    @JSON
    @Headers(
        "kjs = jdhdssssssssssssssss123",
        "zzzzzlkjsj = hdhbbmxm33333333xn2222"
    )
    @Params(
        "kjs = jdhdssssssssssssssss123",
        "zzzzzlkjsj = hdhbbmxmxn22220000000000"
    )
    fun go(
        @Param("名称") name: String = "大幅度发到付",
        @Param("json测") jc: JSONObject = JSONObject()
            .put("", ""),
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