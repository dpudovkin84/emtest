package edu.study.emtest.throttler;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ThrottleTest {

    @Test
    public void concurrencyTest() throws InterruptedException {
        final int maxSessionsPerIp = 5;
        final int maxSessionLifetime = 5;
        // number of thread producing load
        final int threadNumber = 100;
        // number of ip in test
        final int ipNumber = 10;
        final IpThrottler ipThrottler = new IpThrottler(maxSessionsPerIp, maxSessionLifetime);

        final CompletableFuture<Void>[] runnableList = IntStream.range(1, threadNumber).boxed()
                .map(el -> CompletableFuture.runAsync(() -> requestTask(ipNumber, ipThrottler)))
                .collect(Collectors.toList()).toArray(CompletableFuture[]::new);
        final CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(runnableList);

        try {
            combinedFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            System.out.println(e.getMessage());
        }
        System.out.println("Result before" + ipThrottler.getSessionsPerIpCache().asMap());

        final Integer maxSessions = ipThrottler.getSessionsPerIpCache().asMap().values()
                .stream()
                .map(ipRateLimiter -> ipRateLimiter.getCurrentSessionsNumber().get())
                .max(Comparator.comparingInt(num -> num))
                .get();

        // check max session number after load
        assertEquals(maxSessionsPerIp, maxSessions);
        TimeUnit.SECONDS.sleep(maxSessionLifetime);

        ipThrottler.getSessionsPerIpCache().cleanUp();

        //check that cache is empty after maxSessionLifetime
        assertEquals(0, ipThrottler.getSessionsPerIpCache().asMap().size());

    }

    private void requestTask(int ipNumber, IpThrottler ipThrottler) {
        // getting ip to make request
        final String ip = "ip" + new Random().nextInt(ipNumber);
        ipThrottler.getSessionByIp(ip);
    }

}
