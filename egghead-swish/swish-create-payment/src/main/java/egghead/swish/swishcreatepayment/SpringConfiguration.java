package egghead.swish.swishcreatepayment;

import com.google.common.collect.ImmutableMap;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.Map;

/**
 * @author mikael
 */
@Configuration
public class SpringConfiguration {

    @Value("${kafka.bootstrap.address}")
    private String bootstrapServers;
    @Value("${kafka.swish.request.topic}")
    private String swishRequestTopic;

    @Value("${kafka.swish.response.topic}")
    private String swishResponseTopic;

    private SenderOptions<Integer, String> createSenderOptions() {
        Map<String, Object> props = new ImmutableMap.Builder<String, Object>()
            .put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            .put(ProducerConfig.CLIENT_ID_CONFIG, "sample-producer")
            .put(ProducerConfig.ACKS_CONFIG, "all")
            .put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class)
            .put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
            .build();
        return SenderOptions.create(props);
    }

    @Bean
    public KafkaSender<Integer, String> getKafkaSender() {
        return KafkaSender.create(createSenderOptions());
    }

    @Bean
    public ReceiverOptions<Integer, String> getReceiverOptions() {
        Map<String, Object> props = new ImmutableMap.Builder<String, Object>()
            .put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            .put(ConsumerConfig.CLIENT_ID_CONFIG, "sample-consumer")
            .put(ConsumerConfig.GROUP_ID_CONFIG, "sample-group")
            .put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class)
            .put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class)
            .put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            .build();
        return ReceiverOptions.create(props);
    }

/*    @PreDestroy
    public void disposeKafkaConsumer() {
        if (kafkaConsumer != null) {
            kafkaConsumer.dispose();
        }
    }

    @PreDestroy
    public void closeKafkaSender() {
        kafkaSender.close();
    }*/
}
