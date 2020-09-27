package com.loe.http;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.loe.http.callback.HttpFileCallback;
import com.loe.http.callback.HttpCallBack;
import com.loe.http.callback.HttpDealCallBack;
import com.loe.http.callback.HttpProgressCallBack;
import com.loe.http.callback.HttpStringCallBack;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * OkHttp3封装类
 */
public class LoeHttp
{
    /**
     * http客户端
     */
    private static OkHttpClient httpClient;

    private static final int OK = 1;
    private static final int ERROR = 2;
    private static final int PROGRESS = 3;

    private static HttpDealCallBack okBeanDealer;

    /**
     * 初始化
     * （Application onCreate()中执行）
     */
    public static void init(final Context context)
    {
        HttpFileUtil.init(context);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        // 持久化Cookie
        if (context != null)
        {
            builder.cookieJar(new CookieJar()
            {
                private final PersistentCookieStore cookieStore = new PersistentCookieStore(context);

                @Override
                public void saveFromResponse(HttpUrl url, List<Cookie> cookies)
                {
                    if (cookies != null && cookies.size() > 0)
                    {
                        for (Cookie item : cookies)
                        {
                            cookieStore.add(url, item);
                        }
                    }
                }

                @Override
                public List<Cookie> loadForRequest(HttpUrl url)
                {
                    List<Cookie> cookies = cookieStore.get(url);
                    return cookies;
                }
            });
        }
        builder.connectTimeout(30, TimeUnit.SECONDS);
        builder.readTimeout(30, TimeUnit.SECONDS);
        builder.writeTimeout(30, TimeUnit.SECONDS);
        httpClient = builder.build();
    }

    public static Link link(String url)
    {
        return new Link(url);
    }

    public static Link get(String url)
    {
        final Link link = new Link(url);
        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                link.get();
            }
        }, 10);
        return link;
    }

    public static Link post(String url)
    {
        final Link link = new Link(url);
        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                link.post();
            }
        }, 10);
        return link;
    }

    public static Link getFile(String url)
    {
        final Link link = new Link(url);
        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                link.getFile();
            }
        }, 10);
        return link;
    }

    /**
     * 提醒间隔
     */
    private static int changeTime = 500;

    /**
     * 设置提醒间隔
     */
    public static void setChangeTime(int changeTime)
    {
        LoeHttp.changeTime = changeTime;
    }

    /**
     * 构建url
     */
    public static String buildUrl(String url, HashMap<String, Object> map)
    {
        StringBuffer sb = new StringBuffer(url);
        String c = "?";
        for (Map.Entry<String, Object> entry : map.entrySet())
        {
            sb.append(c + entry.getKey() + "=" + entry.getValue());
            c = "&";
        }
        return sb.toString();
    }

    /**
     * 保存文件
     */
    private static File saveFile(String path, long maxLen, Response response, Link link)
    {
        // 输出文件流
        RandomAccessFile randomAccessFile = null;
        // 网络输入流
        InputStream is = null;
        BufferedInputStream bis = null;
        // 如果文件链接成功
        if (response.isSuccessful())
        {
            try
            {
                // 网络输入流总长度
                final long len = maxLen > 0 ? maxLen : response.body().contentLength();
                link.len = len;

                // 判断剩余空间
                if(HttpFileUtil.getAvailableStorage() < len)
                {
                    link.result = "剩余空间不足";
                    return null;
                }

                // 设置进度灵敏度
                final long dRate = len / changeTime - 1;
                long nowRate = 0;
                // 网络输入流当前长度
                long now = 0;
                // 网络输入流
                is = response.body().byteStream();
                bis = new BufferedInputStream(is);
                // 网络输入缓存
                byte[] buffer = new byte[2048];
                File tempFile = HttpFileUtil.getTemp(path, link.tempFlag);
                if (!tempFile.exists())
                {
                    if (!tempFile.getParentFile().exists())
                    {
                        tempFile.getParentFile().mkdirs();
                    }
                }

                long fl = tempFile.length();
                now = fl > 0 ? fl - 1 : fl;
                link.now = now;
                // 判断是否已下载完成
                if(now < len)
                {
                    randomAccessFile = new RandomAccessFile(tempFile, "rw");
                    randomAccessFile.seek(now);
                    // 临时读取长度
                    int l;
                    while (!link.isEnd && (l = bis.read(buffer)) > 0)
                    {
                        now += l;
                        nowRate += l;
                        randomAccessFile.write(buffer, 0, l);
                        if (nowRate > dRate)
                        {
                            nowRate = 0;
                            // 发送进度至UI线程
                            Message message = new Message();
                            link.now = now;
                            message.obj = link;
                            message.what = PROGRESS;
                            handler.sendMessage(message);
                        }
                    }
                    randomAccessFile.close();
                }
                response.close();
                is.close();
                bis.close();
                if(!link.isEnd)
                {
                    // 发送进度至UI线程
                    Message message = new Message();
                    link.now = now;
                    message.obj = link;
                    message.what = PROGRESS;
                    HttpFileUtil.renameAll(tempFile, path);
                    handler.sendMessage(message);
                    // 完成后清理temp
                    HttpFileUtil.clearTemp();
                }
            } catch (Exception e)
            {
                try
                {
                    if (is != null)
                    {
                        is.close();
                    }
                    if (bis != null)
                    {
                        bis.close();
                    }
                } catch (Exception e0)
                {
                }
                try
                {
                    if (randomAccessFile != null)
                    {
                        randomAccessFile.close();
                    }
                } catch (Exception e0)
                {
                }
            }
        }
        return null;
    }

    private static Handler handler = new Handler(Looper.getMainLooper())
    {
        @Override
        public void handleMessage(Message msg)
        {
            if (msg.what > 0)
            {
                Link link = (Link) msg.obj;
                try
                {
                    switch (msg.what)
                    {
                        case OK:
                            link.isEnd = true;
                            if (link.okCallback != null)
                            {
                                link.okCallback.logic(link.result);
                            }
                            break;
                        case ERROR:
                            link.isEnd = true;
                            if (link.errorCallback != null)
                            {
                                link.errorCallback.logic(link.result);
                            }
                            break;
                        case PROGRESS:
                            if (link.progressCallBack != null)
                            {
                                link.progressCallBack.onChange(link.now, link.len, link.now * 100.0 / link.len);
                            }
                            break;
                    }
                } catch (Exception e)
                {
                    Log.e("HttpRuntime", e + "\nresult: " + link.result);
                    link.isEnd = true;
                    if (link.errorCallback != null)
                    {
                        link.errorCallback.logic(e.toString());
                    }
                }
            }
        }
    };

    public static String stringMap(Map<String, Object> map)
    {
        StringBuilder sb = new StringBuilder();
        String rn = "    ";
        for (Map.Entry<String, Object> entry : map.entrySet())
        {
            sb.append(rn + entry.getKey() + " = " + entry.getValue());
            rn = "\n    ";
        }
        return sb.toString();
    }

    public static void setOkBeanDealer(HttpDealCallBack dealer)
    {
        LoeHttp.okBeanDealer = dealer;
    }

    public static class Link
    {
        private String url;
        private Map<String, Object> headers;
        private HashMap<String, Object> params;
        private JSONObject paramJson;
        private HashMap<String, File> files;
        private String save;
        private HttpStringCallBack okCallback;
        private HttpStringCallBack errorCallback;
        private HttpProgressCallBack progressCallBack;
        private boolean noDealer;
        private long now, len;
        private boolean isAutoName;
        private boolean isUseTemp = false;
        private String tempFlag = "";

        private String result;
        private Response response;

        private boolean isEnd = false;

        private long tempOutTime = 60 * 60 * 1000;

        public Link(String url)
        {
            headers = new HashMap<>();
            params = new HashMap<>();
            files = new HashMap<>();

            String lower = url.toLowerCase();
            if (!lower.startsWith("http:") && !lower.startsWith("https:") && !lower.startsWith("ftp:") && !lower
                    .startsWith("file:"))
            {
                this.url = "http://" + url;
            }else
            {
                this.url = url;
            }
        }

        public String getUrl()
        {
            return url;
        }

        public Link header(String key, String value)
        {
            headers.put(key, value);
            return this;
        }

        public Link param(String key, Object value)
        {
            if (value != null)
            {
                params.put(key, value);
            }
            return this;
        }

        public Link param(HashMap<String, Object> map)
        {
            params.putAll(map);
            return this;
        }

        public Link param(JSONObject json)
        {
            paramJson = json;
            return this;
        }

        public Link isJson()
        {
            paramJson = new JSONObject();
            return this;
        }

        public Link noDealer()
        {
            noDealer = true;
            return this;
        }

        public Link file(String key, File value)
        {
            files.put(key, value);
            return this;
        }

        public Link file(String key, String value)
        {
            files.put(key, new File(value));
            return this;
        }

        public Link save(String path)
        {
            save = path;
            return this;
        }

        public Link save(String path, boolean isAutoName)
        {
            save = path;
            this.isAutoName = isAutoName;
            return this;
        }

        public Link ok(HttpStringCallBack callBack)
        {
            okCallback = callBack;
            return this;
        }

        public Link okBean(final HttpCallBack callBack)
        {
            okCallback = new HttpStringCallBack()
            {
                @Override
                public void logic(String s)
                {
                    final NetBean bean = new NetBean(s);
                    bean.response = response;
                    if (okBeanDealer != null && !noDealer)
                    {
                        okBeanDealer.result(Link.this, bean);
                    }
                    callBack.result(bean);
                }
            };
            return this;
        }

        public String getParamString()
        {
            return stringMap(params);
        }

        public String getHeaderString()
        {
            return stringMap(headers);
        }

        public Link error(HttpStringCallBack callBack)
        {
            errorCallback = callBack;
            return this;
        }

        public Link progress(HttpProgressCallBack callBack)
        {
            progressCallBack = callBack;
            return this;
        }

        public Link useTemp(boolean useTemp)
        {
            isUseTemp = useTemp;
            return this;
        }

        public Link tempFlag(String flag)
        {
            tempFlag = flag;
            return this;
        }

        public Link tempOutTime(long tempOutTime)
        {
            this.tempOutTime = tempOutTime;
            return this;
        }

        public Link get()
        {
            isEnd = false;
            if (url == null || url.isEmpty())
            {
                errorCallback.logic("url非法");
                return this;
            }
            Request.Builder builder = new Request.Builder().url(buildUrl(url, params));
            for (Map.Entry<String, Object> entry : headers.entrySet())
            {
                try
                {
                    builder.addHeader(entry.getKey(), entry.getValue().toString());
                } catch (Exception e)
                {
                }
            }
            httpClient.newCall(builder.build()).enqueue(new Callback()
            {
                @Override
                public void onResponse(Call call, Response response) throws IOException
                {
                    if(!isEnd)
                    {
                        Link.this.response = response;
                        result = response.body().string();
                        Message msg = new Message();
                        msg.what = OK;
                        msg.obj = Link.this;
                        handler.sendMessage(msg);
                    }
                }

                @Override
                public void onFailure(Call call, IOException e)
                {
                    if(!isEnd)
                    {
                        String s = e.getMessage();
                        if (s.contains("timed out"))
                        {
                            s = "连接超时";
                        }
                        result = s.isEmpty() ? e.toString() : s;
                        Message msg = new Message();
                        msg.what = ERROR;
                        msg.obj = Link.this;
                        handler.sendMessage(msg);
                    }
                }
            });
            return this;
        }

        public Link post()
        {
            isEnd = false;
            if (url == null || url.isEmpty())
            {
                errorCallback.logic("url非法");
                return this;
            }
            RequestBody body;
            if (files.isEmpty())
            {
                // Json模式
                if (paramJson != null)
                {
                    MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                    for (Map.Entry<String, Object> entry : params.entrySet())
                    {
                        try {
                            paramJson.put(entry.getKey(), entry.getValue());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    body = RequestBody.create(JSON, paramJson.toString());
                }
                else
                {
                    FormBody.Builder builder = new FormBody.Builder();
                    for (Map.Entry<String, Object> entry : params.entrySet())
                    {
                        builder.add(entry.getKey(), entry.getValue() + "");
                    }
                    body = builder.build();
                }
            }
            else
            {
                MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
                for (Map.Entry<String, Object> entry : params.entrySet())
                {
                    builder.addFormDataPart(entry.getKey(), entry.getValue() + "");
                }
                for (Map.Entry<String, File> entry : files.entrySet())
                {
                    File file = entry.getValue();
                    builder.addFormDataPart("file", file.getName(), RequestBody.create(MediaType.parse("file/*"), file));
                }
                body = builder.build();
            }

            Request.Builder builder = new Request.Builder().url(url).post(body);
            for (Map.Entry<String, Object> entry : headers.entrySet())
            {
                try
                {
                    builder.addHeader(entry.getKey(), entry.getValue().toString());
                } catch (Exception e)
                {
                }
            }
            httpClient.newCall(builder.build()).enqueue(new Callback()
            {
                @Override
                public void onResponse(Call call, Response response) throws IOException
                {
                    if(!isEnd)
                    {
                        Link.this.response = response;
                        result = response.body().string();
                        Message msg = new Message();
                        msg.what = OK;
                        msg.obj = Link.this;
                        handler.sendMessage(msg);
                    }
                }

                @Override
                public void onFailure(Call call, IOException e)
                {
                    if(!isEnd)
                    {
                        String s = e.getMessage();
                        if (s.contains("timed out"))
                        {
                            s = "连接超时";
                        }
                        result = s.isEmpty() ? e.toString() : s;
                        Message msg = new Message();
                        msg.what = ERROR;
                        msg.obj = Link.this;
                        handler.sendMessage(msg);
                    }
                }
            });
            return this;
        }

        public Link getFile()
        {
            isEnd = false;
            if (url == null || url.isEmpty())
            {
                errorCallback.logic("url非法");
                return this;
            }
            String path = (save == null || save.isEmpty() ? HttpFileUtil.basePath + "down/" : save);
            // 无文件名
            if (path.lastIndexOf("/") == path.length() - 1)
            {
                if (isAutoName)
                {
                    path = path + System.currentTimeMillis() + "." + HttpFileUtil.getExtension(url);
                }
                else
                {
                    path = path + HttpFileUtil.getUrlNameExt(url);
                }
            }
            // 无后缀
            else if (path.lastIndexOf(".") < path.length() - 5)
            {
                path = path + "." + HttpFileUtil.getExtension(url);
            }
            result = path;
            // 保存
            if (url.startsWith(HttpFileUtil.ASSETS))
            {
                HttpFileUtil.assetsToFile(url, path, new HttpFileCallback()
                {
                    @Override
                    public void onChange(long len, long now)
                    {
                        if(!isEnd)
                        {
                            Link.this.len = len;
                            Link.this.now = now;
                            Message msg = new Message();
                            msg.what = PROGRESS;
                            msg.obj = Link.this;
                            handler.sendMessage(msg);
                        }
                    }

                    @Override
                    public void response(File file)
                    {
                        if(!isEnd)
                        {
                            Message msg = new Message();
                            msg.what = OK;
                            msg.obj = Link.this;
                            handler.sendMessage(msg);
                        }
                    }

                    @Override
                    public void error()
                    {
                        if(!isEnd)
                        {
                            Message msg = new Message();
                            msg.what = ERROR;
                            msg.obj = Link.this;
                            handler.sendMessage(msg);
                        }
                    }
                });
                return this;
            }

            final Request.Builder builder = new Request.Builder().url(buildUrl(url, params));
            for (Map.Entry<String, Object> entry : headers.entrySet())
            {
                try
                {
                    builder.addHeader(entry.getKey(), entry.getValue().toString());
                } catch (Exception e)
                {
                }
            }

            // 如果temp文件存在
            final File tempFile = HttpFileUtil.getTemp(path, tempFlag);
            if (tempFile.exists() && tempFile.length() > 0)
            {
                // 不支持断点 或者 temp已过期
                if(!isUseTemp || System.currentTimeMillis() - tempFile.lastModified() > tempOutTime )
                {
                    toGetFile(builder, -1);
                    return this;
                }
                // 断点下载
                httpClient.newCall(builder.build()).enqueue(new Callback()
                {
                    @Override
                    public void onFailure(Call call, IOException e)
                    {
                        if(!isEnd)
                        {
                            String s = e.getMessage();
                            result = s.isEmpty() ? e.toString() : s;
                            Message msg = new Message();
                            msg.what = ERROR;
                            msg.obj = Link.this;
                            handler.sendMessage(msg);
                        }
                    }

                    @Override
                    public void onResponse(Call call, Response response)
                    {
                        if(!isEnd)
                        {
                            long contentLength = response.body().contentLength();
                            builder.addHeader("RANGE", "bytes=" + (tempFile.length()-1) + "-");
                            response.close();
                            toGetFile(builder, contentLength);
                        }
                    }
                });
            }else
            {
                toGetFile(builder, -1);
            }
            return this;
        }

        private void toGetFile(Request.Builder builder, final long maxLen)
        {
            httpClient.newCall(builder.build()).enqueue(new Callback()
            {
                @Override
                public void onResponse(Call call, Response response)
                {
                    if(!isEnd)
                    {
                        Link.this.response = response;
                        Message msg = new Message();
                        if(saveFile(result, maxLen, response, Link.this) != null)
                        {
                            if(!isEnd)
                            {
                                msg.what = OK;
                                msg.obj = Link.this;
                                handler.sendMessage(msg);
                            }
                        }else
                        {
                            if(!isEnd)
                            {
                                if(result == null || result.isEmpty())
                                {
                                    result = "下载出错";
                                }
                                msg.what = ERROR;
                                msg.obj = Link.this;
                                handler.sendMessage(msg);
                            }
                        }
                    }
                }

                @Override
                public void onFailure(Call call, IOException e)
                {
                    if(!isEnd)
                    {
                        String s = e.getMessage();
                        result = s.isEmpty() ? e.toString() : s;
                        Message msg = new Message();
                        msg.what = ERROR;
                        msg.obj = Link.this;
                        handler.sendMessage(msg);
                    }
                }
            });
        }

        public boolean isEnd()
        {
            return isEnd;
        }

        public void end()
        {
            isEnd = true;
        }
    }
}