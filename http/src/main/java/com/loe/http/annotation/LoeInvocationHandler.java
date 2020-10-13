package com.loe.http.annotation;

import com.loe.http.Link;
import com.loe.http.LoeHttp;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;

public class LoeInvocationHandler implements InvocationHandler
{
    private String baseUrl;

    public LoeInvocationHandler(String baseUrl)
    {
        if (baseUrl != null)
        {
            this.baseUrl = baseUrl;
        }
        else
        {
            this.baseUrl = "";
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        if (method.getDeclaringClass() == Object.class)
        {
            return method.invoke(this, args);
        }
        Link link = null;

        boolean noDealer = false;
        boolean isJson = false;
        String tag = null;

        HashMap<String, Object> headers = new HashMap<>();
        HashMap<String, Object> params = new HashMap<>();

        if (method.getAnnotations().length > 0)
        {
            for (Annotation type : method.getAnnotations())
            {
                if (type instanceof NO_DEAL)
                {
                    noDealer = true;
                    continue;
                }
                if (type instanceof JSON)
                {
                    isJson = true;
                    continue;
                }
                if (type instanceof TAG)
                {
                    tag = ((TAG) type).value();
                    continue;
                }
                if (type instanceof Headers)
                {
                    String[] hs = ((Headers) type).value();
                    for (String h : hs)
                    {
                        String[] kv = h.trim().split("=");
                        if (kv.length > 1)
                        {
                            headers.put(kv[0].trim(), kv[1].trim());
                        }
                    }
                    continue;
                }
                if (type instanceof Params)
                {
                    String[] hs = ((Params) type).value();
                    for (String h : hs)
                    {
                        String[] kv = h.trim().split("=");
                        if (kv.length > 1)
                        {
                            params.put(kv[0].trim(), kv[1].trim());
                        }
                    }
                    continue;
                }

                if (type instanceof LINK)
                {
                    String url = baseUrl + ((LINK) type).value();
                    link = new Link(url);
                    initLinkInfo(link, method, args);
                }
                else if (type instanceof GET)
                {
                    String url = baseUrl + ((GET) type).value();
                    link = LoeHttp.get(url);
                    initLinkInfo(link, method, args);
                }
                else if (type instanceof GET_FILE)
                {
                    String url = baseUrl + ((GET_FILE) type).value();
                    link = LoeHttp.getFile(url);
                    initLinkInfo(link, method, args);
                }
                else if (type instanceof POST)
                {
                    String url = baseUrl + ((POST) type).value();
                    link = LoeHttp.post(url);
                    initLinkInfo(link, method, args);
                }
                else if (type instanceof PUT)
                {
                    String url = baseUrl + ((PUT) type).value();
                    link = LoeHttp.put(url);
                    initLinkInfo(link, method, args);
                }
                else if (type instanceof DELETE)
                {
                    String url = baseUrl + ((DELETE) type).value();
                    link = LoeHttp.delete(url);
                    initLinkInfo(link, method, args);
                }
            }
        }
        if (link != null)
        {
            link.header(headers);
            link.param(params);
            if (noDealer)
            {
                link.noDealer();
            }
            if (isJson)
            {
                link.isJson();
            }
            if (tag != null)
            {
                link.tag(tag);
            }
        }
        return link;
    }

    private static void initLinkInfo(Link link, Method method, Object[] args)
    {
        Annotation[][] as = method.getParameterAnnotations();
        for (int i = 0; i < as.length; i++)
        {
            if (as[i].length > 0)
            {
                Annotation paramsType = as[i][0];
                String paramKey = null;
                Object paramValue = args[i];
                if (paramsType instanceof Param)
                {
                    paramKey = ((Param) paramsType).value();
                    link.param(paramKey, paramValue);
                }
                else if (paramsType instanceof Header)
                {
                    paramKey = ((Header) paramsType).value();
                    link.header(paramKey, paramValue.toString());
                }
                else if (paramsType instanceof ParamFile)
                {
                    paramKey = ((ParamFile) paramsType).value();
                    if (paramValue instanceof File)
                    {
                        link.file(paramKey, (File) paramValue);
                    }
                    else
                    {
                        link.file(paramKey, paramValue.toString());
                    }
                }
            }
        }
    }
}