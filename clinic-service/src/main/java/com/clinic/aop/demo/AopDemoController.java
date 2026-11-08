package com.clinic.aop.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Два ендпоінти для живої демонстрації на захисті (потрібен JWT, бо /api/**).
 * Обидва повертають однакову відповідь, але в консолі IDE:
 *  - /external -> з'являється рядок [MeasureTime] ... took ... ms
 *  - /self     -> такого рядка НЕМАЄ (аспект обійдено через this.)
 */
@RestController
@RequestMapping("/api/demo/aop")
@RequiredArgsConstructor
public class AopDemoController {

    private final SelfInvocationDemoService demoService;

    @GetMapping("/external")
    public Map<String, String> external() {
        return Map.of("path", "external call (through proxy)",
                "result", demoService.timedOperation());
    }

    @GetMapping("/self")
    public Map<String, String> self() {
        return Map.of("path", "self-invocation (this.method(), proxy bypassed)",
                "result", demoService.callViaThis());
    }
}
