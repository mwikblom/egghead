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
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import reactor.kafka.sender.SenderRecord;

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

    @Value("${kafka.swish.response.topic}")
    private String swishResponseTopic;

    private Disposable kafkaConsumer;
    private KafkaSender<Integer, String> kafkaSender;

    @PostConstruct
    public void setupKafkaConsumer() {
        kafkaConsumer = this.createKafkaConsumer(swishRequestTopic);
    }

    private ReceiverOptions<Integer, String> createReceiverOptions() {
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

    private Disposable createKafkaConsumer(String topic) {

        ReceiverOptions<Integer, String> options = createReceiverOptions()
            .subscription(Collections.singleton(topic));

        Flux<ReceiverRecord<Integer, String>> kafkaFlux = KafkaReceiver.create(options)
            .receive();

        Flux<SenderRecord<Integer, String, Integer>> outFlux = kafkaFlux.map(record -> {
            ReceiverOffset offset = record.receiverOffset();
            System.out.printf("Received message: topic-partition=%s offset=%d timestamp=%s key=%d value=%s\n",
                offset.topicPartition(),
                offset.offset(),
                new SimpleDateFormat("HH:mm:ss:SSS z dd MMM yyyy").format(new Date(record.timestamp())),
                record.key(),
                record.value());
            return SenderRecord.create(swishResponseTopic, null, null, null, "Message_" + record.value(), null);
        });

        outFlux.as(x -> kafkaSender.send(x));


        return kafkaFlux.subscribe(record -> {
            ReceiverOffset offset = record.receiverOffset();
            System.out.printf("Received message: topic-partition=%s offset=%d timestamp=%s key=%d value=%s\n",
                offset.topicPartition(),
                offset.offset(),
                new SimpleDateFormat("HH:mm:ss:SSS z dd MMM yyyy").format(new Date(record.timestamp())),
                record.key(),
                record.value());
            Mono<SenderRecord<Integer, String, Integer>> message = Mono.just(SenderRecord.create(swishResponseTopic, null, null, null, "Message_" + record.value(), null));
            kafkaSender.send(message)
                .subscribe(senderResult -> {

                });
            offset.acknowledge();
        });
    }

    @Bean
    public KafkaSender<Integer, String> getKafkaSender() {
        kafkaSender = KafkaSender.create(createSenderOptions());
        return kafkaSender;
    }

    @PreDestroy
    public void disposeKafkaConsumer() {
        kafkaConsumer.dispose();
    }

    @PreDestroy
    public void closeKafkaSender() {
        kafkaSender.close();
    }
}
