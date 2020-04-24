package me.luliru.gateway.core.mock.v2;

import lombok.extern.slf4j.Slf4j;
import me.luliru.gateway.core.RequestContext;
import me.luliru.gateway.core.process.Processor;
import me.luliru.gateway.core.process.dto.RequestParam;
import me.luliru.gateway.core.repository.MysqlRepository;
import me.luliru.gateway.core.repository.entity.ApplicationQa;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/**
 * MockApplicationQaFindProcessor
 * Created by luliru on 2019/8/5.
 */
@Slf4j
public class MockApplicationQaFindProcessor implements Processor<RequestParam,ApplicationQa> {

    private Scheduler blockScheduler;

    private MysqlRepository mysqlRepository = new MysqlRepository();

    public MockApplicationQaFindProcessor(Scheduler blockScheduler){
        this.blockScheduler = blockScheduler;
    }

    @Override
    public Mono<ApplicationQa> process(RequestContext ctx, Mono<RequestParam> mono) {
        return mono.publishOn(blockScheduler)
                .flatMap(param -> {
                    log.debug("db operating:{}",mono);
                    ApplicationQa applicationQa = new ApplicationQa();
                    applicationQa.setId(1L);
                    applicationQa.setDevepId(1L);
                    applicationQa.setAppName("app_name");
                    applicationQa.setAppKey("app_key");
                    applicationQa.setAppSecret("app_secret");
                    applicationQa.setCallbackUrl("callback_url");
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return Mono.just(applicationQa);
                }).publishOn(ctx.scheduler());
    }
}
