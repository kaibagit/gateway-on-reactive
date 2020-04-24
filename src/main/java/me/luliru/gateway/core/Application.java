package me.luliru.gateway.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;

/**
 * Application
 * Created by luliru on 2019/7/8.
 */
@Slf4j
@ImportResource(locations = "classpath:/spring/application-*.xml")
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, SolrAutoConfiguration.class})
@ComponentScan(basePackages = {"me.luliru"})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        log.info("Gateway started.");
    }
}
