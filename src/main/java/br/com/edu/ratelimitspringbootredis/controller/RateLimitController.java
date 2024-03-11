package br.com.edu.ratelimitspringbootredis.controller;

import br.com.edu.ratelimitspringbootredis.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class RateLimitController
{
    @Autowired
    private Bucket bucket;

    @GetMapping("/user/{id}")
    public ResponseEntity<?> getInfo()
    {
        //+Bucket bucket = rateLimitConfig.resolveBucket("id");
        if(bucket.tryConsume(1))
        {
            return ResponseEntity.status(200).body("Success for user ");
        }else {
            return ResponseEntity.status(429).body("Rate limit exceeded for user ");
        }

    }

}
