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
    private double value;
    private Date date;
}
