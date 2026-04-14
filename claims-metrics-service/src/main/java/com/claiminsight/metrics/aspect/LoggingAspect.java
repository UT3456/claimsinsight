package com.claiminsight.metrics.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

/** AOP aspect that logs all controller method calls, return values, exceptions, and execution time. */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    /** Pointcut targeting all methods in the controller package. */
    @Pointcut("execution(* com.claiminsight.metrics.controller.*.*(..))")
    public void controllerMethods() {}

    /** Logs the method name before execution. */
    @Before("controllerMethods()")
    public void logBefore(JoinPoint jp) {
        log.info("[BEFORE] {}.{}()", jp.getTarget().getClass().getSimpleName(), jp.getSignature().getName());
    }

    /** Logs the method name after execution completes. */
    @After("controllerMethods()")
    public void logAfter(JoinPoint jp) {
         log.info("[AFTER] {}.{}()", jp.getTarget().getClass().getSimpleName(), jp.getSignature().getName());
    }

    /** Logs the return value on successful execution. */
    @AfterReturning(pointcut = "controllerMethods()", returning = "result")
    public void logAfterReturning(JoinPoint jp, Object result) {
        log.info("[RETURNING] {}.{}() => {}", jp.getTarget().getClass().getSimpleName(), jp.getSignature().getName(), result);
    }

    /** Logs the exception message when a method throws. */
    @AfterThrowing(pointcut = "controllerMethods()", throwing = "ex")
    public void logAfterThrowing(JoinPoint jp, Throwable ex) {
        log.error("[THROWING] {}.{}() threw: {}", jp.getTarget().getClass().getSimpleName(), jp.getSignature().getName(), ex.getMessage());
    }

    /** Measures and logs total execution time in milliseconds. */
    @Around("controllerMethods()")
    public Object logAround(ProceedingJoinPoint jp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Object result = jp.proceed();
            log.info("[AROUND] {}.{}() in {} ms", jp.getTarget().getClass().getSimpleName(), jp.getSignature().getName(), System.currentTimeMillis() - start);
            return result;
        } catch (Throwable ex) {
            log.error("[AROUND] {}.{}() failed in {} ms", jp.getTarget().getClass().getSimpleName(), jp.getSignature().getName(), System.currentTimeMillis() - start);
            throw ex;
        }
    }
}
