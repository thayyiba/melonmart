package com.melon.melonmart.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

public class SellerOrderDTO {

    private int orderId;
    private int buyerId;
    private String buyerName;
    private String buyerEmail;
    private BigDecimal orderTotal;
    private String status;
    private Timestamp createdAt;
    private List<SellerOrderItemDTO> items;

    public SellerOrderDTO() {
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(int buyerId) {
        this.buyerId = buyerId;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getBuyerEmail() {
        return buyerEmail;
    }

    public void setBuyerEmail(String buyerEmail) {
        this.buyerEmail = buyerEmail;
    }

    public BigDecimal getOrderTotal() {
        return orderTotal;
    }

    public void setOrderTotal(BigDecimal orderTotal) {
        this.orderTotal = orderTotal;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public List<SellerOrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<SellerOrderItemDTO> items) {
        this.items = items;
    }
}