package egghead.swish.swishcreatepayment.swish.model;

/**
 * @author mikael
 */
public enum SwishStatusCode {

    DEBITED("Refund: Money has been taken from the payers account"),
    CREATED("Payment: The payment request was created"),
    DECLINED("Refund: The payer declined the payment"),
    ERROR("Payment/Refund: Some error occurred. See SwishErrorCode"),
    PAID("Payment/Refund: The payment or refund was successful"),
    VALIDATED("Refund: The refund was validated");

    private final String message;

    SwishStatusCode(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return this.name() + ": " + message;
    }
}
