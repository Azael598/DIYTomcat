package com.john.diytomcat;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NetUtil;
import cn.hutool.core.util.StrUtil;
import com.john.diytomcat.http.Request;
import com.john.diytomcat.http.Response;
import com.john.diytomcat.util.Constant;
import sun.swing.StringUIClientPropertyKey;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Bootstrap {
    public static void main(String[] args) {
        try{
            int port=18080;
            if (!NetUtil.isUsableLocalPort(port)){
                System.out.println("port has already been used");
                return;
            }
            ServerSocket ss = new ServerSocket(port);
            while (true){
                Socket s = ss.accept();
                Request request = new Request(s);
                System.out.println("浏览器的输入信息： \r\n" + request.getRequestString());
                System.out.println("uri:" + request.getUri());
//                InputStream is = s.getInputStream();
//                int bufferSize=1024;
//                byte[] buffer = new byte[bufferSize];
//                is.read(buffer);
//                String requestString = new String(buffer, "utf-8");
//                System.out.println("input from the browser"+requestString);

                //Refactor the response module
                Response response = new Response();
                String html = "Hello DIY Tomcat";
                response.getWriter().println(html);
                handle200(s,response);

//                OutputStream os = s.getOutputStream();
//                String response_head = "HTTP/1.1 200 OK\r\n"+"Content-Type: text/html\r\n\r\n";
//                String responseString="Hello DIY Tomcat";
//                responseString=response_head+responseString;
//                os.write(responseString.getBytes());
//                os.flush();
//                s.close();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private static void handle200(Socket s,Response response) throws IOException{
        String contentType = response.getContentType();
        String headText= Constant.response_head_202;
        headText= StrUtil.format(headText,contentType);
        byte[] head = headText.getBytes();
        byte[] body = response.getBody();
        byte[] responseBytes = new byte[head.length+body.length];
        ArrayUtil.copy(head,0,responseBytes,0,head.length);
        ArrayUtil.copy(body,0,responseBytes,head.length,body.length);

        OutputStream os = s.getOutputStream();
        os.write(responseBytes);
        s.close();
    }
}
