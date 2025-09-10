package com.example.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;


import com.example.dto.CurrencyExchange;
import com.fasterxml.jackson.databind.JsonNode;
import com.example.config.RedisConfig;

import java.text.ParseException;
import java.util.zip.DataFormatException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ExchangeService{
    // Redis - 
    private final  RestClient restClient = RestClient.create();
    private final RedisTemplate<String,CurrencyExchange> redisTemplate;
    private final Logger logger = LoggerFactory.getLogger(ExchangeService.class);
    public ExchangeService(RedisTemplate<String,CurrencyExchange> redisTemplate){
        this.redisTemplate = redisTemplate;
    }
    private void saveToCache(CurrencyExchange data, String baseCurrency, String targetCurrency) {
        String cacheKey = "exchange:" + baseCurrency.toUpperCase() + ":" + targetCurrency.toUpperCase();
        long ttlSeconds = getTtlForCurrencyPair(baseCurrency, targetCurrency);
        
        try {
            redisTemplate.opsForValue().set(cacheKey, data, ttlSeconds, TimeUnit.SECONDS);
        }catch (Exception e) {
            logger.warn("Failed to write currency exchange data to Redis cache. " +
                       "Key: {}, Base: {}, Target: {}, Operation will continue without cache. " +
                       "Error: {}", 
                       cacheKey, baseCurrency.toUpperCase(), targetCurrency.toUpperCase(), e.getMessage(), e);
        }
        
    }
    // Smart TTL logic
    private long getTtlForCurrencyPair(String base, String target) {
        Set<String> majorCurrencies = Set.of("USD", "EUR", "GBP", "JPY", "CAD", "AUD");
        
        if (majorCurrencies.contains(base.toUpperCase()) && 
            majorCurrencies.contains(target.toUpperCase())) {
            return 4 * 60 * 60; // 4 hours for major pairs
        }
        
        return 2 * 60 * 60; // 2 hours for others
    }
    
    // Response Validation - 
    public boolean responseValidation(JsonNode responseBody, String baseCurrency, String targetCurrency){
        baseCurrency = baseCurrency.toLowerCase();
        targetCurrency = targetCurrency.toLowerCase(); 
        if(responseBody == null || responseBody.isEmpty()){ 
            logger.warn("Resonse is empty or null.");
            return false;
        }
        String date = responseBody.get("date").asText();
        if(date==null || date.isEmpty()){
            logger.warn("Date is missing.");
            return false;
        }
        JsonNode currencyList = responseBody.path(baseCurrency);
        if(currencyList == null || currencyList.isEmpty()){
            logger.warn("No Rates for {} found."+baseCurrency.toUpperCase());
            return false;
        }
        if(currencyList.path(targetCurrency).isMissingNode() == true ){
            logger.warn("Target currency {} not found."+targetCurrency.toUpperCase());
            return false;
        }
        return true;

    }
    public CurrencyExchange getExchangeRate(String baseCurrency, String targetCurrency){
        try{
            baseCurrency = baseCurrency.toLowerCase();
            targetCurrency = targetCurrency.toLowerCase();
            
            ArrayList<String> URLs = new ArrayList<>(Arrays.asList(
                "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/{baseCurrency}.json",
                "https://latest.currency-api.pages.dev/v1/currencies/{baseCurrency}.json"
            ));
            int lengthOfURLs = URLs.size();
            JsonNode responseBody = null;
            Boolean success = false;
            for(int i =0 ; i<lengthOfURLs ; i++){
               try{
                responseBody = restClient.get()
                                .uri(URLs.get(i),baseCurrency)
                                .retrieve()
                                .body(JsonNode.class);
                if(responseValidation(responseBody, baseCurrency, targetCurrency) == false){
                    logger.warn("CDN {} returned invalid data, trying next CDN", i+1);
                    continue;
                }
                else{
                    logger.debug("Successfully Retrieved data from CDN {}",i+1);
                    success = true;
                    break;
                }
               } catch(RestClientException e){
                    logger.warn("CDN {} failed. Trying remaining CDNs.",i+1,e.getMessage(),e);
               }
            
            }
            if(success==false){
                throw new RestClientException("All CDNs failed.");
            }

            JsonNode dateNode = responseBody.path("date");
            String dateStr = dateNode.isMissingNode() ? null : dateNode.asText();

            Date date = null;
            if (dateStr != null) {
                try {
                    date = new SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
                } catch (ParseException e) {
                    logger.error("Invalid Date Format.",e);
                }
            }
            if(responseValidation(responseBody, baseCurrency, targetCurrency) == false){
                throw new DataFormatException("Data Format received is invalid");
            }
            JsonNode baseNode = responseBody.path(baseCurrency);
            double targetCurrencyRate = baseNode.path(targetCurrency).asDouble(0.0);
            CurrencyExchange currencyExchange = new CurrencyExchange();
            currencyExchange.setFallbackLevel(0);
            currencyExchange.setFallbackReason("Primary API success.");
            currencyExchange.setBasecurrency(baseCurrency);
            currencyExchange.setTargetcurrency(targetCurrency);
            currencyExchange.setDate(date);
            currencyExchange.setRate(targetCurrencyRate);
            saveToCache(currencyExchange, baseCurrency, targetCurrency);
            return currencyExchange;
        } catch (RestClientException e) {
            logger.warn("Primary API failed. Using Fallback.",e);
            return getExchangeRateFromFallBackAlternativeAPI(baseCurrency, targetCurrency, e );
        } catch(Exception e){
            logger.error("Unexpected error in primary API: {}", e.getMessage());
            return createErrorResponse(baseCurrency, targetCurrency, e);
        }

    }
    private CurrencyExchange getFromCache(String baseCurrency , String targetCurrency){
        try{
        String cacheKey = "exchange:" + baseCurrency.toUpperCase() + ":" + targetCurrency.toUpperCase();
        CurrencyExchange currencyExchange = redisTemplate.opsForValue().get(cacheKey);
        if(currencyExchange != null){
            logger.debug("Cache hit for {}/{}", baseCurrency, targetCurrency);
        }
        return currencyExchange;
        } catch (Exception e) {
            // Handle all Redis exceptions with proper logging
            logger.warn("⚠️ Redis operation failed for {}/{}: {}",
                       baseCurrency, targetCurrency, e.getMessage());
            return null;
        }


    }
    private CurrencyExchange getExchangeRateFromFallBackAlternativeAPI(String baseCurrency, String targetCurrency, Exception primaryError){
        try{
            baseCurrency = baseCurrency.toUpperCase();
            targetCurrency = targetCurrency.toUpperCase();
            String API_KEY = System.getenv("EXCHANGE_RATE_API_KEY");
            String urlTemplate = "https://v6.exchangerate-api.com/v6/{API_KEY}/latest/{baseCurrency}";
            JsonNode response = restClient.get()
                                .uri(urlTemplate,API_KEY,baseCurrency)
                                .retrieve()
                                .body(JsonNode.class);
            JsonNode rates = response.get("conversion_rates");
            Double targetCurrencyRate = rates.get(targetCurrency).asDouble();

            String dateStr = response.get("time_last_update_utc").asText();
            Date date = null;
            if (dateStr != null) {
                try {
                    date = new SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
                } catch (ParseException e) {
                    logger.error("Invalid Date Format.",e);
                }
            }
            CurrencyExchange currencyExchange = new CurrencyExchange();
            currencyExchange.setFallbackLevel(1);
            currencyExchange.setFallbackReason("Fallback API used.");
            currencyExchange.setBasecurrency(baseCurrency);
            currencyExchange.setTargetcurrency(targetCurrency);
            currencyExchange.setDate(date);
            currencyExchange.setRate(targetCurrencyRate);
            saveToCache(currencyExchange, baseCurrency, targetCurrency);
            return currencyExchange; 
        }catch(RestClientException e){
            logger.warn("Secondary Fallback failed. Using Cache.",e);
            return getExchangeRateFromCache(baseCurrency, targetCurrency, e);
        }
    }
    public CurrencyExchange getExchangeRateFromCache(String baseCurrency, String targetCurrency, Exception e){
        return getFromCache(baseCurrency, targetCurrency);
    }
    private CurrencyExchange createErrorResponse(String base, String target, Exception e) {
        CurrencyExchange error = new CurrencyExchange();
        error.setFallbackLevel(3);
        error.setFallbackReason("All sources failed: " + e.getMessage());
        error.setBasecurrency(base);
        error.setTargetcurrency(target);
        error.setRate(0.0);
        return error;
    }
}

