# NikyFarm Project — Current Completion Status

**Last Updated:** September 13, 2026

This section reflects the actual implementation status after the backend integrity, checkout and Razorpay payment work
completed during the current development cycle.

The older audit sections below remain useful as a historical issue register. However, completed issues listed in this
section should be considered resolved unless a new regression is discovered.

---

# Current Development Status

## Core Backend

| Module                      | Status                           |
|-----------------------------|----------------------------------|
| Authentication & JWT        | ✅ Completed                     |
| Users                       | ✅ Completed                     |
| Categories                  | ✅ Completed                     |
| Products                    | ✅ Completed                     |
| Product Variants            | ✅ Completed                     |
| Product Images / Cloudinary | ✅ Completed                     |
| Product Attributes          | ✅ Completed                     |
| Addresses                   | ✅ Completed                     |
| Cart                        | ✅ Completed                     |
| Coupons                     | ✅ Completed                     |
| Orders                      | ✅ Completed                     |
| Inventory                   | ✅ Completed                     |
| Razorpay Payments           | ✅ Core Implementation Completed |

---

# Completed Critical Issues

## MONEY-01 — Money Foundation

**Status: ✅ COMPLETED**

All product and product variant monetary values were migrated to `BigDecimal`.

Completed work includes:

* Product price migration to `BigDecimal`
* Product discounted price migration to `BigDecimal`
* ProductVariant price migration to `BigDecimal`
* ProductVariant discounted price migration to `BigDecimal`
* DTO monetary validation using decimal validation
* Service-level pricing validation using `BigDecimal.compareTo()`
* Mapper compatibility
* Order pricing compatibility

Example pricing validation:

```java
if(discountedPrice !=null
        &&price !=null
        &&discountedPrice.

compareTo(price) >0){

        throw new

BusinessRuleViolationException(
      "Discounted price cannot be greater than price");
}
```

---

# PROD-01 — Product & Variant Data Integrity

**Status: ✅ COMPLETED**

Completed work includes:

* Product pricing validation
* Variant pricing validation
* SKU validation
* Product and variant relationship validation
* Product active-state validation
* Variant active-state validation
* Default variant consistency
* Product/variant data integrity improvements

---

# INV-01 — Inventory Concurrency

**Status: ✅ COMPLETED**

Inventory stock updates now use a concurrency-safe approach.

The previous unsafe pattern:

```text
Read Stock
    ↓
Check Stock
    ↓
Deduct Stock
```

was replaced with atomic repository-level stock updates.

The inventory service now centralizes:

```text
deductStock()
restoreStock()
```

This reduces the risk of overselling during concurrent checkout operations.

---

# CART-01 — Cart Integrity

**Status: ✅ COMPLETED**

Cart operations now validate:

* Quantity must be greater than zero
* Requested quantity against live stock
* Product active state
* ProductVariant active state
* ProductVariant availability

Important design decision:

The cart does NOT permanently deduct inventory.

Stock is validated again during checkout/payment processing.

---

# ADDR-01 — Address Ownership

**Status: ✅ COMPLETED**

Checkout validates that:

```text
Shipping Address
        AND
Billing Address
```

belong to the authenticated customer.

Address IDs are no longer trusted without ownership validation.

---

# COUPON-01 — Coupon Concurrency

**Status: ✅ COMPLETED**

Coupon processing now includes concurrency protection.

Completed work includes:

* Safe coupon validation
* Usage limit protection
* Atomic usage handling
* Preventing concurrent overuse
* Safe `timesUsed` updates

---

# ORDER MODULE

## ORDER-01 — Address Ownership

**Status: ✅ COMPLETED**

Shipping and billing addresses are validated against the authenticated user.

---

## ORDER-02 — Order Status Transitions

**Status: ✅ COMPLETED**

Order status transitions are controlled.

The lifecycle follows:

```text
PENDING
    ↓
PROCESSING
    ↓
SHIPPED
    ↓
DELIVERED
```

Invalid reverse or terminal transitions are rejected.

---

## ORDER-03 — Cancellation & Stock

**Status: ✅ COMPLETED**

Cancellation now protects against:

* Cancelling an already cancelled order
* Cancelling shipped orders
* Cancelling delivered orders
* Multiple stock restoration

Stock restoration uses centralized inventory logic.

---

## ORDER-04 — Inventory Concurrency

**Status: ✅ COMPLETED**

Order/payment inventory processing uses the centralized inventory service and concurrency-safe stock operations.

---

# CHECKOUT-02 — Payment-Aware Checkout

**Status: ✅ COMPLETED**

The checkout lifecycle was redesigned around payment confirmation.

Current flow:

```text
Cart
    ↓
Validate Checkout
    ↓
Create Order
    ↓
Create Payment Attempt
    ↓
Customer Pays
    ↓
Verify Payment / Webhook
    ↓
Payment CAPTURED
    ↓
Inventory Processing
    ↓
Payment SUCCESS
    ↓
Clear Cart
```

Failure states are handled separately to avoid incorrectly marking an order as fully successful.

---

# PAYMENT & RAZORPAY MODULE

## Payment Entity

**Status: ✅ COMPLETED**

The payment entity now stores:

```text
id

order

provider

providerOrderId

providerPaymentId

providerRefundId

paymentMethod

amount
refundAmount

currency

status

failureCode
failureReason

paidAt
refundedAt

createdAt
updatedAt
```

Money fields use:

```java
BigDecimal
```

---

## Payment Status Lifecycle

Current payment states:

```text
CREATED
    ↓
PENDING
    ↓
CAPTURED
    ↓
SUCCESS
```

Alternative lifecycle:

```text
PENDING
    ↓
FAILED
```

Refund lifecycle:

```text
CAPTURED / SUCCESS
        ↓
REFUNDED
```

---

## PAY-01 — Payment Model

**Status: ✅ COMPLETED**

Payment entity redesigned for provider integration.

---

## PAY-02 — BigDecimal Money Handling

**Status: ✅ COMPLETED**

Payment amounts and refund amounts use `BigDecimal`.

---

## PAY-03 — Provider IDs

**Status: ✅ COMPLETED**

The following identifiers are stored:

```text
providerOrderId
providerPaymentId
providerRefundId
```

---

## PAY-04 — Backend Signature Verification

**Status: ✅ COMPLETED**

Flutter success callbacks are not trusted as the final payment confirmation source.

Razorpay payment signatures are verified by the backend.

---

## PAY-05 — Razorpay Webhook

**Status: ✅ COMPLETED**

Webhook processing includes:

* Raw payload processing
* Razorpay webhook signature verification
* Payment event handling
* Duplicate event safety
* Payment/order reconciliation

The webhook endpoint is not dependent on customer JWT authentication.

---

## PAY-06 — Idempotency

**Status: ✅ COMPLETED**

Payment processing protects against duplicate processing from:

```text
Flutter Verification
        +
Webhook
        +
Retry
```

Database locking and payment state checks prevent repeated lifecycle execution.

---

## PAY-07 — Payment Retry Lifecycle

**Status: ✅ COMPLETED**

Failed/interrupted payment attempts can follow the defined retry lifecycle without incorrectly duplicating completed
payment processing.

---

## PAY-08 — Failure Information

**Status: ✅ COMPLETED**

Payments can store:

```text
failureCode
failureReason
```

This supports:

* Support debugging
* Retry decisions
* Payment failure analysis

---

## PAY-09 — Currency

**Status: ✅ COMPLETED**

Currency is explicitly stored in payment records.

Example:

```text
INR
```

---

## PAY-10 — Refund Lifecycle

**Status: ✅ COMPLETED**

Full Razorpay refund support is implemented.

Current lifecycle:

```text
CAPTURED / SUCCESS
        ↓
Validate Refund Eligibility
        ↓
Lock Payment
        ↓
Call Razorpay Refund API
        ↓
Store providerRefundId
        ↓
Store refundAmount
        ↓
Store refundedAt
        ↓
Payment → REFUNDED
        ↓
Order PaymentStatus → REFUNDED
```

Duplicate refund requests are handled safely.

---

## PAY-11 — Webhook Security

**Status: ✅ COMPLETED**

Webhook security is configured independently from normal customer JWT authentication.

Only the required verified provider webhook endpoint should be publicly reachable.

Other API endpoints remain protected according to their authorization rules.

---

## PAY-12 — Payment Event History

**Status: 🟡 LATER**

Potential future improvement:

```text
Payment
    ↓
PaymentEvent History
```

Possible events:

```text
PAYMENT_CREATED
PAYMENT_CAPTURED
PAYMENT_SUCCESS
PAYMENT_FAILED
WEBHOOK_RECEIVED
REFUND_REQUESTED
REFUND_COMPLETED
```

This is not required for the current Flutter MVP.

---

# CURRENT PAYMENT MODULE STATUS

```text
PAY-01   ✅
PAY-02   ✅
PAY-03   ✅
PAY-04   ✅
PAY-05   ✅
PAY-06   ✅
PAY-07   ✅
PAY-08   ✅
PAY-09   ✅
PAY-10   ✅
PAY-11   ✅
PAY-12   🟡 Later Improvement
```

Core payment implementation is complete.

Remaining payment work is primarily integration testing with Flutter and Razorpay.

---

# PENDING BEFORE PRODUCTION

## 🔴 P0 / Critical

### SEC-01 — Final Security Audit

Verify:

* Admin authorization
* Customer authorization
* JWT filters
* Public catalog access
* Webhook access
* Payment endpoint ownership
* Order ownership
* Secrets exposure

---

### SEC-02 — Externalize Secrets

Move all secrets to environment variables:

```text
Database Password
JWT Secret
Cloudinary Secret
Razorpay Key Secret
Razorpay Webhook Secret
```

Rotate any secrets that were previously committed to source control.

---

### DB-01 — Database Migrations

Introduce database migrations.

Recommended tool:

```text
Flyway
```

Avoid relying on:

```text
spring.jpa.hibernate.ddl-auto=update
```

for production schema management.

---

### TEST-01 — Critical Flow Testing

Create tests for:

```text
Authentication
Cart
Coupon
Inventory
Order
Payment
Webhook
Refund
```

Critical scenarios include:

* Concurrent stock deduction
* Concurrent coupon usage
* Duplicate payment verification
* Duplicate webhook
* Payment retry
* Inventory failure after payment capture
* Refund retry
* Order cancellation

---

# IMPORTANT PENDING WORK

## USER-01 — Password Reset

Implement:

```text
Forgot Password
    ↓
Secure Token / OTP
    ↓
Password Reset
```

---

## ORDER-07 — Shipping Policy

Currently:

```text
shippingFee = 0
```

Define:

* Free shipping rules
* Local delivery
* Pickup
* Minimum order value
* Delivery fee

---

## ADDR-02 — Order Address Snapshot

Consider storing order-time address snapshots.

Example:

```text
shippingName
shippingPhone
shippingAddressLine1
shippingAddressLine2
shippingCity
shippingState
shippingPincode
```

This prevents historical order data from changing when customers edit their address.

---

## API-01 — Error Handling

Return proper HTTP responses.

Example:

```text
Unsupported Method
    ↓
405 Method Not Allowed
```

instead of a generic:

```text
500 Internal Server Error
```

---

## API-06 — API Documentation

Update:

```text
Postman Collection
```

for:

* Attributes
* Orders
* Coupons
* Payments
* Webhooks

Later add:

```text
Swagger / OpenAPI
```

if required.

---

# FUTURE IMPROVEMENTS

## P2

* Payment event history
* Inventory stock movement history
* Audit logs
* Cloudinary reconciliation
* API versioning
* OpenAPI documentation
* Pagination improvements
* Sort field allow-listing

---

## P3

* Admin analytics dashboard
* Revenue charts
* Top products
* Customer analytics
* Advanced inventory reporting
* Advanced coupon analytics

---

# NEXT DEVELOPMENT PHASE

The backend core is now sufficiently complete to begin Flutter development.

Recommended Flutter implementation order:

```text
1. Authentication
        ↓
2. Product Catalog
        ↓
3. Product Details
        ↓
4. Cart
        ↓
5. Addresses
        ↓
6. Checkout
        ↓
7. Razorpay Payment
        ↓
8. Order History
        ↓
9. Profile
        ↓
10. Wishlist
```

---

# REAL PAYMENT TESTING CHECKLIST

When the Flutter checkout UI is ready, test:

```text
[ ] Create Razorpay Order

[ ] Successful Payment

[ ] Backend Signature Verification

[ ] Failed Payment

[ ] Payment Retry

[ ] Duplicate Verification Request

[ ] Duplicate Webhook

[ ] Webhook + API Verification

[ ] Inventory Failure After Capture

[ ] Successful Refund

[ ] Duplicate Refund Request

[ ] Payment Status Reconciliation
```

---

# CURRENT RECOMMENDATION

## Start Flutter Development

The current backend core is complete enough for frontend integration.

Do not delay Flutter development for PAY-12, analytics, audit logs or advanced administration features.

Complete real payment testing during Flutter checkout implementation, then perform the final production audit before
deployment.
