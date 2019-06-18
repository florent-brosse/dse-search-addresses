package com.datastax.address.demo.conf;

import com.datastax.dse.driver.api.core.DseSession;
import com.datastax.dse.driver.api.core.DseSessionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.util.List;

@Configuration
public class DseConf {

    @Value("#{'${driver.contactPoints}'.split(',')}")
    protected List<String> contactPoints;

    @Value("${driver.port:9042}")
    protected int port;

    @Value("${driver.localdc}")
    protected String localDc;

    @Value("${driver.keyspace}")
    protected String keyspaceName;


    @Bean
    public DseSessionBuilder sessionBuilder() {
        DseSessionBuilder sessionBuilder = new DseSessionBuilder();
        for (String contactPoint : contactPoints) {
            InetSocketAddress address = InetSocketAddress.createUnresolved(contactPoint, port);
            sessionBuilder = sessionBuilder.addContactPoint(address);
        }
        return sessionBuilder.withLocalDatacenter(localDc);
    }

    @Bean
    public DseSession session(
            DseSessionBuilder sessionBuilder) {
        return sessionBuilder.withKeyspace(keyspaceName).build();
    }
}
