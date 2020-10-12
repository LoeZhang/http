package com.loe.http;

import com.loe.http.callback.HttpPassCallBack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Response;

/**
 * Created by zls
 */
public class NetBean
{
    private String result;

    public String getResult()
    {
        return result;
    }

    public static String KEY_MSG = "msg";
    public static String  KEY_CODE = "code";
    public static String  KEY_DATA = "data";
    public static HttpPassCallBack PASS_SUCCESS = new HttpPassCallBack()
    {
        @Override
        public boolean invoke(int code, String codeString)
        {
            return code == 200;
        }
    };

    public static void init(String KEY_MSG, String KEY_CODE, String KEY_DATA, HttpPassCallBack PASS_SUCCESS)
    {
        NetBean.KEY_MSG = KEY_MSG;
        NetBean.KEY_CODE = KEY_CODE;
        NetBean.KEY_DATA = KEY_DATA;
        NetBean.PASS_SUCCESS = PASS_SUCCESS;
    }

    public String msg;
    public int code;
    public String codeString;
    public boolean success;
    public JSONObject data;
    public JSONArray array;
    public String string;

    public Response response;

    public static final int ERROR_JSON = -604725097;
    public static final int ERROR_LINK = -704725097;
    public static final int ERROR_INTERCEPT = -804725097;

    public static String ERROR_JSON_MSG = "数据格式有误";
    public static String ERROR_LINK_MSG = "网络连接失败";
    public static String ERROR_INTERCEPT_MSG = "请求被拦截";

    public NetBean(String result)
    {
        this.result = result;

        try
        {
            JSONObject json = new JSONObject(result);

            msg = json.optString(KEY_MSG, "");
            code = json.optInt(KEY_CODE, 0);
            codeString = json.optString(KEY_CODE, "");
            success = PASS_SUCCESS.invoke(code, codeString);
            data = json.optJSONObject(KEY_DATA);
            array = json.optJSONArray(KEY_DATA);
            string = json.optString(KEY_DATA, "");

        } catch (JSONException e)
        {
            msg = ERROR_JSON_MSG;
            code = ERROR_JSON;
            codeString = ERROR_JSON + "";
            success = false;
            string = result;
        }

        if(data == null) data = new JSONObject();
        if(array == null) array = new JSONArray();
    }

    public String gotString(String key, String defaultValue)
    {
        String s = data.optString(key, defaultValue);
        if(s == null || s == "null") return defaultValue;
        return s;
    }

    public String gotString(String key)
    {
        return gotString(key, "");
    }

    public int gotInt(String key, int defaultValue)
    {
        return data.optInt(key, defaultValue);
    }

    public int gotInt(String key)
    {
        return gotInt(key,0);
    }

    public long gotLong(String key, long defaultValue)
    {
        return data.optLong(key, defaultValue);
    }

    public long gotLong(String key)
    {
        return gotLong(key, 0);
    }

    public double gotDouble(String key, double defaultValue)
    {
        return data.optDouble(key, defaultValue);
    }

    public double gotDouble(String key)
    {
        return gotDouble(key, 0);
    }

    public boolean gotBoolean(String key, boolean defaultValue)
    {
        return data.optBoolean(key, defaultValue);
    }

    public boolean gotBoolean(String key)
    {
        return gotBoolean(key, false);
    }

    public String gotDoubleString(String key, String defaultValue)
    {
        double d = gotDouble(key, Double.MAX_VALUE);
        if (d == Double.MAX_VALUE) return defaultValue;
        long l = (long) d;
        return d - l == 0 ? l + "" : d + "";
    }

    public String gotDoubleString(String key)
    {
        double d = gotDouble(key);
        long l = (long) d;
        return d - l == 0 ? l + "" : d + "";
    }

    public JSONObject gotJson(String key)
    {
        JSONObject o = data.optJSONObject(key);
        try
        {
            if (o == null)
            {
                return new JSONObject(data.optString(key));
            }
        } catch (Exception e)
        {
        }
        return o;
    }

    public JSONArray gotArray(String key)
    {
        JSONArray o = data.optJSONArray(key);
        try
        {
            if (o == null)
            {
                return new JSONArray(data.optString(key));
            }
        } catch (Exception e)
        {
        }
        return o;
    }

    ////////////////////////////////////////////////////////////

    public String gotString(int i, String defaultValue)
    {
        String s = array.optString(i, defaultValue);
        if(s == null || s == "null") return defaultValue;
        return s;
    }

    public String gotString(int i)
    {
        return gotString(i, "");
    }

    public int gotInt(int i, int defaultValue)
    {
        return array.optInt(i, defaultValue);
    }

    public int gotInt(int i)
    {
        return gotInt(i,0);
    }

    public long gotLong(int i, long defaultValue)
    {
        return array.optLong(i, defaultValue);
    }

    public long gotLong(int i)
    {
        return gotLong(i, 0);
    }

    public double gotDouble(int i, double defaultValue)
    {
        return array.optDouble(i, defaultValue);
    }

    public double gotDouble(int i)
    {
        return gotDouble(i, 0);
    }

    public boolean gotBoolean(int i, boolean defaultValue)
    {
        return array.optBoolean(i, defaultValue);
    }

    public boolean gotBoolean(int i)
    {
        return gotBoolean(i, false);
    }

    public String gotDoubleString(int i, String defaultValue)
    {
        double d = gotDouble(i, Double.MAX_VALUE);
        if (d == Double.MAX_VALUE) return defaultValue;
        long l = (long) d;
        return d - l == 0 ? l + "" : d + "";
    }

    public String gotDoubleString(int i)
    {
        double d = gotDouble(i);
        long l = (long) d;
        return d - l == 0 ? l + "" : d + "";
    }

    public JSONObject gotJson(int i)
    {
        JSONObject o = array.optJSONObject(i);
        try
        {
            if (o == null)
            {
                return new JSONObject(array.optString(i));
            }
        } catch (Exception e)
        {
        }
        return o;
    }

    public JSONArray gotArray(int i)
    {
        JSONArray o = array.optJSONArray(i);
        try
        {
            if (o == null)
            {
                return new JSONArray(array.optString(i));
            }
        } catch (Exception e)
        {
        }
        return o;
    }


    public void setDataKey(String key)
    {
        data = gotJson(key);
    }

    public boolean has(String s)
    {
        return data.has(s);
    }

    public Object get(String key)
    {
        return data.opt(key);
    }

    public Object get(int i)
    {
        return array.opt(i);
    }

    public int size()
    {
        return array.length();
    }

    public String getHeader(String key)
    {
        if(response != null)
        {
            return response.header(key, "");
        }
        return "";
    }

    public String getHeaderString()
    {
        if(response != null)
        {
            return response.headers().toString();
        }
        return "";
    }

    @Override
    public String toString()
    {
        return result;
    }
}