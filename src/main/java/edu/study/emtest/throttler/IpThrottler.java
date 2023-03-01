package edu.study.emtest.throttler;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
@Component
@Slf4j
public class IpThrottler {
    // The maximum number of sessions that can be executed
    // concurrently per IP
    private int maxSessionsPerIp;
    // Time int sec, that session is supposed to live
    private int maxSessionLifetime;

    private LoadingCache<String, IpRateLimiter> sessionsPerIpCache;

    private ExecutorService executor;

    public IpThrottler(@Value("${ip.rateLimiter.session.max:5}") int maxSessionsPerIp,
                       @Value("${ip.rateLimiter.session.lifetime:5}") int maxSessionLifetime) {
        this.maxSessionsPerIp = maxSessionsPerIp;
        this.maxSessionLifetime = maxSessionLifetime;
        this.sessionsPerIpCache = Caffeine.newBuilder()
                .expireAfterWrite(maxSessionLifetime, TimeUnit.SECONDS)
                .refreshAfterWrite(maxSessionLifetime, TimeUnit.SECONDS)
                .build(key -> null);
        executor = new ThreadPoolExecutor(
                maxSessionsPerIp * 20, maxSessionsPerIp * 20, 0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(100));
    }

    @SuppressWarnings("ConstantConditions")
    public boolean getSessionByIp(@NonNull String clientIpAddress) {
        sessionsPerIpCache.asMap().putIfAbsent(clientIpAddress, new IpRateLimiter());
        return sessionsPerIpCache.asMap().computeIfPresent(clientIpAddress, (k, rateLimiter) -> {
                    rateLimiter.getAccessByIp(clientIpAddress);
                    return rateLimiter;
                })
                .getCurrentSessionsNumber()
                .get() < maxSessionsPerIp;
    }

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    public class IpRateLimiter {
        //        Active sessions number
        private final AtomicInteger currentSessionsNumber = new AtomicInteger(0);

        public int getAccessByIp(String ip) {

            if (currentSessionsNumber.get() >= maxSessionsPerIp) {
                return currentSessionsNumber.get();
            } else {
                currentSessionsNumber.incrementAndGet();
            }
            executor.submit(() -> sessionTask(ip));
            return currentSessionsNumber.get();
        }

        // ip is conveyed for logging only
        private void sessionTask(String ip) {
            try {
                log.info("Ip address {} active sessions {}", ip, currentSessionsNumber);
                // Do some work here
                TimeUnit.SECONDS.sleep(maxSessionLifetime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                currentSessionsNumber.decrementAndGet();
            }
        }
    }
}
