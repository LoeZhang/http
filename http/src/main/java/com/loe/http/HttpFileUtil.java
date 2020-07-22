package com.loe.http;

import android.content.Context;
import android.os.Environment;

import com.loe.http.callback.HttpFileCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

class HttpFileUtil
{

    public static final String ASSETS = "file:///android_asset/";

    public static Context context;

    public static String basePath = "/mnt/sdcard/";

    public static void init(Context context)
    {
        String[] ps = context.getPackageName().split("\\.");
        init(context, ps[ps.length - 1]);
    }

    public static void init(Context context, String appName)
    {
        HttpFileUtil.context = context;
        basePath = getFilePath(appName);
    }

    /**
     * 获取文件系统路径
     */
    public static String getFilePath(String appName)
    {
        // 判断SDCard是否存在
        boolean sdExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        if (sdExist)
        {
            // 获取SDCard的路径
            File sdFile = Environment.getExternalStorageDirectory();
            return sdFile.getPath() + "/" + appName + "/";
        }
        // 否则返回app路径
        return context.getFilesDir().getPath() + "/";
    }

    /**
     * 保存文件
     */
    public static void assetsToFile(String path, String nPath, final HttpFileCallback fileCallback)
    {
        final String oldPath = path.replace(ASSETS, "");
        final String newPath = nPath + oldPath;

        new Thread()
        {
            @Override
            public void run()
            {
                File file = null;
                FileOutputStream fos = null;
                try
                {
                    InputStream is = context.getAssets().open(oldPath);

                    file = new File(newPath);
                    // 如果文件存在则删除
                    if (file.exists())
                    {
                        delete(file);
                    }
                    // 如果文件夹路径不存在，则创建路径
                    if (!file.getParentFile().exists())
                    {
                        file.getParentFile().mkdirs();
                    }
                    fos = new FileOutputStream(file, true);
                    byte[] buffer = new byte[2048];
                    int byteCount = 0;
                    while ((byteCount = is.read(buffer)) != -1)
                    {
                        fos.write(buffer, 0, byteCount);
                    }
                    fos.flush();
                    is.close();
                    fos.close();
                    fileCallback.response(file);
                } catch (Exception e)
                {
                    if (fos != null)
                    {
                        try
                        {
                            fos.close();
                        } catch (Exception e0)
                        {
                        }
                    }
                    fileCallback.error();
                }
            }
        }.start();
    }

    /**
     * 获取扩展名
     */
    public static String getExtension(String path)
    {
        int start = path.lastIndexOf(".");
        int end = path.indexOf("?");
        if (start < 0)
        {
            return "";
        }
        if (end > start)
        {
            return path.substring(start + 1, end).toLowerCase();
        }
        return path.substring(start + 1).toLowerCase();
    }

    /**
     * 获取url文件名和扩展名
     */
    public static String getUrlNameExt(String url)
    {
        int start = url.lastIndexOf("/");
        int end = url.indexOf("?");
        if (start < 0)
        {
            return url;
        }
        if (end > start)
        {
            return url.substring(start + 1, end);
        }
        return url.substring(start + 1);
    }

    /**
     * 删除文件（文件夹）
     */
    public static boolean delete(File file)
    {
        if (file.isFile())
        {
            return file.delete();
        }
        else
        {
            if (file.isDirectory())
            {
                File[] childFiles = file.listFiles();
                if (childFiles == null || childFiles.length == 0)
                {
                    return file.delete();
                }

                for (int i = 0; i < childFiles.length; i++)
                {
                    delete(childFiles[i]);
                }
                return file.delete();
            }
        }
        return false;
    }

    /**
     * 重命名
     */
    public static File renameAll(File file, String newPath)
    {
        File newFile = new File(newPath);
        file.renameTo(newFile);
        return newFile;
    }
}
