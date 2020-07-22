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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private static File saveFile(String path, Response response, Link link)
    {
        File file = null;
        // 输出文件流
        FileOutputStream fos = null;
        // 网络输入流
        InputStream is = null;
        // 如果文件链接成功
        if (response.isSuccessful())
        {
            try
            {
                // 网络输入流总长度
                final long len = response.body().contentLength();
                link.len = len;
                // 设置进度灵敏度
                final long dRate = len / changeTime - 1;
                long nowRate = 0;
                // 网络输入流当前长度
                long now = 0;
                // 网络输入流
                is = response.body().byteStream();
                // 网络输入缓存
                byte[] buffer = initBuffer(len);
                file = new File(path + ".temp");
                // 如果文件存在
                if (file.exists())
                {
                    file.delete();
                }
                else
                // 如果文件夹路径不存在，则创建路径
                {
                    if (!file.getParentFile().exists())
                    {
                        file.getParentFile().mkdirs();
                    }
                }
                fos = new FileOutputStream(file, true);
                // 临时读取长度
                int l;
                while ((l = is.read(buffer)) != -1)
                {
                    now += l;
                    nowRate += l;
                    fos.write(buffer, 0, l);
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
                // 发送进度至UI线程
                Message message = new Message();
                link.now = now;
                message.obj = link;
                message.what = PROGRESS;
                fos.flush();
                is.close();
                fos.close();
                file = HttpFileUtil.renameAll(file, path);
                handler.sendMessage(message);
            } catch (Exception e)
            {
                try
                {
                    if (is != null)
                    {
                        is.close();
                    }
                } catch (Exception e0)
                {
                }
                try
                {
                    if (fos != null)
                    {
                        fos.close();
                    }
                } catch (Exception e0)
                {
                }
            }
        }
        return file;
    }

    /**
     * 获取适当的缓存
     */
    private static byte[] initBuffer(long len)
    {
        final int K = 1024;
        final int M = 1024 * 1024;
        byte[] buffer = null;
        if (len < 128 * K)
        {
            buffer = new byte[5 * K];
        }
        else
        {
            if (len < 1 * M)
            {
                buffer = new byte[20 * K];
            }
            else
            {
                if (len < 8 * M)
                {
                    buffer = new byte[256 * K];
                }
                else
                {
                    if (len < 32 * M)
                    {
                        buffer = new byte[1 * M];
                    }
                    else
                    {
                        buffer = new byte[2 * M];
                    }
                }
            }
        }
        return buffer;
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
                            if (link.okCallback != null)
                            {
                                link.okCallback.logic(link.result);
                            }
                            break;
                        case ERROR:
                            if (link.errorCallback != null)
                            {
                                link.errorCallback.logic(link.result);
                            }
                            break;
                        case PROGRESS:
                            if (link.progressCallBack != null)
                            {
                                link.progressCallBack.onChange(link.now, link.len);
                            }
                            break;
                    }
                } catch (Exception e)
                {
                    Log.e("HttpRuntime", e + "\nresult: " + link.result);
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
        private HashMap<String, String> headers;
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

        private String result;

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

        public Link get()
        {
            if (url == null || url.isEmpty())
            {
                errorCallback.logic("url非法");
                return this;
            }
            Request.Builder builder = new Request.Builder().url(buildUrl(url, params));
            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                try
                {
                    builder.addHeader(entry.getKey(), entry.getValue());
                } catch (Exception e)
                {
                }
            }
            httpClient.newCall(builder.build()).enqueue(new Callback()
            {
                @Override
                public void onResponse(Call call, Response response) throws IOException
                {
                    result = response.body().string();
                    Message msg = new Message();
                    msg.what = OK;
                    msg.obj = Link.this;
                    handler.sendMessage(msg);
                }

                @Override
                public void onFailure(Call call, IOException e)
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
            });
            return this;
        }

        public Link post()
        {
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
            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                try
                {
                    builder.addHeader(entry.getKey(), entry.getValue());
                } catch (Exception e)
                {
                }
            }
            httpClient.newCall(builder.build()).enqueue(new Callback()
            {
                @Override
                public void onResponse(Call call, Response response) throws IOException
                {
                    result = response.body().string();
                    Message msg = new Message();
                    msg.what = OK;
                    msg.obj = Link.this;
                    handler.sendMessage(msg);
                }

                @Override
                public void onFailure(Call call, IOException e)
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
            });
            return this;
        }

        public Link getFile()
        {
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
                        Link.this.len = len;
                        Link.this.now = now;
                        Message msg = new Message();
                        msg.what = PROGRESS;
                        msg.obj = Link.this;
                        handler.sendMessage(msg);
                    }

                    @Override
                    public void response(File file)
                    {
                        Message msg = new Message();
                        msg.what = OK;
                        msg.obj = Link.this;
                        handler.sendMessage(msg);
                    }

                    @Override
                    public void error()
                    {
                        Message msg = new Message();
                        msg.what = ERROR;
                        msg.obj = Link.this;
                        handler.sendMessage(msg);
                    }
                });
                return this;
            }

            Request.Builder builder = new Request.Builder().url(buildUrl(url, params));
            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                try
                {
                    builder.addHeader(entry.getKey(), entry.getValue());
                } catch (Exception e)
                {
                }
            }
            httpClient.newCall(builder.build()).enqueue(new Callback()
            {
                @Override
                public void onResponse(Call call, Response response)
                {
                    Message msg = new Message();
                    saveFile(result, response, Link.this);
                    msg.what = OK;
                    msg.obj = Link.this;
                    handler.sendMessage(msg);
                }

                @Override
                public void onFailure(Call call, IOException e)
                {
                    String s = e.getMessage();
                    result = s.isEmpty() ? e.toString() : s;
                    Message msg = new Message();
                    msg.what = ERROR;
                    msg.obj = Link.this;
                    handler.sendMessage(msg);
                }
            });
            return this;
        }
    }
}