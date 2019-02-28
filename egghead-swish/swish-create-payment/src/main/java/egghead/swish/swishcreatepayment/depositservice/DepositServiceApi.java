package egghead.swish.swishcreatepayment.depositservice;

import egghead.swish.swishcreatepayment.depositservice.model.DepositOrderResponse;
import egghead.swish.swishcreatepayment.kafka.model.SwishDepositKafkaRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * @author mikael
 */
@Service
public class DepositServiceApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(DepositServiceApi.class);
    private final WebClient webClient;

    public DepositServiceApi() {

        this.webClient = WebClient.builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .build();
    }

    public Mono<DepositOrderResponse> callDepositOrder(Scheduler scheduler, SwishDepositKafkaRequest swishDepositKafkaRequest) {
        String path = "todos/1";

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
}
