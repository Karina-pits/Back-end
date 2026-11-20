package com.clinic.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Топологія брокера з боку видавця. Оголошення ідентичні до notification-service
 * (вони ідемпотентні). Чергу оголошуємо і тут, щоб події, опубліковані ДО першого
 * запуску споживача (або поки він зупинений), не губилися, а чекали в черзі.
 * Назви дублюються текстом, а не спільним класом: сервіси не залежать один від одного.
 */
@Configuration
public class ClinicMessagingConfig {

    public static final String EXCHANGE = "clinic.events";
    private static final String QUEUE = "notification.appointments";
    private static final String DEAD_LETTER_EXCHANGE = "clinic.events.dlx";
    private static final String DEAD_LETTER_QUEUE = "notification.appointments.dlq";
    private static final String DEAD_LETTER_KEY = "dead";

    @Bean
    public TopicExchange clinicEventsExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(DEAD_LETTER_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DEAD_LETTER_KEY)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue()).to(clinicEventsExchange()).with("appointment.#");
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DEAD_LETTER_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
