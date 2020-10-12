package com.loe.http;

import android.os.Message;

import com.loe.http.callback.HttpCallBack;
import com.loe.http.callback.HttpFileCallback;
import com.loe.http.callback.HttpInterceptor;
import com.loe.http.callback.HttpProgressCallBack;
import com.loe.http.callback.HttpStringCallBack;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.loe.http.LoeHttp.buildUrl;
import static com.loe.http.LoeHttp.handler;
import static com.loe.http.LoeHttp.httpClient;
import static com.loe.http.LoeHttp.OK;
import static com.loe.http.LoeHttp.ERROR;
import static com.loe.http.LoeHttp.PROGRESS;
import static com.loe.http.LoeHttp.resultDealer;
import static com.loe.http.LoeHttp.interceptors;
import static com.loe.http.LoeHttp.saveFile;
import static com.loe.http.LoeHttp.stringMap;

public class Link
{
    String url;
    String tag = "";
    private Map<String, Object> headers;
    private HashMap<String, Object> params;
    private JSONObject paramJson;
    private HashMap<String, File> files;
    private String save;
    HttpStringCallBack okCallback;
    HttpStringCallBack errorCallback;
    HttpProgressCallBack progressCallBack;
    private boolean noDealer;
    long now, len;
    private boolean isAutoName;
    private boolean isUseTemp = false;
    String tempFlag = "";

    String result;
    private Response response;

    boolean isEnd = false;

    private long tempOutTime = 60 * 60 * 1000;

    public Link(String url)
    {
        headers = new HashMap<>();
        params = new HashMap<>();
        files = new HashMap<>();

        String lower = url.toLowerCase();
        if (!lower.startsWith("http:") && !lower.startsWith("https:") && !lower.startsWith("ftp:") && !lower.startsWith("file:"))
        {
            this.url = "http://" + url;
        }
        else
        {
            this.url = url;
        }
    }

    public String getUrl()
    {
        return url;
    }

    public String getTag()
    {
        return tag;
    }

    public Link tag(String tag)
    {
        this.tag = tag;
        return this;
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

    public Link okString(final HttpStringCallBack callBack)
    {
        okCallback = callBack;
        return this;
    }

    public Link ok(final HttpCallBack callBack)
    {
        okCallback = new HttpStringCallBack()
        {
            @Override
            public void logic(String s)
            {
                final NetBean bean = new NetBean(s);
                bean.response = response;
                if (resultDealer != null && !noDealer)
                {
                    if(!resultDealer.onDeal(Link.this, bean))
                    {
                        callBack.result(bean);
                    }
                }else
                {
                    callBack.result(bean);
                }
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

    /**
     * 同步get请求
     *
     * @return
     */
    public Response syncResponseGet() throws IOException
    {
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
        return httpClient.newCall(builder.build()).execute();
    }

    /**
     * 同步post请求
     */
    public Response syncResponsePost() throws IOException
    {
        return syncResponseBody("post");
    }

    /**
     * 同步put请求
     */
    public Response syncResponsePut() throws IOException
    {
        return syncResponseBody("put");
    }

    /**
     * 同步delete请求
     */
    public Response syncResponseDelete() throws IOException
    {
        return syncResponseBody("delete");
    }

    private Response syncResponseBody(String type) throws IOException
    {
        RequestBody body;
        if (files.isEmpty())
        {
            // Json模式
            if (paramJson != null)
            {
                MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                for (Map.Entry<String, Object> entry : params.entrySet())
                {
                    try
                    {
                        paramJson.put(entry.getKey(), entry.getValue());
                    } catch (JSONException e)
                    {
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

        Request.Builder builder;
        switch (type)
        {
            case "put":
                builder = new Request.Builder().url(url).put(body);
                break;
            case "delete":
                builder = new Request.Builder().url(url).delete(body);
                break;
            default:
                builder = new Request.Builder().url(url).post(body);
                break;
        }
        for (Map.Entry<String, Object> entry : headers.entrySet())
        {
            try
            {
                builder.addHeader(entry.getKey(), entry.getValue().toString());
            } catch (Exception e)
            {
            }
        }
        return httpClient.newCall(builder.build()).execute();
    }

    /**
     * 同步请求Bean
     *
     * @param type 请求类型
     */
    private NetBean syncBean(String type)
    {
        NetBean bean = null;
        if(intercept())
        {
            bean = new NetBean("");
            bean.msg = NetBean.ERROR_INTERCEPT_MSG;
            bean.code = NetBean.ERROR_INTERCEPT;
            bean.codeString = NetBean.ERROR_INTERCEPT + "";
            return bean;
        }

        try
        {
            response = null;
            switch (type)
            {
                case "get":
                    response = syncResponseGet();
                    break;
                case "put":
                    response = syncResponsePut();
                    break;
                case "delete":
                    response = syncResponseDelete();
                    break;
                default:
                    response = syncResponsePost();
                    break;
            }

            bean = new NetBean(response.body().string());
            bean.response = response;
        } catch (IOException e)
        {
            e.printStackTrace();
            bean = new NetBean(e.toString());
            bean.msg = NetBean.ERROR_LINK_MSG;
            bean.code = NetBean.ERROR_LINK;
            bean.codeString = NetBean.ERROR_LINK + "";
        }
        if (resultDealer != null && !noDealer)
        {
            final NetBean fBean = bean;
            handler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    resultDealer.onDeal(Link.this, fBean);
                }
            });
        }
        return bean;
    }

    public NetBean syncGet()
    {
        return syncBean("get");
    }

    public NetBean syncPost()
    {
        return syncBean("post");
    }

    public NetBean syncPut()
    {
        return syncBean("put");
    }

    public NetBean syncDelete()
    {
        return syncBean("delete");
    }

    public NetFileBean syncGetFile()
    {
        isEnd = false;

        NetFileBean bean = new NetFileBean();

        if(intercept())
        {
            bean.init(null, NetBean.ERROR_INTERCEPT_MSG, null, this);
            return bean;
        }

        try
        {
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
                bean.init(HttpFileUtil.assetsToFile(url, path), result, null, null);
                return bean;
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
                if (!isUseTemp || System.currentTimeMillis() - tempFile.lastModified() > tempOutTime)
                {
                    File file = toSyncGetFile(builder, -1);
                    bean.init(file, result, response, this);
                }
                else
                {
                    // 断点下载
                    Response response = httpClient.newCall(builder.build()).execute();
                    long contentLength = response.body().contentLength();
                    builder.addHeader("RANGE", "bytes=" + (tempFile.length() - 1) + "-");
                    response.close();

                    File file = toSyncGetFile(builder, contentLength);
                    bean.init(file, result, response, this);
                }
            }
            else
            {
                File file = toSyncGetFile(builder, -1);
                bean.init(file, result, response, this);
            }
        } catch (Exception e)
        {
            bean.init(null, result, null, this);
        }
        return bean;
    }

    private File toSyncGetFile(Request.Builder builder, long maxLen) throws Exception
    {
        response = httpClient.newCall(builder.build()).execute();
        File file = saveFile(result, maxLen, response, Link.this);
        if (file != null)
        {
            return file;
        }
        else
        {
            throw new Exception(result);
        }
    }

    /**
     * get请求
     */
    public Link get()
    {
        if(intercept()) return this;

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
                if (!isEnd)
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
                if (!isEnd)
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

    /**
     * post请求
     */
    public Link post()
    {
        return requestBody("post");
    }

    /**
     * put请求
     */
    public Link put()
    {
        return requestBody("put");
    }

    /**
     * delete请求
     */
    public Link delete()
    {
        return requestBody("delete");
    }

    private Link requestBody(String type)
    {
        if(intercept()) return this;

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
                    try
                    {
                        paramJson.put(entry.getKey(), entry.getValue());
                    } catch (JSONException e)
                    {
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
        Request.Builder builder;
        switch (type)
        {
            case "put":
                builder = new Request.Builder().url(url).put(body);
                break;
            case "delete":
                builder = new Request.Builder().url(url).delete(body);
                break;
            default:
                builder = new Request.Builder().url(url).post(body);
                break;
        }
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
                if (!isEnd)
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
                if (!isEnd)
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
        if(intercept()) return this;

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
                    if (!isEnd)
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
                    if (!isEnd)
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
                    if (!isEnd)
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
            if (!isUseTemp || System.currentTimeMillis() - tempFile.lastModified() > tempOutTime)
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
                    if (!isEnd)
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
                    if (!isEnd)
                    {
                        long contentLength = response.body().contentLength();
                        builder.addHeader("RANGE", "bytes=" + (tempFile.length() - 1) + "-");
                        response.close();
                        toGetFile(builder, contentLength);
                    }
                }
            });
        }
        else
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
                if (!isEnd)
                {
                    Link.this.response = response;
                    Message msg = new Message();
                    if (saveFile(result, maxLen, response, Link.this) != null)
                    {
                        if (!isEnd)
                        {
                            msg.what = OK;
                            msg.obj = Link.this;
                            handler.sendMessage(msg);
                        }
                    }
                    else
                    {
                        if (!isEnd)
                        {
                            if (result == null || result.isEmpty())
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
                if (!isEnd)
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

    /**
     * 拦截处理
     */
    private boolean intercept()
    {
        if(interceptors != null)
        {
            for (HttpInterceptor interceptor : interceptors)
            {
                if(interceptor.intercept(this))
                {
                    return true;
                }
            }
        }
        return false;
    }
}