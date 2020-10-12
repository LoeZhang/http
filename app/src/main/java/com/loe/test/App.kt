package com.loe.test

import android.app.Application
import android.util.Log
import com.loe.http.LoeHttp
import com.loe.logger.LoeLogger

class App : Application()
{
    override fun onCreate()
    {
        super.onCreate()
        app = this

        LoeHttp.init(app)
        LoeLogger.init(app, false)
//        NetBean.init("message", "status", "data") { code, cs -> code == 200 }
        LoeHttp.setResultDealer()
        { link, bean ->
            LoeLogger.net(link.url, link.paramString, link.headerString, bean.result)
            when (bean.code)
            {
                401, 1002, 4254, 4255, 4002, 4007 ->
                {
//                    start(LoginActivity::class)
//                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    return@setResultDealer true
                }
            }
            false
        }

        LoeHttp.addInterceptor()
        {link ->
            link.header("qqzls", "23924458477834")
            link.param("傻子", 122333)
            false
        }
    }

    companion object
    {
        lateinit var app: Application
            private set
    }
}