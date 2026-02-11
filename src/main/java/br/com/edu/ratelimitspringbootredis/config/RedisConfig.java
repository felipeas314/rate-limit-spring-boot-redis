package br.com.edu.ratelimitspringbootredis.config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.redisson.Bucket4jRedisson;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    @Value("${redis.mode:single}")
    private String redisMode;

    @Value("${redis.address:redis://localhost:6379}")
    private String redisAddress;

    @Value("${redis.sentinel.master-name:}")
    private String sentinelMasterName;

    @Value("${redis.sentinel.addresses:}")
    private String sentinelAddresses;

    @Value("${redis.cluster.addresses:}")
    private String clusterAddresses;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        switch (redisMode) {
            case "sentinel" -> config.useSentinelServers()
                    .setMasterName(sentinelMasterName)
                    .addSentinelAddress(sentinelAddresses.split(","));
            case "cluster" -> config.useClusterServers()
                    .addNodeAddress(clusterAddresses.split(","));
            default -> config.useSingleServer()
                    .setAddress(redisAddress);
        }
        return Redisson.create(config);
    }

    @Bean
    public ProxyManager<String> proxyManager(RedissonClient redissonClient) {
        return Bucket4jRedisson
                .casBasedBuilder(((Redisson) redissonClient).getCommandExecutor())
                .build();
    }
}
