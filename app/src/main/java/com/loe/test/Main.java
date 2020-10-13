package com.loe.test;

import com.loe.http.annotation.LoeInvocationHandler;
import java.lang.reflect.Proxy;

class Main
{
    public static void main(String[] args)
    {
//        Class<?> clazz = TestInterface.class;
//        TestInterface tf = (TestInterface) Proxy.newProxyInstance(
//                clazz.getClassLoader(),
//                new Class[]{clazz},
//                new LoeInvocationHandler(""));
//        tf.go("是多少", "333");

        String[] ss = "hdsh;\njdhd".split("[\n|;]+");
        System.out.println(ss[1]);
    }
}