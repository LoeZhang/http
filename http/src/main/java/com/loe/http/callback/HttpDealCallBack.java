package com.loe.http.callback;

import com.loe.http.LoeHttp;
import com.loe.http.NetBean;

public interface HttpDealCallBack
{
    void result(LoeHttp.Link link, NetBean bean);
}
