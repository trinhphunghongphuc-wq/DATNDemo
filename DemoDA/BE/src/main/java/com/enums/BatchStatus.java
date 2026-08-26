package com.enums;

public enum BatchStatus {
    CREATED,
    IN_PRODUCTION,
    ASSIGNED_TO_DISTRIBUTOR,   //Dành cho nghiệp vụ của Distributor
    RECEIVED_BY_DISTRIBUTOR,   //Dành cho nghiệp vụ của Distributor
    IN_DISTRIBUTION,           //Dành cho nghiệp vụ của Distributor
    DELIVERED_TO_RETAILER,  //Dành cho nghiệp vụ của Retailer
    DELIVERY_REJECTED,      //Dành cho nghiệp vụ của Retailer
    AT_RETAIL,              //Dành cho nghiệp vụ của Retailer
    SOLD_OUT
}