package com.loe.http

import org.json.JSONArray
import org.json.JSONObject

/**
 * Created by zls
 */
class NetBean(val resultString: String)
{
    companion object
    {
        var KEY_MSG = "msg"
        var KEY_CODE = "code"
        var KEY_DATA = "data"
        var PASS_SUCCESS = fun(code: Int, cs: String): Boolean
        {
            return code == 200
        }

        fun init(KEY_MSG:String,KEY_CODE:String,KEY_DATA:String,PASS_SUCCESS:(code: Int, cs: String)->Boolean)
        {
            Companion.KEY_MSG = KEY_MSG
            Companion.KEY_CODE = KEY_CODE
            Companion.KEY_DATA = KEY_DATA
            Companion.PASS_SUCCESS = PASS_SUCCESS
        }
    }

    var msg: String
    var code: Int = 0
    var codeString: String = ""
    var success: Boolean = false
    var data: JSONObject
    var array: JSONArray
    var string: String

    init
    {
        val json = JSONObject(resultString)
        msg = json.optString(KEY_MSG, "")
        code = json.optInt(KEY_CODE, 0)
        codeString = json.optString(KEY_CODE, "")
        success = PASS_SUCCESS(code, codeString)
        data = json.optJSONObject(KEY_DATA) ?: JSONObject()
        array = json.optJSONArray(KEY_DATA) ?: JSONArray()
        string = json.optString(KEY_DATA, "")
    }

    fun gotString(key: String, default: String = ""): String
    {
        val s = data.optString(key, default)
        if(s == "null") return default
        return data.optString(key, default) ?: default
    }

    fun gotInt(key: String, default: Int = 0): Int
    {
        return data.optInt(key, default)
    }

    fun gotLong(key: String, default: Long = 0): Long
    {
        return data.optLong(key, default)
    }

    fun gotDouble(key: String, default: Double = 0.0): Double
    {
        return data.optDouble(key, default)
    }

    fun gotBoolean(key: String, default: Boolean = false): Boolean
    {
        return data.optBoolean(key, default)
    }

    fun gotDoubleString(key: String, default: String? = null): String
    {
        val d = gotDouble(key)
        if (default != null && d == 0.0) return default
        val l = d.toLong()
        return if (d - l == 0.0) l.toString() else d.toString()
    }

    fun gotJson(key: String): JSONObject
    {
        var o = data.optJSONObject(key)
        try
        {
            if (this != null && o == null) o = JSONObject(gotString(key))
        } catch (e: Exception)
        {
        }
        return o ?: JSONObject()
    }

    fun gotArray(key: String): JSONArray
    {
        var js = data.optJSONArray(key)
        try
        {
            if (this != null && js == null) js = JSONArray(gotString(key))
        } catch (e: Exception)
        {
        }
        return js ?: JSONArray()
    }

    fun gotString(i: Int, default: String = ""): String
    {
        return array.optString(i, default) ?: default
    }

    fun gotInt(i: Int, default: Int = 0): Int
    {
        return array.optInt(i, default)
    }

    fun gotLong(i: Int, default: Long = 0): Long
    {
        return array.optLong(i, default)
    }

    fun gotDouble(i: Int, default: Double = 0.0): Double
    {
        return array.optDouble(i, default)
    }

    fun gotBoolean(i: Int, default: Boolean = false): Boolean
    {
        return array.optBoolean(i, default)
    }

    fun gotDoubleString(i: Int, default: String? = null): String
    {
        val d = gotDouble(i)
        if (default != null && d == 0.0) return default
        val l = d.toLong()
        return if (d - l == 0.0) l.toString() else d.toString()
    }

    fun gotJson(i: Int): JSONObject
    {
        var o = array.optJSONObject(i)
        try
        {
            if (this != null && o == null) o = JSONObject(gotString(i))
        } catch (e: Exception)
        {
        }
        return o ?: JSONObject()
    }

    fun gotArray(i: Int): JSONArray
    {
        var js = array.optJSONArray(i)
        try
        {
            if (this != null && js == null) js = JSONArray(gotString(i))
        } catch (e: Exception)
        {
        }
        return js ?: JSONArray()
    }

    fun setDataKey(key: String)
    {
        data = gotJson(key)
    }

    fun has(s: String): Boolean
    {
        return data.has(s)
    }

    operator fun get(key: String): Any?
    {
        return data.opt(key)
    }

    operator fun get(i: Int): Any?
    {
        return array.opt(i)
    }

    fun size(): Int
    {
        return array.length()
    }

    override fun toString(): String
    {
        return msg
    }
}