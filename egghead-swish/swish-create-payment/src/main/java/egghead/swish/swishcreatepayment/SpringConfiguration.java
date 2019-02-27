package egghead.swish.swishcreatepayment;

import com.google.common.collect.ImmutableMap;
import egghead.swish.swishcreatepayment.kafka.model.SwishDepositRequest;
import egghead.swish.swishcreatepayment.kafka.model.UiCreatePaymentResponse;
import egghead.swish.swishcreatepayment.kafka.model.WorkflowDepositFinalizedResponse;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import javax.annotation.PreDestroy;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author mikael
 */
@Configuration
public class SpringConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringConfiguration.class);

    @Value("${kafka.bootstrap.address}")
    private String bootstrapServers;

    private final Set<KafkaSender> kafkaSenders;

    public SpringConfiguration() {
        kafkaSenders = new HashSet<>();
    }

    private <T> SenderOptions<Integer, T> senderOptions(Class<? extends Serializer<T>> valueSerializerClass) {
        Map<String, Object> props = new ImmutableMap.Builder<String, Object>()
            .put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            .put(ProducerConfig.CLIENT_ID_CONFIG, "sample-producer")
            .put(ProducerConfig.ACKS_CONFIG, "all")
            .put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class)
            .put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializerClass)
            .build();
        return SenderOptions.create(props);
    }

    @Bean
    public KafkaSender<Integer, UiCreatePaymentResponse> kafkaSenderForUiCreayePaymentResponse() {
        KafkaSender<Integer, UiCreatePaymentResponse> kafkaSender = KafkaSender.create(senderOptions(UiCreatePaymentResponse.ResponseSerializer.class));
        kafkaSenders.add(kafkaSender);
        return kafkaSender;
    }

    @Bean
    public KafkaSender<Integer, WorkflowDepositFinalizedResponse> kafkaSenderForWorkflowDepositFinalizedResponse() {
        KafkaSender<Integer, WorkflowDepositFinalizedResponse> kafkaSender = KafkaSender.create(senderOptions(WorkflowDepositFinalizedResponse.ResponseSerializer.class));
        kafkaSenders.add(kafkaSender);
        return kafkaSender;
    }

    @Bean
    public ReceiverOptions<Integer, SwishDepositRequest> receiverOptionsForSwishDepositRequest() {
        Map<String, Object> props = new ImmutableMap.Builder<String, Object>()
            .put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            .put(ConsumerConfig.CLIENT_ID_CONFIG, "sample-consumer")
            .put(ConsumerConfig.GROUP_ID_CONFIG, "sample-group")
            .put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class)
            .put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, SwishDepositRequest.RequestDeserializer.class) // TODO maybe use JsonDeserializer
            .put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            .build();
        return ReceiverOptions.create(props);
    }

    @PreDestroy
    public void closeKafkaSenders() {
        kafkaSenders.forEach(kafkaSender -> {
            try {
                kafkaSender.close();
            } catch (Exception e) {
                LOGGER.info("Unable to close kafka sender", e);
            }
        });
    }
}
