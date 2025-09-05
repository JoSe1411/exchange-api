package com.example.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import com.example.dto.CurrencyExchange;
import com.example.dto.ConvertCurrency;
import com.example.service.ConverterService;
import com.example.service.ExchangeService;

@RestController
@RequestMapping("/api/v1")
public class Controller {
    private final ExchangeService exchangeService;
    private final ConverterService converterService;

    public Controller(ExchangeService exchangeService,ConverterService converterService){
        this.exchangeService = exchangeService;
        this.converterService = converterService;
    }

    @GetMapping("/check")
    public String check(){
        return "All good.";
    }
    @GetMapping("/rates/{baseCurrency}/{targetCurrency}")
    public ResponseEntity<CurrencyExchange> exchangeRate(@PathVariable String baseCurrency, @PathVariable String targetCurrency) {
        try{
            CurrencyExchange currencyExchange = exchangeService.getExchangeRate(baseCurrency, targetCurrency);
            return ResponseEntity.ok(currencyExchange);
           
        } catch (RestClientException ex) {
            return ResponseEntity.status(502).build();
        }
    }

    @GetMapping("/convert")
    public ResponseEntity<ConvertCurrency> convertCurrency(@RequestParam String baseCurrency, @RequestParam String targetCurrency, @RequestParam Double amount){
        try{
            ConvertCurrency currencyExchange = converterService.getValue(baseCurrency,targetCurrency,amount);
        }
    }
}
