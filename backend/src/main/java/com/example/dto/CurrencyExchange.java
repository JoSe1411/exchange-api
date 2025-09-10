package com.example.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurrencyExchange {
    private String basecurrency;
    private String targetcurrency;
    private int fallbackLevel;
    private String fallbackReason; 
    private Double rate;
    private Date date;  
}


