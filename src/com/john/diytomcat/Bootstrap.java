package com.john.diytomcat;

import cn.hutool.core.util.NetUtil;

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
                InputStream is = s.getInputStream();
                int bufferSize=1024;
                byte[] buffer = new byte[bufferSize];
                is.read(buffer);
                String requestString = new String(buffer, "utf-8");
                System.out.println("input from the browser"+requestString);
                OutputStream os = s.getOutputStream();
                String response_head = "HTTP/1.1 200 OK\r\n"+"Content-Type: text/html\r\n\r\n";
                String responseString="Hello Tomcat";
                responseString=response_head+responseString;
                os.write(responseString.getBytes());
                os.flush();
                s.close();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
