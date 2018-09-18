package com.yoyo.conferenceservice.configuration.cassandra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.config.java.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.convert.CustomConversions;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

@Configuration
@EnableCassandraRepositories(basePackages = {"com.kanz.conferenceservice.repository.cassandra"})
public class CassandraConfig extends AbstractCassandraConfiguration {

    @Value("${cassandra.contact-points}")
    private String contactPoints;
    @Value("${cassandra.port}")
    private int port;
    @Value("${cassandra.keyspace-name}")
    private String keySpaceName;
    @Value("${cassandra.base-packages}")
    private String basePackages;

    @Bean(name="cassandraCustomConversions")
    @Override
    public CustomConversions customConversions() {
        return super.customConversions();
    }

    @Override
    public SchemaAction getSchemaAction() {
        return SchemaAction.CREATE_IF_NOT_EXISTS;
    }

    @Override
    protected String getContactPoints() {
        return contactPoints;
    }

    @Override
    protected int getPort() {
        return port;
    }

    @Override
    public String[] getEntityBasePackages() {
        return new String[]{basePackages};
    }

    @Override
    protected String getKeyspaceName() {
        return keySpaceName;
    }
}
