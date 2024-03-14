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

    /**
     * @param key
     * In a production env, the resolveBucket function takes in the key param as an authentication
     * token(say). Then the relevant user details can be extracted from that token to fetch the
     * corresponding rate limit details for that particular user from the DB and subsequently
     * process the request according to those details.
     * */
    @Bean
    public Bucket resolveBucket() {
        Supplier<BucketConfiguration> configSupplier = getConfigSupplierForUser();
        return buckets.builder().build("rate_limit", configSupplier);
    }

    private Supplier<BucketConfiguration> getConfigSupplierForUser() {
        return () -> (BucketConfiguration.builder()
                .addLimit(l -> l.capacity(5).refillIntervally(5,Duration.ofMinutes(1)))
                .build());
    }
}
