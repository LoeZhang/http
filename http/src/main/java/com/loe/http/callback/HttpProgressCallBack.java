package com.loe.http.callback;

/**
 * Created by Administrator on 2016/10/21.
 */
public interface HttpProgressCallBack
{
    void onChange(long now, long len);
}
