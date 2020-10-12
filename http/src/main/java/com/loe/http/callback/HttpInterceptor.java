package com.loe.http.callback;

import com.loe.http.Link;

public interface HttpInterceptor
{
    boolean intercept(Link link);
}
