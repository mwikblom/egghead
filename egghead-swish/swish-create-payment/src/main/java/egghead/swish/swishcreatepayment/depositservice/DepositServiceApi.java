package egghead.swish.swishcreatepayment.depositservice;

import egghead.swish.swishcreatepayment.depositservice.model.DepositOrderResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * @author mikael
 */
public class DepositServiceApi {

    public Mono<DepositOrderResponse> callDepositOrder() {
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

}
