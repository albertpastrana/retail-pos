# A customer wants to use loyalty

**Situation:** The customer brings her paper loyalty card and wants the current sale to count, or wants to redeem the loyalty benefit.

## Count the current receipt

The paper card is the source of truth for the customer's running balance. The till only calculates what the current receipt can earn; it does not store the accumulated balance.

The current receipt earns one stamp for each complete 10 euros of eligible spend:

- A total discount makes the receipt earn zero stamps.
- A sale percentage or line discount does not earn stamps for that line.
- Gift vouchers do not earn stamps.
- A negative line reduces eligible spend.

## Redeem the benefit

1. Check the customer's paper card.
2. When it has reached the redemption level, press the loyalty redemption action in the sales actions.
3. Check that the `-5 EUR` line appears on the receipt.
4. Record the redemption on the paper card.
5. Check the new total and stamp count before continuing with payment.

The `-5 EUR` line is a loyalty redemption. It is not a gift voucher and it is not a payment method.
