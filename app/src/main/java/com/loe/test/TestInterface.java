package com.loe.test;

import com.loe.http.Link;
import com.loe.http.annotation.DELETE;
import com.loe.http.annotation.GET;
import com.loe.http.annotation.IS_JSON;
import com.loe.http.annotation.NO_DEALER;
import com.loe.http.annotation.PUT;
import com.loe.http.annotation.ParamFile;
import com.loe.http.annotation.Header;
import com.loe.http.annotation.LINK;
import com.loe.http.annotation.POST;
import com.loe.http.annotation.Param;
import com.loe.http.annotation.TAG;

import java.io.File;

public interface TestInterface
{
    @DELETE("test/request.php")
    @IS_JSON
    Link go(@Param("名称") String name,
            @Header("some") int age);

    @POST("test/request.php")
    @NO_DEALER
    Link pf(@Param("ddd是") String name,
            @ParamFile("文件") File file);

    void back();
}