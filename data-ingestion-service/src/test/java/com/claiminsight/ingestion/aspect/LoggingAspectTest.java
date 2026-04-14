package com.claiminsight.ingestion.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoggingAspectTest {

    private final LoggingAspect loggingAspect = new LoggingAspect();

    @Test
    void simpleAdviceMethods_executeWithoutThrowing() {
        JoinPoint joinPoint = mock(JoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getTarget()).thenReturn(this);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("sampleMethod");

        loggingAspect.controllerMethods();
        loggingAspect.logBefore(joinPoint);
        loggingAspect.logAfter(joinPoint);
        loggingAspect.logAfterReturning(joinPoint, "ok");
        loggingAspect.logAfterThrowing(joinPoint, new RuntimeException("boom"));
    }

    @Test
    void aroundAdvice_returnsResultOnSuccess() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getTarget()).thenReturn(this);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("aroundSuccess");
        when(joinPoint.proceed()).thenReturn("done");

        Object result = loggingAspect.logAround(joinPoint);

        assertEquals("done", result);
    }

    @Test
    void aroundAdvice_rethrowsFailure() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        RuntimeException failure = new RuntimeException("failed");
        when(joinPoint.getTarget()).thenReturn(this);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("aroundFailure");
        when(joinPoint.proceed()).thenThrow(failure);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> loggingAspect.logAround(joinPoint));

        assertEquals("failed", thrown.getMessage());
    }
}
