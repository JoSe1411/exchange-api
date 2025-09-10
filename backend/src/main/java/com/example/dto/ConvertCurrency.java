package com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConvertCurrency {
    private String baseCurrency;
    private String targetCurrency;
    private Double originalAmount;
    private Double convertedAmount;
    private String status;
    private Date date;
}
