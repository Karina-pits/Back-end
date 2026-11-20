package com.clinic.notification.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Топологія брокера. Те саме оголошує і clinic-service (оголошення ідемпотентні):
 * так черга існує й накопичує повідомлення, навіть якщо споживач ще жодного разу не запускався.
 *
 * Надійність: усе durable (переживає перезапуск брокера), повідомлення persistent,
 * а повідомлення, які не вдалося обробити після всіх спроб, потрапляють у DLQ
 * (мертва черга), а не губляться.
 */
@Configuration
public class NotificationMessagingConfig {

    public static final String EXCHANGE = "clinic.events";
    public static final String QUEUE = "notification.appointments";
    public static final String DEAD_LETTER_EXCHANGE = "clinic.events.dlx";
    public static final String DEAD_LETTER_QUEUE = "notification.appointments.dlq";
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
        // усі три події: appointment.created / appointment.cancelled / appointment.completed
        return BindingBuilder.bind(notificationQueue()).to(clinicEventsExchange()).with("appointment.#");
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DEAD_LETTER_KEY);
    }

    /**
     * JSON-конвертер. Тип повідомлення визначається за параметром методу слухача
     * (INFERRED), а не за іменем класу у заголовку, надісланому видавцем.
     */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        typeMapper.setTrustedPackages("com.clinic.common.dto");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
