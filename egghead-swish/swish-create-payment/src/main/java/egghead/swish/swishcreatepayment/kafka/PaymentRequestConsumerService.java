package egghead.swish.swishcreatepayment.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

@Service
public class PaymentRequestConsumerService {
    private final static Logger LOGGER = LoggerFactory.getLogger(PaymentRequestConsumerService.class);
    private final KafkaSender<Integer, String> kafkaSender;
    private final ReceiverOptions<Integer, String> receiverOptions;
    private final String swishProducerTopic;
    private final String swishConsumerTopic;
    private Disposable kafkaConsumer;

    @Autowired
    public PaymentRequestConsumerService(KafkaSender<Integer, String> kafkaSender,
                                         @Value("${kafka.swish.response.topic}") String swishProducerTopic,
                                         @Value("${kafka.swish.request.topic}") String swishConsumerTopic,
                                         ReceiverOptions<Integer, String> receiverOptions) {
        this.kafkaSender = kafkaSender;
        this.swishProducerTopic = swishProducerTopic;
        this.swishConsumerTopic = swishConsumerTopic;
        this.receiverOptions = receiverOptions;
    }

    @PostConstruct
    private void initKafkaConsumer() {
        LOGGER.info("Starting Kafka Consumer For Deposit Requests");
        kafkaConsumer = createKafkaConsumer();
        LOGGER.info("Created.");
    }

    private Mono<Tuple2<String, ReceiverOffset>> createBlockingCall(Scheduler scheduler, String message, ReceiverOffset offset) {
        return Mono.fromCallable(() -> {
            Thread.sleep(5000);
            String value = message + " TESTO";
            System.out.println("Thread is:" + Thread.currentThread().getName());
            return Tuples.of(value, offset);
        }).subscribeOn(scheduler);
    }

    private Disposable createKafkaConsumer() {
        ReceiverOptions<Integer, String> options = receiverOptions
            .subscription(Collections.singleton(swishConsumerTopic));

        Scheduler scheduler = Schedulers.elastic();

        return KafkaReceiver.create(options)
            .receive()
            .map(record -> {
                ReceiverOffset offset = record.receiverOffset();
                System.out.printf("Received message: topic-partition=%s offset=%d timestamp=%s key=%d value=%s\n",
                    offset.topicPartition(),
                    offset.offset(),
                    new SimpleDateFormat("HH:mm:ss:SSS z dd MMM yyyy").format(new Date(record.timestamp())),
                    record.key(),
                    record.value());

                // HTTP-blocking call-ish.
                return createBlockingCall(scheduler, record.value(), offset);
            })
            .subscribe(publisher -> publisher
                .map(value -> SenderRecord.create(new ProducerRecord<Integer, String>(swishProducerTopic, "Message: " + value.getT1()), value.getT2()))
                .as(kafkaSender::send)
                .subscribe());
    }

    @PreDestroy
    private void disposeKafkaConsumer() {
        if (kafkaConsumer != null) {
            kafkaConsumer.dispose();
        }
    }

    @PreDestroy
    public void closeKafkaSender() {
        kafkaSender.close();
    }
}
