package egghead.swish.swishcreatepayment.integration.model;

public class PaymentRequestObject {

    private String payeePaymentReference;
    private String callbackUrl;
    private String payeeAlias;
    private String amount;
    private String message;
    private String currency;

    public String getPayeePaymentReference() {
        return payeePaymentReference;
    }

    public void setPayeePaymentReference(String payeePaymentReference) {
        this.payeePaymentReference = payeePaymentReference;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getPayeeAlias() {
        return payeeAlias;
    }

    public void setPayeeAlias(String payeeAlias) {
        this.payeeAlias = payeeAlias;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
