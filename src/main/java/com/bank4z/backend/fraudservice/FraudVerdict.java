package com.bank4z.backend.fraudservice;

/**
 * What the fraud engine decided about one transfer attempt. "reason" is for
 * admins and logs only; it can contain thresholds and counts, so it is never
 * sent back to the customer.
 */
public record FraudVerdict(
        FraudFlag.Rule rule,
        FraudFlag.RiskLevel riskLevel,
        FraudFlag.Action action,
        String reason
) {}