package egghead.eggheadkafka.kafka.spring;

import com.google.common.collect.ImmutableMap;
import egghead.eggheadkafka.kafka.model.Request;
import egghead.eggheadkafka.kafka.model.Response;
import egghead.eggheadkafka.kafka.processing.RequestProcessor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * @author mikael
 */
@Configuration
@EnableKafka
@ComponentScan(basePackageClasses = {KafkaSpringConfiguration.class, RequestProcessor.class})
public class KafkaSpringConfiguration {

    public static final String GROUP_ID = "aGroupId";
    public static final String REQUEST_TOPIC = "aRequestApi";
    public static final String RESPONSE_TOPIC = "aResponseApi";

    @Value(value = "${kafka.bootstrapAddress}")
    private String bootstrapAddress;

    @Bean
    public ConsumerFactory<String, Request> consumerFactory() {

        return new DefaultKafkaConsumerFactory<>(ImmutableMap.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress,
            ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, Request.RequestDeserializer.class
        ));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Request> kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, Request> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    @Bean
    public ProducerFactory<String, Response> producerFactory() {

        return new DefaultKafkaProducerFactory<>(ImmutableMap.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, Response.ResponseSerializer.class
        ));
    }

    @Bean
    public KafkaTemplate<String, Response> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
