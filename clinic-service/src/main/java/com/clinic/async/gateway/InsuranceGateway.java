package com.clinic.async.gateway;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Повільний побічний ефект №3: виклик зовнішнього сервісу (перевірка страховки пацієнта).
 * Зараз це імітація; у реальному проєкті тут був би HTTP-клієнт до API страхової.
 */
@Component
@RequiredArgsConstructor
public class InsuranceGateway {

    private final SimulatedWork work;

    public String check(Long patientId) {
        work.pause();
        return "ACTIVE";
    }
}
