package com.loe.test

import android.app.Application
import android.util.Log
import com.loe.http.LoeHttp
import com.loe.http.NetBean

class App : Application()
{
    override fun onCreate()
    {
        super.onCreate()
        app = this

        LoeHttp.init(app)

//        NetBean.init("message", "status", "data") { code, cs -> code == 200 }
        LoeHttp.setOkBeanDealer()
        { lnik, bean ->
            Log.d("PRETTYLOGGER", "url：${lnik.url}")
            Log.d("PRETTYLOGGER", "params：" + lnik.paramString.replace("\n", " "))
//            if (!lnik.url.contains("area")) LogUtil.dJson(bean.resultString)
            when (bean.code)
            {
                401, 1002, 4254, 4255, 4002, 4007 ->
                {
//                    baseIniter!!.exitLogin(bean.msg)
                    return@setOkBeanDealer
                }
            }
        }
    }

    companion object
    {
        lateinit var app: Application
            private set
    }
}