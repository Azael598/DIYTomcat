package com.john.diytomcat.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import com.john.diytomcat.classloader.WebappClassLoader;
import com.john.diytomcat.exception.WebConfigDuplicatedException;
import com.john.diytomcat.http.ApplicationContext;
import com.john.diytomcat.http.StandardServletConfig;
import com.john.diytomcat.util.Constant;
import com.john.diytomcat.util.ContextXMLUtil;
import com.john.diytomcat.watcher.ContextFileChangeWatcher;
import org.apache.tools.ant.taskdefs.condition.Http;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.util.*;

public class Context {
    private String path;
    private String docBase;
    private File contextWebXmlFile;
    private WebappClassLoader webappClassLoader;
    private Host host;
    private boolean reloadable;
    private ServletContext servletContext;
    private Map<Class<?>, HttpServlet> servletPool;
    private List<String> loadOnStartupServletClassNames;

    private Map<String, String> url_servletClassName;
    private Map<String, String> url_servletName;
    private Map<String, String> servletName_className;
    private Map<String, String> className_servletName;
    private Map<String, Map<String,String>> servlet_className_init_params;

    private ContextFileChangeWatcher contextFileChangeWatcher;

    public Context(String path, String docBase, Host host, boolean reloadable) {

        this.path = path;
        this.docBase = docBase;
        this.contextWebXmlFile = new File(docBase, ContextXMLUtil.getWatchedResource());
        this.url_servletClassName = new HashMap<>();
        this.url_servletName = new HashMap<>();
        this.servletName_className = new HashMap<>();
        this.className_servletName = new HashMap<>();
        ClassLoader commonClassLoader = Thread.currentThread().getContextClassLoader();
        this.webappClassLoader = new WebappClassLoader(docBase, commonClassLoader);
        this.host = host;
        this.reloadable = reloadable;
        this.servletContext = new ApplicationContext(this);
        this.servletPool = new HashMap<>();
        this.servlet_className_init_params = new HashMap<>();
        this.loadOnStartupServletClassNames = new ArrayList<>();
        deploy();


    }

    public void stop() {
        webappClassLoader.stop();
        contextFileChangeWatcher.stop();
        destroyServlets();
    }

    public void reload() {
        host.reload(this);
    }

    private void deploy() {
        TimeInterval timeInterval = DateUtil.timer();
        LogFactory.get().info("Deploying web application directory {}", this.docBase);
        init();
        if (reloadable) {
            contextFileChangeWatcher = new ContextFileChangeWatcher(this);
            contextFileChangeWatcher.start();
        }
        LogFactory.get().info("Deployment of web application directory {} has finished in {} ms", this.docBase, timeInterval.intervalMs());
    }

    private void init() {
        if (!contextWebXmlFile.exists())
            return;
        try {
            checkDuplicated();
        } catch (WebConfigDuplicatedException e) {
            e.printStackTrace();
            return;
        }
        String xml = FileUtil.readUtf8String(contextWebXmlFile);
        Document d = Jsoup.parse(xml);
        parseServletMapping(d);
        parseServletInitParams(d);
        parseLoadOnStartup(d);
        handleLoadOnStartup();
    }

    private void parseServletMapping(Document d) {
        Elements mappingurlElements = d.select("servlet-mapping url-pattern");
        for (Element mappingurlElement : mappingurlElements) {
            String urlPattern = mappingurlElement.text();
            String servletName = mappingurlElement.parent().select("servlet-name").first().text();
            url_servletName.put(urlPattern, servletName);
        }
        Elements servletNameElements = d.select("servlet servlet-name");
        for (Element servletNameElement : servletNameElements) {
            String servletName = servletNameElement.text();
            String servletClass = servletNameElement.parent().select("servlet-class").first().text();
            servletName_className.put(servletName, servletClass);
            className_servletName.put(servletClass, servletName);
        }
        Set<String> urls = url_servletName.keySet();
        for (String url : urls) {
            String servletName = url_servletName.get(url);
            String servletClassName = servletName_className.get(servletName);
            url_servletClassName.put(url, servletClassName);
        }
    }

    private void parseServletInitParams(Document d){
        Elements servletClassNameElements = d.select("servlet-class");
        for(Element servletClassNameElement :servletClassNameElements){
            String servletClassName = servletClassNameElement.text();
            Elements initElements = servletClassNameElement.parent().select("init-param");
            if(initElements.isEmpty())
                continue;
            Map<String,String> initParams = new HashMap<>();
            for(Element element : initElements){
                String name = element.select("param-name").get(0).text();
                String value = element.select("param-value").get(0).text();
                initParams.put(name, value);
            }
            servlet_className_init_params.put(servletClassName,initParams);
        }
//        System.out.println("class_name_init_params:"+servlet_className_init_params);
    }

    private void checkDuplicated(Document d, String mapping, String desc) throws WebConfigDuplicatedException {
        Elements elements = d.select(mapping);
        List<String> contents = new ArrayList<>();
        for (Element e : elements) {
            contents.add(e.text());
        }
        Collections.sort(contents);
        for (int i = 0; i < contents.size() - 1; i++) {
            String contentPre = contents.get(i);
            String contentNext = contents.get(i + 1);
            if (contentPre.equals(contentNext)) {
                throw new WebConfigDuplicatedException(StrUtil.format(desc, contentPre));
            }
        }
    }

    private void checkDuplicated() throws WebConfigDuplicatedException {
        String xml = FileUtil.readUtf8String(contextWebXmlFile);
        Document d = Jsoup.parse(xml);
        checkDuplicated(d, "servlet-mapping url-pattern", "servlet url 重复，请保持其唯一性:{}");
        checkDuplicated(d, "servlet servlet-name", "servlet 名称重复，请保持其唯一性:{}");
        checkDuplicated(d, "servlet servlet-class", "servlet 类名重复，请保持其唯一性:{}");
    }

    public String getServletClassName(String uri) {
        return url_servletClassName.get(uri);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDocBase() {
        return docBase;
    }

    public void setDocBase(String docBase) {
        this.docBase = docBase;
    }

    public boolean isReloadable() {
        return reloadable;
    }

    public void setReloadable(boolean reloadable) {
        this.reloadable = reloadable;
    }

    public WebappClassLoader getWebappClassLoader() {
        return webappClassLoader;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public synchronized HttpServlet getServlet(Class<?> clazz) throws InstantiationException, IllegalAccessException, ServletException {
        HttpServlet servlet = servletPool.get(clazz);
        if (null == servlet) {
            servlet = (HttpServlet) clazz.newInstance();
            ServletContext servletContext = this.getServletContext();
            String className = clazz.getName();
            String servletName = className_servletName.get(className);
            Map<String, String> initParameters = servlet_className_init_params.get(className);
            StandardServletConfig servletConfig = new StandardServletConfig(servletContext, servletName, initParameters);
            servlet.init(servletConfig);
            servletPool.put(clazz, servlet);
        }
        return servlet;
    }

    private void destroyServlets(){
        Collection<HttpServlet>servlets=servletPool.values();
        for(HttpServlet servlet : servlets){
            servlet.destroy();
        }
    }

    private void parseLoadOnStartup(Document d){
        Elements es = d.select("load-on-startup");
        for(Element e:es){
            String loadOnStartupServletClassName = e.parent().select("servlet-class").text();
            loadOnStartupServletClassNames.add(loadOnStartupServletClassName);
        }
    }

    public void handleLoadOnStartup(){
        for(String loadOnStartupServletClassName : loadOnStartupServletClassNames){
            try{
                Class<?> clazz = webappClassLoader.loadClass(loadOnStartupServletClassName);
                getServlet(clazz);
            }catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ServletException e){
                e.printStackTrace();
            }
        }
    }
}
