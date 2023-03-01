package edu.study.emtest.controller;

import edu.study.emtest.exception.ThresholdReachedException;
import edu.study.emtest.throttler.IpThrottler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@Slf4j
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommonController extends BaseApiController {

    private final IpThrottler ipThrottler;

    @GetMapping("/test")
    public void testAccess(HttpServletRequest request) {
        if (!ipThrottler.getSessionByIp(request.getRemoteAddr())) {
            throw new ThresholdReachedException();
        }
        log.info("Client ip is {}", request.getRemoteHost());
    }


}
