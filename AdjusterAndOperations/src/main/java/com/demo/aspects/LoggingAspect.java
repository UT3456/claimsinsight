package com.demo.aspects;

import java.util.Arrays;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Aspect
public class LoggingAspect {
    @Pointcut("execution(* com.demo.controller.AdjusterPerformanceController.*(..)) || " +
            "execution(* com.demo.controller.SLAViolationController.*(..))")
    public void controllerMethods() {}

    @Before("controllerMethods()")
    public void beforeMethod(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        log.info(">>> START: {}() | Args: {}", methodName, Arrays.toString(args));
    }

    @After("controllerMethods()")
    public void afterMethod(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        log.info("Successful execution flow reached for: {}", methodName);
    }

    @AfterReturning(pointcut = "controllerMethods()", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        log.info("<<< END: {}() | Result: {}", methodName, result);
    }

    @AfterThrowing(pointcut = "controllerMethods()", throwing = "error")
    public void afterThrowing(JoinPoint joinPoint, Throwable error) {
        String methodName = joinPoint.getSignature().getName();
        log.error("!!! EXCEPTION in {}: {} - Message: {}",
                methodName,
                error.getClass().getSimpleName(),
                error.getMessage());
    }
}