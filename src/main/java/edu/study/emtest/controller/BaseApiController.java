package edu.study.emtest.controller;

import edu.study.emtest.exception.ThresholdReachedException;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@NoArgsConstructor
public abstract class BaseApiController {

    @ExceptionHandler(ThresholdReachedException.class)
    public ResponseEntity<Void> handleException(HttpServletRequest request) {
        log.error("IP {} reached its limit", request.getRemoteHost());
        return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
    }
}
