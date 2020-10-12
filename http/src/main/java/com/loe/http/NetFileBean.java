package com.loe.http;

import com.loe.http.callback.CallBack;

import java.io.File;

import okhttp3.Response;

/**
 * Created by zls
 */
public class NetFileBean
{
    public String msg;

    public boolean success;

    public Response response;

    public File file;

    public Link link;

    public void init(File file, String msg, Response response, Link link)
    {
        success = file != null;

        this.file = file;
        this.msg = msg;
        this.response = response;
        this.link = link;
    }

    public String getHeader(String key)
    {
        if (response != null)
        {
            return response.header(key, "");
        }
        return "";
    }

    public String getHeaderString()
    {
        if (response != null)
        {
            return response.headers().toString();
        }
        return "";
    }

    @Override
    public String toString()
    {
        return msg + "";
    }

    ///////////////////// 扩展 ////////////////////////

    public final NetFileBean success(CallBack callBack)
    {
        if(success)
        {
            callBack.callBack();
        }
        return this;
    }

    public final NetFileBean error(CallBack callBack)
    {
        if(!success)
        {
            callBack.callBack();
        }
        return this;
    }
}