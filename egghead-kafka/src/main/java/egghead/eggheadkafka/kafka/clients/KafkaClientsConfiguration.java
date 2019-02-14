package egghead.eggheadkafka.kafka.clients;

import com.google.common.collect.ImmutableMap;
import egghead.eggheadkafka.kafka.model.Request;
import egghead.eggheadkafka.kafka.model.Response;
import egghead.eggheadkafka.kafka.processing.RequestProcessor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
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

import java.util.Collections;
import java.util.Properties;

/**
 * @author mikael
 */
@Configuration
@EnableKafka
@ComponentScan(basePackageClasses = {KafkaClientsConfiguration.class, RequestProcessor.class})
public class KafkaClientsConfiguration {

    public static final String GROUP_ID = "aGroupId";
    public static final String REQUEST_TOPIC = "aRequestApi";
    public static final String RESPONSE_TOPIC = "aResponseApi";

    @Value(value = "${kafka.bootstrapAddress}")
    private String bootstrapAddress;

    @Bean
    public Producer<String, Response> producer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, Response.ResponseSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    @Bean
    private Consumer<String, Request> consumer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, Request.RequestDeserializer.class.getName());

        Consumer<String, Request> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(REQUEST_TOPIC));
        return consumer;
    }


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
