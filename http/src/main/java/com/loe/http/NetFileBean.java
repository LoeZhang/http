package com.loe.http;

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

    public LoeHttp.Link link;

    public void init(File file, String msg, Response response, LoeHttp.Link link)
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
}