package io.github.levasey.saga.ccps.web.controller;

import io.github.levasey.saga.core.dto.CreditCardProcessRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("ccp")
public class CreditCardProcessorController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreditCardProcessorController.class);

    @PostMapping("/process")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void processCreditCard(@RequestBody @Valid CreditCardProcessRequest request) {
        LOGGER.info("Processing request: {}", request);
        if (request.getCardNumber().toString().endsWith("0000")) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Card declined");
        }
    }
}
