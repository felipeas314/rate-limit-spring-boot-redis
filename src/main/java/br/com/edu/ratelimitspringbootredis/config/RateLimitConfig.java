package br.com.edu.ratelimitspringbootredis.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.function.Supplier;

@Configuration
public class RateLimitConfig {

    @Autowired
    public ProxyManager buckets;

    @Bean
    public Bucket resolveBucket() {
        Supplier<BucketConfiguration> configSupplier = getConfig();
        return buckets.builder().build("rate_limit", configSupplier);
    }

    private Supplier<BucketConfiguration> getConfig() {
        return () -> (BucketConfiguration.builder()
                .addLimit(l -> l.capacity(5).refillIntervally(5,Duration.ofMinutes(1)))
                .build());
    }
}
