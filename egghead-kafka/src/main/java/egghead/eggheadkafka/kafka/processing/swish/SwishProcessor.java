package egghead.eggheadkafka.kafka.processing.swish;

import egghead.eggheadkafka.kafka.model.Request;
import egghead.eggheadkafka.kafka.model.Response;
import egghead.eggheadkafka.kafka.processing.PaymentMethodProcessor;

import java.util.Collections;

/**
 * @author mikael
 */
public class SwishProcessor implements PaymentMethodProcessor {

    @Override
    public Response process(Request request) {
        Response response = new Response();
        response.setVersion(0);
        response.setId(request.getId());
        response.setMetaInfo(Collections.emptyMap());
        return response;
    }
}
