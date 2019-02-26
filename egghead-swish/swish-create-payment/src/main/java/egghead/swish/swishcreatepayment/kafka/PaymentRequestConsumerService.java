package egghead.swish.swishcreatepayment.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
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
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PaymentRequestConsumerService {
    private final static Logger LOGGER = LoggerFactory.getLogger(PaymentRequestConsumerService.class);
    private final KafkaSender<Integer, String> kafkaSender;
    private final ReceiverOptions<Integer, String> receiverOptions;
    private final String swishProducerTopic;
    private final String swishConsumerTopic;
    private Disposable kafkaConsumer;
    private AtomicInteger counter = new AtomicInteger();

    public static class Response {
        private long userId;
        private long id;
        private String title;
        private boolean completed;

        public long getUserId() {
            return userId;
        }

        public void setUserId(long userId) {
            this.userId = userId;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }
    }

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

    private Mono<String> callDepositService(Scheduler scheduler) {
        String path = "todos/1";
        WebClient webClient = WebClient.builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .build();


        return webClient.get()
            .uri(path)
            .retrieve()
            .bodyToMono(Response.class)
            .subscribeOn(scheduler)
            .map(response -> {
                Integer currentCount = counter.getAndIncrement();
                LOGGER.info("getOrderInfo - THREAD-ID: {}, OrderId {}:", Thread.currentThread().getId(), currentCount);
                return String.valueOf(currentCount);
            });
    }

    private Mono<Tuple2<String, ReceiverOffset>> callCreateDeposit(String orderId, Scheduler scheduler, String message, ReceiverOffset offset) {
        String path = "todos/1";
        WebClient webClient = WebClient.builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .build();

        return webClient.get()
            .uri(path)
            .retrieve()
            .bodyToMono(Response.class)
            .subscribeOn(scheduler)
            .map(response -> {
                String value = String.format("Title: %s. Original message: %s", response.getTitle(), message);
                LOGGER.info("CreateDeposit - THREAD.ID {}, ORDER-ID {}, MESSAGE: {}", Thread.currentThread().getId(), orderId, message);

                return Tuples.of(value, offset);
            });
    }

    private Mono<Tuple2<String, ReceiverOffset>> doPaymentChain(String message, ReceiverOffset offset) {
        Scheduler scheduler = Schedulers.elastic();
        Mono<String> metaData = callDepositService(scheduler);
        return metaData
            .flatMap(foo -> callCreateDeposit(foo, scheduler, message, offset));

/*        Mono<Tuple2<String, ReceiverOffset>> joinedMono = metaData
            .zipWith(paymentRequestResponse, (orderInfo, paymentResponse) -> {
                LOGGER.info("THREAD.ID: {}          Got orderInfo {} and payment value: {}", Thread.currentThread().getId(), orderInfo, paymentResponse.getT1());
                return paymentResponse;
        });*/
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
            .flatMap(record -> {
                ReceiverOffset offset = record.receiverOffset();
                // HTTP-blocking callCreateDeposit-ish.
                return doPaymentChain(record.value(), offset);
            })
            .map(value -> SenderRecord.create(new ProducerRecord<Integer, String>(swishProducerTopic, "Message: " + value.getT1()), value.getT2()))
            .as(kafkaSender::send)
            .subscribe();
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
