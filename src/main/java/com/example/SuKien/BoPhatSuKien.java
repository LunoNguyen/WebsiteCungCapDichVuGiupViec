package com.example.SuKien;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Component;

/**
 * Cho entity listener (do Hibernate tạo, không phải bean Spring) phát sự kiện Spring.
 */
@Component
public class BoPhatSuKien implements ApplicationEventPublisherAware {

    private static volatile ApplicationEventPublisher publisher;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher p) {
        publisher = p;
    }

    public static void phat(Object suKien) {
        ApplicationEventPublisher p = publisher;
        if (p != null) p.publishEvent(suKien);
    }
}
