package egghead.swish.swishcreatepayment;

import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.google.common.collect.ImmutableMap;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
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
    private Disposable kafkaConsumer;

    @PostConstruct
    public void setupKafkaConsumer() {
        kafkaConsumer = this.createKafkaConsumer(swishRequestTopic);
    }

    private ReceiverOptions<Integer, String> createReceiverOptions() {
        Map<String, Object> props = new ImmutableMap.Builder<String, Object>()
            .put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            .put(ConsumerConfig.CLIENT_ID_CONFIG, "sample-consumer")
            .put(ConsumerConfig.GROUP_ID_CONFIG, "sample-group")
            .put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, NumberDeserializers.IntegerDeserializer.class)
            .put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class)
            .put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            .build();
        return ReceiverOptions.create(props);
    }

    private Disposable createKafkaConsumer(String topic) {

        ReceiverOptions<Integer, String> options = createReceiverOptions()
            .subscription(Collections.singleton(topic));

        Flux<ReceiverRecord<Integer, String>> kafkaFlux = KafkaReceiver.create(options)
            .receive();

        return kafkaFlux.subscribe(record -> {
            ReceiverOffset offset = record.receiverOffset();
            System.out.printf("Received message: topic-partition=%s offset=%d timestamp=%s key=%d value=%s\n",
                offset.topicPartition(),
                offset.offset(),
                new SimpleDateFormat("HH:mm:ss:SSS z dd MMM yyyy").format(new Date(record.timestamp())),
                record.key(),
                record.value());
            offset.acknowledge();
        });
    }

    @PreDestroy
    public void disposeKafkaConsumer() {
        kafkaConsumer.dispose();
    }
}
