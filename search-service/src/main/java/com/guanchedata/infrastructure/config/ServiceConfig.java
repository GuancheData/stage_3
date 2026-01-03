package com.guanchedata.infrastructure.config;

public class ServiceConfig {
    
    private final String clusterName;
    private final int servicePort;

    public ServiceConfig() {
        this.clusterName = System.getenv().getOrDefault("CLUSTER_NAME", "SearchEngine");
        this.servicePort = Integer.parseInt(System.getenv().getOrDefault("SERVICE_PORT", "7003"));
    }

    public String getClusterName() {
        return clusterName;
    }

    public int getServicePort() {
        return servicePort;
    }
}
