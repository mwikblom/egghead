package egghead.eggheadkafka.kafka.reactor;

import com.google.common.collect.ImmutableMap;
import egghead.eggheadkafka.kafka.model.Request;
import egghead.eggheadkafka.kafka.model.Response;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.Map;

/**
 * @author mikael
 */
@Configuration
@EnableKafka
@ComponentScan(basePackageClasses = {KafkaReactorConfiguration.class, RequestProcessor.class})
public class KafkaReactorConfiguration {

    public static final String GROUP_ID = "aGroupId";
    public static final String REQUEST_TOPIC = "aRequestApi";
    public static final String RESPONSE_TOPIC = "aResponseApi";
    public static final String SENDER_CONFIG = "aSenderConfig";
    public static final String RECEIVER_CONFIG = "aReceiverConfig";

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaReactorConfiguration.class);

    @Value(value = "${kafka.bootstrapAddress}")
    private String bootstrapAddress;

    @Resource
    private RequestProcessor requestProcessor;

    private Disposable requestApiSubscription;

    @Bean
    public KafkaSender<String, Response> kafkaSender() {
        return KafkaSender.create(SenderOptions.create(senderProps()));
    }

    private Map<String, Object> senderProps() {
        return ImmutableMap.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress,
            ProducerConfig.CLIENT_ID_CONFIG, SENDER_CONFIG,
            ProducerConfig.ACKS_CONFIG, "all",
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringDeserializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, Request.RequestDeserializer.class
        );
    }

    @PostConstruct
    public void subscribeToRequests() {

        KafkaReceiver<String, Request> kafkaReceiver = KafkaReceiver.create(
            ReceiverOptions.<String, Request>create(receiverProps())
                .subscription(Collections.singletonList(KafkaReactorConfiguration.REQUEST_TOPIC))
        );

        Flux<ReceiverRecord<String, Request>> flux = kafkaReceiver.receive()
            .onErrorContinue((throwable, object) -> {
                LOGGER.warn("Caught throwable {} continue", throwable);
            });

        requestApiSubscription = flux.subscribe(receiverRecord -> {
            ReceiverOffset receiverOffset = receiverRecord.receiverOffset();

            requestProcessor.process(receiverRecord.value());

            receiverOffset.acknowledge();
        });
    }

    @PreDestroy
    public void disposeSubscription() {
        if (requestApiSubscription != null && !requestApiSubscription.isDisposed()) {
            requestApiSubscription.dispose();
        }
    }

    private Map<String, Object> receiverProps() {
        return new ImmutableMap.Builder<String, Object>()
            .put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress)
            .put(ConsumerConfig.CLIENT_ID_CONFIG, RECEIVER_CONFIG)
            .put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID)
            .put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class)
            .put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, Request.RequestDeserializer.class)
            .put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            .build();
    }
}
