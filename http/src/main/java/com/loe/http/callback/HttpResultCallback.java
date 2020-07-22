package com.loe.http.callback;

/**
 * Created by zls on 2016/12/20.
 */
public abstract class HttpResultCallback
{
    /**
     * 请求结果
     */
    public String result;
    /**
     * 请求地址
     */
    public String url;

    /**
     * 请求回应
     */
    public abstract void response(String result) throws Exception;

    /**
     * 错误回应
     */
    public abstract void error();
}
