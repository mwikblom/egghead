package egghead.swish.swishcreatepayment.kafka;

import egghead.swish.swishcreatepayment.depositservice.model.DepositOrderResponse;
import egghead.swish.swishcreatepayment.swish.SwishApi;
import egghead.swish.swishcreatepayment.swish.model.CreatePaymentRequestResponse;
import egghead.swish.swishcreatepayment.swish.model.PaymentRequestObject;
import egghead.swish.swishcreatepayment.swish.model.SwishPaymentStatus;
import egghead.swish.swishcreatepayment.kafka.model.SwishDepositKafkaRequest;
import egghead.swish.swishcreatepayment.kafka.model.UiCreatePaymentKafkaResponse;
import egghead.swish.swishcreatepayment.kafka.model.WorkflowDepositFinalizedKafkaResponse;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import reactor.util.function.Tuple4;
import reactor.util.function.Tuples;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.ws.Response;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.Currency;

@Service
public class PaymentRequestConsumerService {
    private final static Logger LOGGER = LoggerFactory.getLogger(PaymentRequestConsumerService.class);

    private final KafkaSender<Integer, UiCreatePaymentKafkaResponse> kafkaSenderForUiCreatePaymentResponse;
    private final KafkaSender<Integer, WorkflowDepositFinalizedKafkaResponse> kafkaSenderForWorkflowDepositFinalizedResponse;
    private final String swishDepositRequestTopic;
    private final String uiCreatePaymentResponseTopic;
    private final String workflowDepositFinalizedResponseTopic;
    private final ReceiverOptions<Integer, SwishDepositKafkaRequest> receiverOptionsForSwishDepositRequest;
    private final SwishApi swishApi;

    private Disposable kafkaConsumerForSwishDepositRequest;

    @Autowired
    public PaymentRequestConsumerService(KafkaSender<Integer, UiCreatePaymentKafkaResponse> kafkaSenderForUiCreatePaymentResponse,
                                         KafkaSender<Integer, WorkflowDepositFinalizedKafkaResponse> kafkaSenderForWorkflowDepositFinalizedResponse,
                                         @Value("${trustly.swish.SwishDepositRequest.topic}") String swishDepositRequestTopic,
                                         @Value("${trustly.swish.UiCreatePaymentResponse.topic}") String uiCreatePaymentResponseTopic,
                                         @Value("${trustly.swish.WorkflowDepositFinalizedResponse.topic}") String workflowDepositFinalizedResponseTopic,
                                         ReceiverOptions<Integer, SwishDepositKafkaRequest> receiverOptionsForSwishDepositRequest,
                                         SwishApi swishApi) {
        this.kafkaSenderForUiCreatePaymentResponse = kafkaSenderForUiCreatePaymentResponse;
        this.kafkaSenderForWorkflowDepositFinalizedResponse = kafkaSenderForWorkflowDepositFinalizedResponse;
        this.swishDepositRequestTopic = swishDepositRequestTopic;
        this.uiCreatePaymentResponseTopic = uiCreatePaymentResponseTopic;
        this.workflowDepositFinalizedResponseTopic = workflowDepositFinalizedResponseTopic;
        this.receiverOptionsForSwishDepositRequest = receiverOptionsForSwishDepositRequest;
        this.swishApi = swishApi;
    }

    @PostConstruct
    private void initKafkaConsumer() {
        LOGGER.info("Starting Kafka Consumer For Deposit Requests");
        kafkaConsumerForSwishDepositRequest = createKafkaConsumer();
        LOGGER.info("Created.");
    }

    private Mono<DepositOrderResponse> callDepositService(Scheduler scheduler, SwishDepositKafkaRequest swishDepositKafkaRequest) {
        String path = "todos/1";
        WebClient webClient = WebClient.builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .build();

        return webClient.get()
            .uri(path)
            .retrieve()
            .bodyToMono(DepositOrderResponse.class)
            .subscribeOn(scheduler)
            .map(depositOrder -> {

                // fake some data here
                depositOrder.setOrderId(swishDepositKafkaRequest.getOrderId());
                depositOrder.setAmount(new BigDecimal(10));
                depositOrder.setCurrency(Currency.getInstance("SEK"));
                depositOrder.setMerchantSwishAlias("1231181189");
                depositOrder.setPayerPhoneNumber("073454321");
                depositOrder.setMobile(true);
                depositOrder.setMessageOnStatement("aPayment");

                LOGGER.info("callDepositService - THREAD-ID: {}: {}:", Thread.currentThread().getId(), depositOrder);

                return depositOrder;
            });
    }

    private Mono<CreatePaymentRequestResponse> callSwishPaymentRequest(Scheduler scheduler, SwishDepositKafkaRequest swishDepositKafkaRequest, DepositOrderResponse depositOrder) {

        PaymentRequestObject paymentRequestObject = new PaymentRequestObject();
        paymentRequestObject.setAmount(depositOrder.getAmount().toString());
        paymentRequestObject.setCallbackUrl("https://www.youtube.com");
        paymentRequestObject.setMessage(depositOrder.getMessageOnStatement());
        paymentRequestObject.setCurrency(depositOrder.getCurrency().getCurrencyCode());
        paymentRequestObject.setPayeePaymentReference(depositOrder.getOrderId());
        paymentRequestObject.setPayeeAlias(depositOrder.getMerchantSwishAlias());

        return swishApi.callCreatePaymentRequest(scheduler, paymentRequestObject);
    }

    private Mono<Tuple4<SwishDepositKafkaRequest, ReceiverOffset, DepositOrderResponse, CreatePaymentRequestResponse>> doSwishDepositChain(SwishDepositKafkaRequest swishDepositKafkaRequest, ReceiverOffset receiverOffset) {

        // TODO correct scheduler
        Scheduler scheduler = Schedulers.elastic();

        Mono<DepositOrderResponse> callDepositService = callDepositService(scheduler, swishDepositKafkaRequest);
        return callDepositService
            .zipWhen(depositOrder -> callSwishPaymentRequest(scheduler, swishDepositKafkaRequest, depositOrder), (depositOrder, createPaymentRequestResponse)
                -> Tuples.of(swishDepositKafkaRequest, receiverOffset, depositOrder, createPaymentRequestResponse));
    }

    private Flux<SwishPaymentStatus> pollSwishPaymentStatus(Scheduler scheduler, SwishDepositKafkaRequest swishDepositKafkaRequest, DepositOrderResponse depositOrder, CreatePaymentRequestResponse swishPaymentRequest) {
        String path = "todos/1";
        WebClient webClient = WebClient.builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .build();

        // TODO use swishPaymentRequestResponse.location to poll status

        // Wait 5 seconds before starting. Then we poll every 2 seconds
        Flux<Long> interval = Flux.interval(Duration.ofSeconds(5), Duration.ofSeconds(2));

        Mono<SwishPaymentStatus> pollReq =
            webClient.get()
                .uri(path)
                .retrieve()
                .bodyToMono(Response.class)
                .subscribeOn(scheduler)
                .map(response -> {
                    // fake som data here
                    SwishPaymentStatus swishPaymentStatus = new SwishPaymentStatus();
                    swishPaymentStatus.setStatus(SwishPaymentStatus.Status.PENDING);

                    LOGGER.info("pollSwishPaymentStatus - THREAD-ID {}: {}", Thread.currentThread().getId(), swishPaymentStatus);
                    return swishPaymentStatus;
                });

        return Flux.from(interval)
            .flatMap(count -> pollReq)
            // Here we should check polling-status. Now we just say if Response == stop, then we stop.
            .takeWhile(swishPaymentStatus -> swishPaymentStatus.getStatus() == SwishPaymentStatus.Status.PENDING)
            // Poll for 15 seconds.
            .take(Duration.ofSeconds(15))
            .doFinally(signalType -> LOGGER.info("Done polling order: {}", depositOrder.getOrderId()));
    }

    private Disposable createKafkaConsumer() {
        ReceiverOptions<Integer, SwishDepositKafkaRequest> options = receiverOptionsForSwishDepositRequest
            .subscription(Collections.singleton(swishDepositRequestTopic));

        // TODO correct scheduler
        Scheduler scheduler = Schedulers.elastic();

        ConnectableFlux<Tuple4<SwishDepositKafkaRequest, ReceiverOffset, DepositOrderResponse, CreatePaymentRequestResponse>> flux = KafkaReceiver.create(options)
            .receive()
            .flatMap(record -> {
                ReceiverOffset receiverOffset = record.receiverOffset();
                SwishDepositKafkaRequest swishDepositKafkaRequest = record.value();

                LOGGER.info("Received Swish deposit request: {}", swishDepositKafkaRequest);

                // HTTP-blocking callCreateDeposit-ish.
                return doSwishDepositChain(swishDepositKafkaRequest, receiverOffset);
            }).publish();

        // Starts poller
        Flux.from(flux)
            .flatMap(swishDepositKafkaRequestOffsetDepositOrderAndSwishPaymentRequest -> {
                SwishDepositKafkaRequest swishDepositKafkaRequest = swishDepositKafkaRequestOffsetDepositOrderAndSwishPaymentRequest.getT1();
                DepositOrderResponse depositOrder = swishDepositKafkaRequestOffsetDepositOrderAndSwishPaymentRequest.getT3();
                CreatePaymentRequestResponse swishPaymentRequest = swishDepositKafkaRequestOffsetDepositOrderAndSwishPaymentRequest.getT4();

                return pollSwishPaymentStatus(scheduler, swishDepositKafkaRequest, depositOrder, swishPaymentRequest);
            })
            .subscribe();

        // Produces record to producer topic..
        flux
            .map(swishDepositKafkaRequestOffsetDepositOrderAndSwishPaymentRequest -> {
                CreatePaymentRequestResponse swishPaymentRequest = swishDepositKafkaRequestOffsetDepositOrderAndSwishPaymentRequest.getT4();
                DepositOrderResponse depositOrder = swishDepositKafkaRequestOffsetDepositOrderAndSwishPaymentRequest.getT3();
                ReceiverOffset receiverOffset = swishDepositKafkaRequestOffsetDepositOrderAndSwishPaymentRequest.getT2();

                UiCreatePaymentKafkaResponse uiCreatePaymentResponse = new UiCreatePaymentKafkaResponse();
                uiCreatePaymentResponse.setOrderId(depositOrder.getOrderId());
                if (swishPaymentRequest.getPaymentRequestToken() != null) {
                    uiCreatePaymentResponse.setAutoStartToken(swishPaymentRequest.getPaymentRequestToken());
                    uiCreatePaymentResponse.setOpenSwishUrl("swish://" + swishPaymentRequest.getPaymentRequestToken()); // TODO correct
                }

                return SenderRecord.create(new ProducerRecord<Integer, UiCreatePaymentKafkaResponse>(uiCreatePaymentResponseTopic, uiCreatePaymentResponse), receiverOffset);
            })
            .as(kafkaSenderForUiCreatePaymentResponse::send)
            .subscribe();

        return flux.connect();
    }

    @PreDestroy
    private void disposeKafkaConsumer() {
        if (kafkaConsumerForSwishDepositRequest != null) {
            kafkaConsumerForSwishDepositRequest.dispose();
        }
    }

    @PreDestroy
    public void closeKafkaSender() {
        kafkaSenderForUiCreatePaymentResponse.close();
    }
}
