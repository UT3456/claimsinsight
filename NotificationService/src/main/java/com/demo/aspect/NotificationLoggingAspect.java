package com.demo.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * AOP Logging Aspect Notifications
 */
@Slf4j
@Aspect
@Component
public class NotificationLoggingAspect {

    private static final String CONTROLLER_POINTCUT =
            "execution(* com.demo.controller.NotificationControllerImpl.*(..))";
   //controller

    @Before(CONTROLLER_POINTCUT)
    public void logBeforeController(JoinPoint joinPoint) {
        log.info("[CONTROLLER] >> Entering: {}() | Args: {}",
                joinPoint.getSignature().getName(),
                Arrays.toString(joinPoint.getArgs()));
    }

    @AfterReturning(pointcut = CONTROLLER_POINTCUT, returning = "result")
    public void logAfterReturningController(JoinPoint joinPoint, Object result) {
        log.info("[CONTROLLER] << Exiting:  {}() | Returned: {}",
                joinPoint.getSignature().getName(), result);
    }

    @AfterThrowing(pointcut = CONTROLLER_POINTCUT, throwing = "exception")
    public void logAfterThrowingController(JoinPoint joinPoint, Throwable exception) {
        log.error("[CONTROLLER] !! Exception in {}() | Type: {} | Message: {}",
                joinPoint.getSignature().getName(),
                exception.getClass().getSimpleName(),
                exception.getMessage());
    }
}
