package com.loe.http.callback;

import java.io.File;

/**
 * Created by zls on 2016/12/20.
 */
public abstract class FileCallback extends ResultCallback
{
    /**
     * 文件
     */
    public File file;
    /**
     * 总长度
     */
    public long len;
    /**
     * 当前长度
     */
    public long now;

    @Override
    public void response(String result)
    {
    }

    /** 下载进度监听 */
    public abstract void onChange(long len,long now);

    /** 请求回应 */
    public abstract void response(File file) throws Exception;
}
