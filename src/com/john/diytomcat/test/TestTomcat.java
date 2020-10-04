package com.john.diytomcat.test;

import cn.hutool.core.util.NetUtil;
import cn.hutool.core.util.StrUtil;
import com.john.diytomcat.util.MiniBrowser;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestTomcat {
    private static int port=18080;
    private static String ip ="127.0.0.1";
    @BeforeClass
    public static void beforeClass(){
        if (NetUtil.isUsableLocalPort(port)){
            System.err.println("请先启动位于端口："+port+"的DIYTomcat");
            System.exit(1);
        }else {
            System.out.println("DIYTomcat 已启动，开始单元测试");
        }
    }

    @Test
    public void testHelloTomcat(){
        String html = getContentString("/");
        Assert.assertEquals(html,"Hello DIY Tomcat");
    }
    private String getContentString(String uri){
        String url= StrUtil.format("http://{}:{}{}",ip,port,uri);
        String content = MiniBrowser.getContentString(url);
        return  content;
    }

}
