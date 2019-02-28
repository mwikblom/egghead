package egghead.swish.swishcreatepayment.depositservice.model;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * This is the response from the deposit service providing data for the payment request
 *
 * @author mikael
 */
public class DepositOrderResponse {

    private String id;
    private String orderId;
    private BigDecimal amount;
    private Currency currency;
    private String messageOnStatement;
    private boolean isMobile;
    private String payerPhoneNumber;
    private String merchantSwishAlias;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public String getMessageOnStatement() {
        return messageOnStatement;
    }

    public void setMessageOnStatement(String messageOnStatement) {
        this.messageOnStatement = messageOnStatement;
    }

    public boolean isMobile() {
        return isMobile;
    }

    public void setMobile(boolean mobile) {
        isMobile = mobile;
    }

    public String getPayerPhoneNumber() {
        return payerPhoneNumber;
    }

    public void setPayerPhoneNumber(String payerPhoneNumber) {
        this.payerPhoneNumber = payerPhoneNumber;
    }

    public String getMerchantSwishAlias() {
        return merchantSwishAlias;
    }

    public void setMerchantSwishAlias(String merchantSwishAlias) {
        this.merchantSwishAlias = merchantSwishAlias;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DepositOrderResponse that = (DepositOrderResponse) o;
        return isMobile == that.isMobile &&
            Objects.equals(orderId, that.orderId) &&
            Objects.equals(amount, that.amount) &&
            Objects.equals(currency, that.currency) &&
            Objects.equals(messageOnStatement, that.messageOnStatement) &&
            Objects.equals(payerPhoneNumber, that.payerPhoneNumber) &&
            Objects.equals(merchantSwishAlias, that.merchantSwishAlias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, amount, currency, messageOnStatement, isMobile, payerPhoneNumber, merchantSwishAlias);
    }

    @Override
    public String toString() {
        return "DepositServiceResponse{" +
            "orderId='" + orderId + '\'' +
            ", amount=" + amount +
            ", currency=" + currency +
            ", messageOnStatement='" + messageOnStatement + '\'' +
            ", isMobile=" + isMobile +
            ", payerPhoneNumber='" + payerPhoneNumber + '\'' +
            ", merchantSwishAlias='" + merchantSwishAlias + '\'' +
            '}';
    }
}
