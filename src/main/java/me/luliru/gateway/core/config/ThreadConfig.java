package me.luliru.gateway.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * ThreadConfig
 * Created by luliru on 2019/7/9.
 */
@Configuration
public class ThreadConfig {

    @Bean
    public Scheduler blockScheduler(){
        return Schedulers.elastic();
//        Scheduler blockScheduler = Schedulers.newParallel("gateway-block",2000);
//        return blockScheduler;
    }
}
