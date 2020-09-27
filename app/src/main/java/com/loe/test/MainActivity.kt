package com.loe.test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.loe.http.HttpFileUtil
import com.loe.http.LoeHttp
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var link : LoeHttp.Link? = null
        button.setOnClickListener()
        {
            if(link !=null && !link!!.isEnd)
            {
                link?.end()
            }else
            {
                link = LoeHttp.getFile("http://wxbtest.iflysec.com/wxb-server/app/appUpdate.apk")
                    .save(HttpFileUtil.basePath + "down/updateTest.apk")
//                    .useTemp(true)
//                    .tempFlag("1.0.1")
                    .progress()
                    {now,len,p->
                        textView.text = "${(p*10).toInt() / 10.0}%"
                    }
                    .ok()
                    {
                        textView.text = "下载完成！"
                        HttpFileUtil.clearTemp()
                    }
            }
        }
    }
}