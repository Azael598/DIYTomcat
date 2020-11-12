package com.john.diytomcat;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import cn.hutool.system.SystemUtil;
import com.john.diytomcat.catalina.Context;
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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Bootstrap {
    public static Map<String, Context> contextMap = new HashMap<>();

    public static void main(String[] args) {
        try {
            logJVM();
            scanContextsOnWebAppsFolder();
            scanContextsInServerXML();
            int port = 18080;
//            if (!NetUtil.isUsableLocalPort(port)){
//                System.out.println("port has already been used");
//                return;
//            }
            ServerSocket ss = new ServerSocket(port);
            while (true) {
                Socket s = ss.accept();
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Request request = new Request(s);
                            // System.out.println("浏览器的输入信息： \r\n" + request.getRequestString());
                            // System.out.println("uri:" + request.getUri());

                            //Refactor the response module
                            Response response = new Response();
                            String uri = request.getUri();
                            if (null == uri) {
                                return;
                            }
                            System.out.println(uri);
                            Context context = request.getContext();
                            if ("/".equals(uri)) {
                                String html = "Hello DIY Tomcat";
                                response.getWriter().println(html);
                            } else {
                                String fileName = StrUtil.removePrefix(uri, "/");
                                File file = FileUtil.file(context.getDocBase(), fileName);
                                if (file.exists()) {
                                    String fileContent = FileUtil.readUtf8String(file);
                                    response.getWriter().println(fileContent);
                                    if (fileName.equals("timeConsume.html")) {
                                        ThreadUtil.sleep(1000);
                                    }
                                } else {
                                    response.getWriter().println("File Not Found");
                                }
                            }
                            handle200(s, response);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                };
                ThreadPoolUtil.run(r);
            }
        } catch (IOException e) {
            LogFactory.get().error(e);
            e.printStackTrace();
        }
    }

    private static void scanContextsInServerXML() {
        List<Context> contexts = ServerXMLUtil.getContexts();
        for (Context context : contexts) {
            contextMap.put(context.getPath(), context);
        }
    }

    private static void scanContextsOnWebAppsFolder() {
        File[] folders = Constant.webappsFolder.listFiles();
        for (File folder : folders) {
            if (!folder.isDirectory()) {
                continue;
            }
            loadContext(folder);
        }
    }

    private static void loadContext(File folder) {
        String path = folder.getName();
        if ("ROOT".equals(path)) {
            path = "/";
        } else {
            path = "/" + path;
        }
        String docBase = folder.getAbsolutePath();
        Context context = new Context(path, docBase);
        contextMap.put(context.getPath(), context);
    }

    private static void logJVM() {
        Map<String, String> infos = new LinkedHashMap<>();
        infos.put("Server version", "DIYTomcat/1.0.0");
        infos.put("Server built", "2020-11-1 14:11:00");
        infos.put("Server number", "1.0.0");
        infos.put("OS Name\t", SystemUtil.get("os.name"));
        infos.put("OS Version", SystemUtil.get("os.version"));
        infos.put("Architecture", SystemUtil.get("os.arch"));
        infos.put("Java Home", SystemUtil.get("java.home"));
        infos.put("JVM Version", SystemUtil.get("java.runtime.version"));
        infos.put("JVM Vendor", SystemUtil.get("java.vm.specification.vendor"));

        Set<String> keys = infos.keySet();
        for (String key : keys) {
            LogFactory.get().info(key + ":\t\t" + infos.get(key));
        }
        //lambda
//        infos.forEach((k,v)->{
//            LogFactory.get().info(k+":\t\t"+infos.get(k));
//        });
    }

    private static void handle200(Socket s, Response response) throws IOException {
        String contentType = response.getContentType();
        String headText = Constant.response_head_202;
        headText = StrUtil.format(headText, contentType);
        byte[] head = headText.getBytes();
        byte[] body = response.getBody();
        byte[] responseBytes = new byte[head.length + body.length];
        ArrayUtil.copy(head, 0, responseBytes, 0, head.length);
        ArrayUtil.copy(body, 0, responseBytes, head.length, body.length);

        OutputStream os = s.getOutputStream();
        os.write(responseBytes);
        s.close();
    }
}
