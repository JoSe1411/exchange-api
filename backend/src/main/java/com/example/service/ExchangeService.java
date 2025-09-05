package com.example.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.example.dto.CurrencyExchange;
import com.fasterxml.jackson.databind.JsonNode;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class ExchangeService{
    private final  RestClient restClient = RestClient.create();
    public CurrencyExchange getExchangeRate(String baseCurrency, String targetCurrency){
        try{
            baseCurrency = baseCurrency.toLowerCase();
            targetCurrency = targetCurrency.toLowerCase();
            String urlTemplate = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/{baseCurrency}.json";
            JsonNode responseBody = restClient.get()
                                .uri(urlTemplate,baseCurrency)
                                .retrieve()
                                .body(JsonNode.class);
            
            JsonNode dateNode = responseBody.path("date");
            String dateStr = dateNode.isMissingNode() ? null : dateNode.asText();

            Date date = null;
            if (dateStr != null) {
                try {
                    date = new SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
                } catch (ParseException e) {
                    // Handle parse error, perhaps log and set to null
                }
            }

            JsonNode baseNode = responseBody.path(baseCurrency);
            double targetCurrencyRate = baseNode.path(targetCurrency).asDouble(0.0);

            CurrencyExchange currencyExchange = new CurrencyExchange();
            currencyExchange.setBasecurrency(baseCurrency);
            currencyExchange.setTargetcurrency(targetCurrency);
            currencyExchange.setDate(date);
            currencyExchange.setRate(targetCurrencyRate);
            currencyExchange.setInCache(false);  
            return currencyExchange;
        } catch (RestClientException e) {
            throw new RuntimeException("Failed to fetch exchange rate", e);
        }
    }
}