package com.example.service;
import java.util.Date;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.example.dto.CurrencyExchange;
import com.example.dto.ConvertCurrency;
import com.example.service.ExchangeService;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


@Service
public class ConverterService {
    public final ExchangeService exchangeService;
    public final Logger logger = LoggerFactory.getLogger(ConverterService.class);
    public ConverterService(ExchangeService exchangeService){
        this.exchangeService=exchangeService;
    }

    public ConvertCurrency getValue(String baseCurrency, String targetCurrency , Double amount){
        
        try{
        CurrencyExchange currencyExchange = exchangeService.getExchangeRate(targetCurrency,baseCurrency);
        Double exchangeRate = currencyExchange.getRate();
        Double convertedValue = amount*exchangeRate;
        Date date = currencyExchange.getDate();
        ConvertCurrency currencyExchangeII = new ConvertCurrency();
        currencyExchangeII.setBaseCurrency(baseCurrency);
        currencyExchangeII.setTargetCurrency(targetCurrency);
        currencyExchangeII.setDate(date);
        currencyExchangeII.setOriginalAmount(amount);
        currencyExchangeII.setConvertedAmount(convertedValue);
        currencyExchangeII.setStatus("SUCCESS");
        return currencyExchangeII;
        } catch(Exception e){
            logger.error("Currency conversion failed: {}", e.getMessage(), e);
            throw new RuntimeException("Unable to perform currency conversion: " + e.getMessage(), e);
        }

    } 
    
}
