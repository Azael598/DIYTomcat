package com.john.diytomcat;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import cn.hutool.system.SystemUtil;
import com.john.diytomcat.catalina.*;
import com.john.diytomcat.classloader.CommonClassLoader;
import com.john.diytomcat.http.Request;
import com.john.diytomcat.http.Response;
import com.john.diytomcat.util.Constant;
import com.john.diytomcat.util.ServerXMLUtil;
import com.john.diytomcat.util.ThreadPoolUtil;
import org.jsoup.internal.StringUtil;
import sun.swing.StringUIClientPropertyKey;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Bootstrap {
    public static void main(String[] args) throws Exception{
        CommonClassLoader commonClassLoader = new CommonClassLoader();
        Thread.currentThread().setContextClassLoader(commonClassLoader);
        String serverClassName = "com.john.diytomcat.catalina.Server";
        Class<?> serverClazz = commonClassLoader.loadClass(serverClassName);
        Object serverObject = serverClazz.newInstance();
        Method m = serverClazz.getMethod("start");
        m.invoke(serverObject);
    }
}
