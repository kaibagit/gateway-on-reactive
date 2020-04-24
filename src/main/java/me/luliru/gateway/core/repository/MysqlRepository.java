package me.luliru.gateway.core.repository;

import com.alibaba.druid.pool.DruidDataSource;

import java.util.concurrent.Executors;

/**
 * MysqlRepository
 * Created by luliru on 2019/7/8.
 */
public class MysqlRepository {

    private DruidDataSource druidDataSource;

    public MysqlRepository(){
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        druidDataSource.setUrl("jdbc:mysql://rm-bp188fb70hknd9gi2o.mysql.rds.aliyuncs.com:3306/openqa_coordinator");
        druidDataSource.setUsername("devuser");
        druidDataSource.setPassword("Devuser123");
        druidDataSource.setInitialSize(50);
        druidDataSource.setMinIdle(50);
        druidDataSource.setMaxActive(100);
        druidDataSource.setMaxWait(100);
        druidDataSource.setValidationQuery("SELECT 'x'");
        druidDataSource.setCreateScheduler(Executors.newScheduledThreadPool(5));
        druidDataSource.setMaxCreateTaskCount(5);
        druidDataSource.setKeepAlive(true);
        this.druidDataSource = druidDataSource;
    }

    public DruidDataSource getDruidDataSource() {
        return druidDataSource;
    }
}
