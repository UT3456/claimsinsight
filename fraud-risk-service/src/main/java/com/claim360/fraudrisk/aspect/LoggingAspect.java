package com.claim360.fraudrisk.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;


@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Before("execution(* com.claim360.fraudrisk.controller.*.*(..))")
    public void logBefore(JoinPoint joinPoint) {
        log.info("REQUEST  | Method: {} | Class: {}",
                joinPoint.getSignature().getName(),
                joinPoint.getTarget().getClass().getSimpleName());
    }

    @After("execution(* com.claim360.fraudrisk.controller.*.*(..))")
    public void logAfter(JoinPoint joinPoint) {
        log.info("RESPONSE | Method: {} | Class: {} | Completed Successfully",
                joinPoint.getSignature().getName(),
                joinPoint.getTarget().getClass().getSimpleName());
    }

    @AfterThrowing(
            pointcut = "execution(* com.claim360.fraudrisk.controller.*.*(..))",
            throwing = "exception"
    )
    public void logAfterThrowing(JoinPoint joinPoint, Throwable exception) {
        log.error("EXCEPTION | Method: {} | Class: {} | Exception: {} | Message: {}",
                joinPoint.getSignature().getName(),
                joinPoint.getTarget().getClass().getSimpleName(),
                exception.getClass().getSimpleName(),
                exception.getMessage());
    }
}