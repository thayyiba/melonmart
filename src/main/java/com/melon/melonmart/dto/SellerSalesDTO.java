package com.melon.melonmart.dto;

import java.math.BigDecimal;

public class SellerSalesDTO {

    private BigDecimal totalRevenue;
    private int totalOrders;
    private int productsSold;
    private BigDecimal averageOrderValue;

    public SellerSalesDTO() {
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(int totalOrders) {
        this.totalOrders = totalOrders;
    }

    public int getProductsSold() {
        return productsSold;
    }

    public void setProductsSold(int productsSold) {
        this.productsSold = productsSold;
    }

    public BigDecimal getAverageOrderValue() {
        return averageOrderValue;
    }

    public void setAverageOrderValue(
            BigDecimal averageOrderValue
    ) {
        this.averageOrderValue = averageOrderValue;
    }
}