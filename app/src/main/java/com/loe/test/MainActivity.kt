package com.loe.test

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.loe.http.LoeHttp
import com.loe.http.NetFileBean
import com.loe.mvp.util.PermissionUtil
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.lang.Exception

class MainActivity : AppCompatActivity(), CoroutineScope
{
    protected val activityJob by lazy { Job() }

    override val coroutineContext get() = Dispatchers.Main + activityJob

    override fun onDestroy()
    {
        activityJob.cancel()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var link: LoeHttp.Link? = null
        button.setOnClickListener()
        {
            if (link != null && !link!!.isEnd)
            {
                link!!.end()
            } else
            {
                PermissionUtil.requestForce(this, PermissionUtil.STORAGE, "存储", false)
                {
                    launchMain()
                    {
                        try
                        {
                            link = link("http://wxbtest.iflysec.com/wxb-server/app/appUpdate.apk")
                                .progress()
                                { now, len, p ->
                                    textView.text = "${(p * 10).toInt() / 10.0}%"
                                    Log.d("loeHttp", "${(p * 10).toInt() / 10.0}%")
                                }
                            link!!.withGetFile().error()
                            {
                                delay(1000)
                                textView.text = "加载结束 : $msg"
                                Log.d("loeHttp", "得1111")
                            }
                            delay(1000)
                            textView.text = "加载结束2222"
                            Log.d("loeHttp", "得2121221")
                            delay(1000)
                            textView.text = "end"
                            Log.d("loeHttp", "得end")
                        } catch (e: Exception)
                        {
                            e
                        }
                    }
                }

//                PermissionUtil.requestForce(this, PermissionUtil.STORAGE, "存储", false)
//                {
//                    link =
//                        LoeHttp.getFile("http://wxbtest.iflysec.com/wxb-server/app/appUpdate.apk")
//                            .save(HttpFileUtil.basePath + "down/updateTest.apk")
//                            .useTemp(true)
//                            .tempFlag("1.0.1")
//                            .progress()
//                            { now, len, p ->
//                                textView.text = "${(p * 10).toInt() / 10.0}%"
//                            }
//                            .ok()
//                            {
//                                textView.text = "下载完成！"
//                                HttpFileUtil.clearTemp()
//                            }
//                }
            }
        }
    }
}