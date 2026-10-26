package com.clinic.async.service;

import com.clinic.async.config.AsyncConfig;
import com.clinic.async.gateway.InsuranceGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Асинхронна обгортка над повільним зовнішнім викликом.
 * Якщо шлюз кине виняток, Future завершиться з помилкою (completeExceptionally),
 * а викликаючий код обробить це через exceptionally/handle.
 */
@Service
@RequiredArgsConstructor
public class InsuranceCheckService {

    private final InsuranceGateway insuranceGateway;

    @Async(AsyncConfig.CLINIC_EXECUTOR)
    public CompletableFuture<String> checkInsurance(Long patientId) {
        return CompletableFuture.completedFuture(insuranceGateway.check(patientId));
    }
}
