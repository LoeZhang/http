package com.loe.http.callback;

import com.loe.http.Link;
import com.loe.http.NetBean;

public interface HttpResultDealer
{
    boolean onDeal(Link link, NetBean bean);
}
