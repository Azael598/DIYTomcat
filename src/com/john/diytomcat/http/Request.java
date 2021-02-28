package com.john.diytomcat.http;

import cn.hutool.core.util.StrUtil;
import com.john.diytomcat.catalina.Context;
import com.john.diytomcat.catalina.Engine;
import com.john.diytomcat.catalina.Service;
import com.john.diytomcat.util.MiniBrowser;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class Request extends BaseRequest{

    private String requestString;
    private String uri;
    private Socket socket;
    private Context context;
    private Service service;
    private String method;

    public Request(Socket socket, Service service) throws IOException {
        this.socket = socket;
        this.service = service;
        parseHttpRequest();
        if (StrUtil.isEmpty(requestString))
            return;
        parseUri();
        parseContext();
        parseMethod();
        if (!"/".equals(context.getPath())) {
            uri = StrUtil.removePrefix(uri, context.getPath());
            if (StrUtil.isEmpty(uri))
                uri = "/";
        }
    }

    private void parseMethod(){
        method = StrUtil.subBefore(requestString, " ", false);
    }

    private void parseContext() {
        Engine engine = service.getEngine();
        context = engine.getDefaultHost().getContext(uri);
        if (null != context) return;
        String path = StrUtil.subBetween(uri, "/", "/");
        if (null == path) {
            path = "/";
        } else {
            path = "/" + path;
        }
        context = engine.getDefaultHost().getContext(path);
        if (null == context) {
            context = engine.getDefaultHost().getContext("/");
        }
    }

    //trans bytes array from client to requestString
    private void parseHttpRequest() throws IOException {
        InputStream is = this.socket.getInputStream();
        byte[] bytes = MiniBrowser.readBytes(is, false);
        requestString = new String(bytes, "utf-8");
    }

    //get uri from httpRequest
    private void parseUri() {
        String temp;

        temp = StrUtil.subBetween(requestString, " ", " ");
        if (!StrUtil.contains(temp, '?')) {
            uri = temp;
            return;
        }
        temp = StrUtil.subBefore(temp, '?', false);
        uri = temp;
    }

    public String getUri() {
        return uri;
    }

    public String getRequestString() {
        return requestString;
    }

    public Context getContext() {
        return context;
    }

    @Override
    public String getMethod() {
        return method;
    }

    public ServletContext getServletContext(){
        return context.getServletContext();
    }

    public String getRealPath(String path){
        return getServletContext().getRealPath(path);
    }

}