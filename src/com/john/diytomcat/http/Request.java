package com.john.diytomcat.http;

import com.john.diytomcat.Bootstrap;
import com.john.diytomcat.catalina.Context;
import com.john.diytomcat.util.MiniBrowser;
import cn.hutool.core.util.StrUtil;


import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class Request {

    private String requestString;
    private String uri;
    private Socket socket;
    private Context context;

    public Request(Socket socket) throws IOException {
        this.socket = socket;
        parseHttpRequest();
        if(StrUtil.isEmpty(requestString))
            return;
        parseUri();
        parseContext();
        if (!"/".equals(context.getPath())){
            uri=StrUtil.removePrefix(uri,context.getPath());
        }
    }

    private void parseContext(){
        String path = StrUtil.subBetween(uri,"/","/");
        if (null==path){
            path="/";
        }else {
            path="/"+path;
        }
        context = Bootstrap.contextMap.get(path);
        if (null==context){
            context=Bootstrap.contextMap.get("/");
        }
    }

    //trans bytes array from client to requestString
    private void parseHttpRequest() throws IOException {
        InputStream is = this.socket.getInputStream();
        byte[] bytes = MiniBrowser.readBytes(is);
        requestString = new String(bytes, "utf-8");
    }

    //get uri form httpRequest
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

    public String getRequestString(){
        return requestString;
    }

    public Context getContext() {
        return context;
    }

}