package com.loe.http;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.loe.http.annotation.LoeInvocationHandler;
import com.loe.http.callback.HttpResultDealer;
import com.loe.http.callback.HttpInterceptor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * OkHttp3封装类
 */
public class LoeHttp
{
    /**
     * http客户端
     */
    static OkHttpClient httpClient;

    static final int OK = 1;
    static final int ERROR = 2;
    static final int PROGRESS = 3;

    static ArrayList<HttpInterceptor> interceptors;
    static HttpResultDealer resultDealer;

    /**
     * 初始化
     * （Application onCreate()中执行）
     */
    public static void init(Context context)
    {
        init(context, 30);
    }

    public static void init(final Context context, long timeoutSeconds)
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
        builder.connectTimeout(timeoutSeconds, TimeUnit.SECONDS);
        builder.readTimeout(timeoutSeconds, TimeUnit.SECONDS);
        builder.writeTimeout(timeoutSeconds, TimeUnit.SECONDS);
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

    public static Link put(String url)
    {
        final Link link = new Link(url);
        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                link.put();
            }
        }, 10);
        return link;
    }

    public static Link delete(String url)
    {
        final Link link = new Link(url);
        new Handler().postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                link.delete();
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
     * 下载回调间隔
     */
    private static int changeTime = 500;

    /**
     * 设置下载回调间隔
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
    static File saveFile(String path, long maxLen, Response response, Link link)
    {
        File file = null;
        // 输出文件流
        RandomAccessFile randomAccessFile = null;
        File tempFile2 = null;
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
                if (HttpFileUtil.getAvailableStorage() < len)
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
                tempFile2 = HttpFileUtil.getTemp(path, link.tempFlag);
                if (!tempFile2.exists())
                {
                    if (!tempFile2.getParentFile().exists())
                    {
                        tempFile2.getParentFile().mkdirs();
                    }
                }
                // 清理temp
                if (maxLen < 0 && tempFile2.exists())
                {
                    tempFile2.delete();
                }

                long fl = tempFile2.length();
                now = fl > 0 ? fl - 1 : fl;
                link.now = now;
                // 判断是否已下载完成
                if (now < len)
                {
                    randomAccessFile = new RandomAccessFile(tempFile2, "rw");
                    if (now > 0)
                    {
                        randomAccessFile.seek(now);
                    }
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
                if (!link.isEnd)
                {
                    // 发送进度至UI线程
                    Message message = new Message();
                    link.now = now;
                    message.obj = link;
                    message.what = PROGRESS;
                    file = HttpFileUtil.renameAll(tempFile2, path);
                    handler.sendMessage(message);
                    // 完成后清理temp
                    HttpFileUtil.clearTemp();
                }
                else
                {
                    link.result = "连接中断";
                }
            } catch (Exception e)
            {
                link.result = e.toString();
                file = null;
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
        return file;
    }

    static Handler handler = new Handler(Looper.getMainLooper())
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

    public static void setResultDealer(HttpResultDealer dealer)
    {
        resultDealer = dealer;
    }

    public static void addInterceptor(HttpInterceptor interceptor)
    {
        if (interceptors != null)
        {
            interceptors.add(interceptor);
        }
        else
        {
            interceptors = new ArrayList<>();
            interceptors.add(interceptor);
        }
    }

    public static void removeInterceptor(HttpInterceptor interceptor)
    {
        if (interceptors != null)
        {
            interceptors.remove(interceptor);
        }
    }

    public static void clearInterceptor()
    {
        if (interceptors != null)
        {
            interceptors.clear();
        }
    }

    /**
     * 创建注解接口实例
     */
    public static <T> T create(final Class<T> service, String baseUrl)
    {
        return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[]{service}, new LoeInvocationHandler(baseUrl));
    }

    public static <T> T create(final Class<T> service)
    {
        return create(service, null);
    }
}