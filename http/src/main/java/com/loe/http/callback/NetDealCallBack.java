package com.loe.http.callback;

import com.loe.http.LoeHttp;
import com.loe.http.NetBean;

public interface NetDealCallBack
{
    void result(LoeHttp.Link link, NetBean bean);
}
