package com.john.diytomcat.catalina;

import com.john.diytomcat.util.ServerXMLUtil;

import java.util.List;

public class Engine {
    private String defaultHost;
    private List<Host> hosts;
    private Service service;

    public Engine(Service service) {
        this.service = service;
        this.defaultHost = ServerXMLUtil.getEngineDefaultHost();
        this.hosts = ServerXMLUtil.getHosts(this);
        checkDefault();
    }

    public Service getService(){
        return service;
    }

    private void checkDefault() {
        if (null == getDefaultHost())
            throw new RuntimeException("the defaultHost" + defaultHost + "does not exist!");
    }

    public Host getDefaultHost() {
        for (Host host : hosts) {
            if (host.getName().equals(defaultHost))
                return host;
        }
        return null;
    }
}
