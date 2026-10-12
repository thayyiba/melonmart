package com.melon.melonmart.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Order {

    private int id;
    private int userId;
    private BigDecimal totalAmount;
    private String status;
    private Timestamp createdAt;
    private String deliveryName;
    private String deliveryEmail;
    private String deliveryPhone;
    private String deliveryAddress;
    private String deliveryCity;
    private String deliveryState;
    private String deliveryPincode;
    private String deliveryLandmark;
    private String paymentMethod = "COD";

    public Order() {
    }

    public Order(int id, int userId, BigDecimal totalAmount,
                 String status, Timestamp createdAt) {
        this.id = id;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDeliveryName() { return deliveryName; }
    public void setDeliveryName(String deliveryName) { this.deliveryName = deliveryName; }

    public String getDeliveryEmail() { return deliveryEmail; }
    public void setDeliveryEmail(String deliveryEmail) { this.deliveryEmail = deliveryEmail; }

    public String getDeliveryPhone() { return deliveryPhone; }
    public void setDeliveryPhone(String deliveryPhone) { this.deliveryPhone = deliveryPhone; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getDeliveryCity() { return deliveryCity; }
    public void setDeliveryCity(String deliveryCity) { this.deliveryCity = deliveryCity; }

    public String getDeliveryState() { return deliveryState; }
    public void setDeliveryState(String deliveryState) { this.deliveryState = deliveryState; }

    public String getDeliveryPincode() { return deliveryPincode; }
    public void setDeliveryPincode(String deliveryPincode) { this.deliveryPincode = deliveryPincode; }

    public String getDeliveryLandmark() { return deliveryLandmark; }
    public void setDeliveryLandmark(String deliveryLandmark) { this.deliveryLandmark = deliveryLandmark; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}