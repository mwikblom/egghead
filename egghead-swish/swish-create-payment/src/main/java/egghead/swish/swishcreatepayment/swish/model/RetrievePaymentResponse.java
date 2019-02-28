package egghead.swish.swishcreatepayment.swish.model;

public class RetrievePaymentResponse {


    private String id;
    private String payeePaymentReference;
    private String paymentReference;
    private String callbackUrl;
    private String payerAlias;
    private String payeeAlias;
    private String amount;
    private String currency;
    private String message;
    private String status;
    private String dateCreated; // 2015-02-19T22:01:53+01:00",
    private String datePaid; //: "2015-02-19T22:03:53+01:00"

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPayeePaymentReference() {
        return payeePaymentReference;
    }

    public void setPayeePaymentReference(String payeePaymentReference) {
        this.payeePaymentReference = payeePaymentReference;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getPayerAlias() {
        return payerAlias;
    }

    public void setPayerAlias(String payerAlias) {
        this.payerAlias = payerAlias;
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getDatePaid() {
        return datePaid;
    }

    public void setDatePaid(String datePaid) {
        this.datePaid = datePaid;
    }

    @Override
    public String toString() {
        return "RetrievePaymentResponse{" +
            "id='" + id + '\'' +
            ", payeePaymentReference='" + payeePaymentReference + '\'' +
            ", paymentReference='" + paymentReference + '\'' +
            ", callbackUrl='" + callbackUrl + '\'' +
            ", payerAlias='" + payerAlias + '\'' +
            ", payeeAlias='" + payeeAlias + '\'' +
            ", amount='" + amount + '\'' +
            ", currency='" + currency + '\'' +
            ", message='" + message + '\'' +
            ", status='" + status + '\'' +
            ", dateCreated='" + dateCreated + '\'' +
            ", datePaid='" + datePaid + '\'' +
            '}';
    }
}
