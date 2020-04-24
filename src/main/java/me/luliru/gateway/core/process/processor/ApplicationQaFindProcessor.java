package me.luliru.gateway.core.process.processor;

import lombok.extern.slf4j.Slf4j;
import me.luliru.gateway.core.RequestContext;
import me.luliru.gateway.core.enums.CodeEnum;
import me.luliru.gateway.core.exception.OpenGatewayException;
import me.luliru.gateway.core.process.Processor;
import me.luliru.gateway.core.process.dto.RequestParam;
import me.luliru.gateway.core.repository.MysqlRepository;
import me.luliru.gateway.core.repository.entity.ApplicationQa;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ApplicationQaFindProcessor
 * Created by luliru on 2019/7/29.
 */
@Slf4j
public class ApplicationQaFindProcessor implements Processor<RequestParam,ApplicationQa> {

    private Scheduler blockScheduler;

    private MysqlRepository mysqlRepository = new MysqlRepository();

    public ApplicationQaFindProcessor(Scheduler blockScheduler){
        this.blockScheduler = blockScheduler;
    }

    @Override
    public Mono<ApplicationQa> process(RequestContext ctx, Mono<RequestParam> mono) {
        return mono.publishOn(blockScheduler)
                .flatMap(param -> {
                    log.debug("db operating:{}",mono);
                    Connection conn = null;
                    try{
                        conn = mysqlRepository.getDruidDataSource().getConnection();
                        PreparedStatement pstmt = conn.prepareStatement("select * from application_qa where app_key = ?");
                        pstmt.setString(1,param.getAppkey());
                        ResultSet rs = pstmt.executeQuery();
                        if(rs.next()) {
                            ApplicationQa applicationQa = new ApplicationQa();
                            applicationQa.setId(rs.getLong("id"));
                            applicationQa.setDevepId(rs.getLong("devep_id"));
                            applicationQa.setAppName(rs.getString("app_name"));
                            applicationQa.setAppKey(rs.getString("app_key"));
                            applicationQa.setAppSecret(rs.getString("app_secret"));
                            applicationQa.setCallbackUrl(rs.getString("callback_url"));
                            applicationQa.setCallbackFormat((byte) rs.getInt("callback_format"));
//                            applicationQa.setInsTm(new Date(record.getLong("ins_tm")));
//                            applicationQa.setUpdTm(new Date(record.getLong("upd_tm")));
                            log.debug("ApplicationQa:{}",applicationQa);
                            return Mono.just(applicationQa);
                        }else {
                            return Mono.error(new OpenGatewayException(CodeEnum.SYS_INVALID_PARAMETER,"无效的appkey"));
                        }
                    }catch (Exception e){
                        return Mono.error(e);
                    }finally {
                        if(conn != null){
                            try {
                                conn.close();
                            } catch (SQLException e) {
                                log.error("Close Connection fail.",e);
                            }
                        }
                    }
                }).publishOn(ctx.scheduler());
    }
}
