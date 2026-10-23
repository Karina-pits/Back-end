package com.clinic.aop.demo;

import com.clinic.aop.annotation.MeasureTime;
import org.springframework.stereotype.Service;

/**
 * НАВМИСНА демонстрація self-invocation (за умовою лаби це задум, а не баг).
 *
 * Один і той самий метод timedOperation() викликається двома шляхами:
 *  1) ззовні (з іншого біна/контролера) - виклик іде через проксі Spring,
 *     тому аспект @MeasureTime СПРАЦЬОВУЄ;
 *  2) зсередини цього ж класу через this.timedOperation() - виклик іде
 *     напряму в об'єкт, ПОМИНАЮЧИ проксі, тому аспект МОВЧКИ НЕ СПРАЦЬОВУЄ
 *     (без жодної помилки чи попередження).
 */
@Service
public class SelfInvocationDemoService {

    @MeasureTime(warnThresholdMs = 1000)
    public String timedOperation() {
        return "timedOperation executed";
    }

    public String callViaThis() {
        // this = реальний об'єкт, а не проксі -> advice не виконається
        return this.timedOperation();
    }
}
