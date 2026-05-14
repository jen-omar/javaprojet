package com.marketplace.services;

import com.marketplace.models.Product;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import java.math.BigDecimal;

public class StripeService {

        public StripeService() {
                		Stripe.apiKey = "YOUR_STRIPE_SECRET_KEY"; // REPLACE WITH YOUR ACTUAL STRIPE SECRET KEY
        }

        public String createCheckoutSession(Product p, String successUrl, String cancelUrl) throws Exception {
                long amountInCents = p.getPrice().multiply(new BigDecimal("100")).longValue();

                SessionCreateParams params = SessionCreateParams.builder()
                                .setMode(SessionCreateParams.Mode.PAYMENT)
                                .setSuccessUrl(successUrl)
                                .setCancelUrl(cancelUrl)
                                .addLineItem(SessionCreateParams.LineItem.builder()
                                                .setQuantity(1L)
                                                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                                                .setCurrency("eur")
                                                                .setUnitAmount(amountInCents)
                                                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData
                                                                                .builder()
                                                                                .setName(p.getName())
                                                                                .build())
                                                                .build())
                                                .build())
                                .build();

                Session session = Session.create(params);
                return session.getUrl();
        }
}
