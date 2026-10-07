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

---

# AI HANDOFF — CURRENT BACKEND ARCHITECTURE & RECENT CHANGES

**Last Updated:** October 3, 2026

> This section is the current handoff for future ChatGPT/AI sessions. Treat the actual source code in the repository as
> the final authority if anything conflicts with this document. Do not invent files, endpoints, DTO fields, entity
> relationships, or business rules that are not present in the code.

## 1. PROJECT PURPOSE

NikyFarm & Nursery (NIKY) is an ecommerce platform for plants, nursery products, pots, gardening materials, seeds,
fertilizers, and related products.

Current architecture:

```text
Flutter Mobile/Web App
        |
        | HTTP / JSON / Multipart
        v
Java Spring Boot REST API
        |
        +-- Spring Security / JWT
        +-- Spring Data JPA / Hibernate
        +-- MySQL
        +-- Cloudinary (media)
        +-- Razorpay (payments)
```

The backend is the source of truth for product/catalog, cart, coupon, order, inventory, address, review, media, and
payment state.

---

# 2. DEVELOPMENT RULES FOR FUTURE AI SESSIONS

These rules are mandatory for future changes:

1. **DO NOT GUESS.** Inspect the current source files before changing code.
2. Existing source code is authoritative over this document.
3. Make the **smallest relevant change** required by the request.
4. Do not redesign APIs or entities unless the requirement actually needs it.
5. Do not change unrelated modules.
6. Preserve existing DTO/API contracts whenever possible.
7. Money values must use `BigDecimal`.
8. Product images use the existing `ProductImage` / variant relationship and `sortOrder`.
9. Do not use a database seeder as a workaround for runtime/business functionality unless explicitly requested.
10. Product image behavior must be implemented through the real image/product services or mapper, not by fake frontend
    data.
11. Preserve existing security rules and add authorization only where required.
12. Complete files are preferred when providing code to the user; do not provide disconnected fragments unless
    specifically requested.
13. If a required file is missing, ask for that file instead of inventing its contents.

---

# 3. CURRENT BACKEND MODULE STATUS

| Module                      | Status            | Notes                                                                                 |
|-----------------------------|-------------------|---------------------------------------------------------------------------------------|
| Authentication / JWT        | ✅ Completed      | Customer authentication and JWT flow exist.                                           |
| Users                       | ✅ Completed      | User/customer functionality exists.                                                   |
| Categories                  | ✅ Completed      | Category hierarchy/catalog support exists.                                            |
| Products                    | ✅ Completed      | Product CRUD and validation implemented.                                              |
| Product Variants            | ✅ Completed      | Variants, pricing, SKU, stock, active/default state.                                  |
| Product Images / Cloudinary | ✅ Completed      | Product and variant image support.                                                    |
| Product Attributes          | ✅ Completed      | Product attribute values exposed through API.                                         |
| Addresses                   | ✅ Completed      | Address ownership validation implemented.                                             |
| Cart                        | ✅ Completed      | Quantity, stock, active-state validation.                                             |
| Coupons                     | ✅ Completed      | Usage/concurrency protections implemented.                                            |
| Orders                      | ✅ Completed      | Status, ownership, cancellation, stock handling.                                      |
| Inventory                   | ✅ Completed      | Concurrency-safe stock operations.                                                    |
| Razorpay Payments           | ✅ Core Completed | Signature verification, webhook, retry/refund lifecycle.                              |
| Reviews                     | ✅ Implemented    | Public reviews, authenticated create/update/delete, eligibility.                      |
| Review Media                | ✅ Implemented    | Multiple images supported; video backend exists but Flutter video workflow is parked. |

---

# 4. PRODUCT + VARIANT MODEL

## ProductVariant

Current important fields:

```text
id
variantLabel
size
colour
price
 discountedPrice
stockQuantity
sku
isDefault
isActive
product
```

Important rules:

- `price` and `discountedPrice` use `BigDecimal`.
- SKU must be valid/unique.
- Variant belongs to a Product.
- A default variant can be identified using `isDefault`.
- Variant active state is validated by product/cart/order logic.

Typical entity file:

```text
entity/ProductVariant.java
```

---

# 5. PRODUCT IMAGE ARCHITECTURE

## ProductImage entity

Important fields:

```text
id
product
variant                 // nullable; null means product-level image
publicId
imageUrl
altText
sortOrder
isPrimary
```

The image can therefore belong to either:

```text
Product
  └── ProductImage (variant == null)
```

or:

```text
Product
  └── ProductVariant
        └── ProductImage (variant == that variant)
```

Important indexes exist for product and variant lookup and image ordering.

Typical entity file:

```text
entity/ProductImage.java
```

## ProductImageResponseDto

The existing response exposes the image's variant context and ordering information, including:

```text
id
productId
variantId
variantLabel
imageUrl
altText
sortOrder
isPrimary
```

The mapper is:

```text
mapper/ProductImageMapper.java
```

The current mapper maps the variant information and `sortOrder` directly from `ProductImage`. It does not invent or
recalculate image ordering. The current implementation confirms this mapping.

---

# 6. PRODUCT CARD IMAGE — IMPORTANT RECENT CHANGE

## Requirement

The Flutter ProductCard already consumes the product API's existing `primaryImageUrl` field.

We deliberately chose **not** to introduce another DTO field or redesign the Flutter API model.

The backend now resolves `ProductResponseDto.primaryImageUrl` from existing product/variant images.

## Current rule

```text
Product
   |
   +-- find default ProductVariant
   |
   +-- find ProductImages belonging to that variant
   |
   +-- sort by sortOrder ASC
   |
   +-- first image URL = primaryImageUrl
   |
   +-- if no default-variant image exists:
           use first product-level image by sortOrder
```

### Source of truth

**`sortOrder` is the source of truth for the display image.**

`isPrimary` is NOT used to choose the ProductCard image in the current mapper logic.

Example:

```text
Default Variant

Image A -> sortOrder 1
Image B -> sortOrder 2
Image C -> sortOrder 3

ProductResponseDto.primaryImageUrl = Image A URL
```

If the ordering changes:

```text
Image B -> sortOrder 1
Image A -> sortOrder 2
Image C -> sortOrder 3
```

then:

```text
primaryImageUrl = Image B URL
```

This avoids maintaining a second duplicated "default image URL" state.

## Current ProductMapper behavior

The current `ProductMapper` calls:

```java
String primaryImageUrl = resolveDisplayImageUrl(product);
```

and passes that value into the existing `ProductResponseDto` field.

The resolver:

1. Gets `product.getProductImages()`.
2. Finds the variant where `isDefault == true`.
3. Filters images belonging to that variant.
4. Sorts them by `sortOrder` ascending.
5. Selects the first non-empty `imageUrl`.
6. Falls back to product-level images (`variant == null`) ordered by `sortOrder`.

No new database column or API field is required.

### Important consequence

If `primaryImageUrl` is still `null`, inspect the actual database relationships first:

```text
ProductImage.product_variant_id
ProductVariant.is_default
ProductImage.sort_order
ProductImage.image_url
```

The mapper cannot create an image URL if no `ProductImage` row exists.

Do **not** modify the seeder just to solve this.

---

# 7. PRODUCT MAPPER FILE MAP

```text
backend/src/main/java/com/nikyfarm/mapper/ProductMapper.java
```

Responsibilities:

- Product -> ProductResponseDto
- ProductResponseDto -> ProductRequestDto
- Resolves effective `primaryImageUrl`
- Counts variants
- Maps category information

Recent image-resolution change is contained here.

Related mapper:

```text
backend/src/main/java/com/nikyfarm/mapper/ProductImageMapper.java
```

Responsibilities:

- ProductImage -> ProductImageResponseDto
- Exposes variant ID/label
- Exposes image URL
- Exposes `sortOrder`
- Exposes `isPrimary`

---

# 8. PRODUCT RESPONSE CONTRACT

Current `ProductResponseDto` remains structurally based on the existing product response contract and includes:

```text
id
name
slug
shortDescription
description
basePrice
discountedPrice
avgRating
reviewCount
isActive
categoryId
categoryName
primaryImageUrl
variantCount
createdAt
updatedAt
```

**Do not add `defaultVariantImageUrl` unless there is a future requirement for the API to expose both values
separately.**

For the current Flutter ProductCard, `primaryImageUrl` is sufficient.

---

# 9. PRODUCT ATTRIBUTES — RECENT BACKEND FEATURE

Product attribute values are exposed through:

```text
GET /api/products/{productId}/attribute-values
```

Response model:

```text
ProductAttributeValueResponseDto
```

Current response fields:

```text
id
productId
attributeId
attributeName
inputType
attributeOptionsId
attributeOptionValue
valueText
```

Important frontend interpretation:

- For option/select-style attributes, use `attributeOptionValue`.
- For text/other input types, use `valueText`.
- Do not invent units because the response does not contain a unit field.

Relevant backend files include:

```text
controller/ProductAttributeValueController.java       [verify exact filename]
dto/ProductAttributeValueResponseDto.java
entity/ProductAttributeValue.java                     [verify exact filename]
service/...                                            [verify current implementation]
```

If a future AI needs to modify this module, inspect the actual repository because the exact service/controller filenames
are authoritative.

---

# 10. REVIEW MODULE

Reviews are implemented with public reading and authenticated customer management.

## API contract

```text
GET  /api/products/{productId}/reviews
POST /api/products/{productId}/reviews
PUT  /api/reviews/{id}
DELETE /api/reviews/{id}
GET  /api/products/{productId}/reviews/eligibility
```

Create/update/delete operations use the authenticated user and owner/admin authorization rules.

The create endpoint uses the authenticated principal's user ID rather than accepting an arbitrary customer ID from the
client.

## ReviewResponseDto

Review response was expanded to include:

```text
List<ReviewMediaResponseDto> media
```

This allows one review to return multiple media records.

---

# 11. REVIEW MEDIA — MULTIPLE IMAGES

## ReviewMedia entity

Important fields:

```text
id
review
mediaType
mediaUrl
publicId
thumbnailUrl
sortOrder
```

The entity uses:

```text
review -> one-to-many -> ReviewMedia
```

and media is ordered by:

```text
sortOrder ASC, id ASC
```

`ReviewMediaType` currently supports:

```text
IMAGE
VIDEO
```

## Review endpoints

Multiple media upload:

```text
POST /api/reviews/{reviewId}/media
```

Delete media:

```text
DELETE /api/reviews/media/{mediaId}
```

Security requires authentication for these operations.

## Current limits

```text
Images per review: up to 5
Videos per review: up to 1
Image max size: 10 MB
Video max size: 50 MB
```

Supported image types:

```text
JPEG / JPG / PNG / WEBP
```

Supported video types:

```text
MP4 / MOV / WEBM
```

## Current development decision

Backend video upload works, but synchronous Cloudinary video upload was observed to take approximately 10–15 minutes in
development.

Therefore:

```text
Multiple image review upload -> ACTIVE
Video review workflow -> PARKED / NOT CURRENT FLUTTER PRIORITY
```

Do not spend time implementing Flutter video review functionality unless explicitly requested.

---

# 12. REVIEW ENTITY RELATIONSHIP

`Reviews` now contains a media collection in addition to the legacy single review image field.

Conceptually:

```text
Reviews
  |
  +-- legacy reviewImage
  |
  +-- media[]
        |
        +-- IMAGE
        +-- IMAGE
        +-- IMAGE
        +-- ...
```

The media relationship uses cascade/orphan removal and ordering by `sortOrder ASC, id ASC`.

Relevant files:

```text
entity/Reviews.java
entity/ReviewMedia.java
entity/ReviewMediaType.java
mapper/ReviewMapper.java                         [verify exact filename]
dto/ReviewResponseDto.java
dto/ReviewMediaResponseDto.java
controller/ReviewController.java                 [verify exact filename]
service/...                                      [verify exact implementation]
```

---

# 13. SECURITY — REVIEW MEDIA

Required authorization rules added for review media operations:

```text
POST   /api/reviews/*/media        -> authenticated
DELETE /api/reviews/media/*        -> authenticated
```

Ownership/admin checks remain part of the review media service logic.

Do not make review media endpoints public merely because image URLs themselves are publicly accessible.

---

# 14. PAYMENT / RAZORPAY STATUS

The payment module remains based on the completed architecture described in the original project document.

Payment entity stores provider-related identifiers and lifecycle information including:

```text
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

Payment lifecycle:

```text
CREATED
   ↓
PENDING
   ↓
CAPTURED
   ↓
SUCCESS
```

Failure path:

```text
PENDING → FAILED
```

Refund path:

```text
CAPTURED / SUCCESS
        ↓
REFUNDED
```

Backend verification is authoritative. Flutter success callbacks are not treated as final payment confirmation.

Webhook processing includes signature verification, event handling, duplicate safety, and reconciliation.

---

# 15. CHECKOUT / ORDER / INVENTORY ARCHITECTURE

Current checkout model:

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

Inventory is not permanently deducted merely because an item is placed in the cart.

Stock is validated again during checkout/payment processing.

Inventory operations are centralized and concurrency-safe.

Cancellation protects against:

```text
already cancelled
shipped
 delivered
multiple stock restoration
```

---

# 16. ADDRESS ARCHITECTURE

Address ownership is validated against the authenticated customer.

Checkout/order code must not trust arbitrary address IDs supplied by a client.

Current project decision:

```text
Separate shipping and billing addresses are supported.
```

However, the frontend MVP does not require two different addresses for every checkout. A single address can be used
where the current checkout contract permits it.

Future production improvement:

```text
Order Address Snapshot
```

should preserve the exact address used at order creation so later customer edits cannot alter historical order
information.

---

# 17. COUPON ARCHITECTURE

Coupon processing includes concurrency protection around usage limits and `timesUsed`.

Important requirement:

```text
Do not validate usage limit and increment timesUsed as two unrelated unsafe operations.
```

Existing locking/atomic approach must be preserved when changing coupon logic.

---

# 18. MONEY / PRICING RULES

All product, variant, payment, refund, and other monetary values must use:

```java
BigDecimal
```

Pricing validation uses `BigDecimal.compareTo()`.

Do not introduce:

```java
float
        Double
```

for monetary business logic.

---

# 19. BACKEND FILE MAP — HIGH LEVEL

Use this map as the starting point when navigating the repository.

```text
backend/
└── src/main/java/com/nikyfarm/
    │
    ├── controller/
    │   ├── AuthController.java
    │   ├── CategoryController.java
    │   ├── ProductController.java
    │   ├── ProductImageController.java             [verify exact name]
    │   ├── ProductAttribute...Controller.java      [verify exact name]
    │   ├── CartController.java
    │   ├── CouponController.java
    │   ├── OrderController.java
    │   ├── PaymentController.java
    │   ├── ReviewController.java
    │   └── ...Admin...Controller.java
    │
    ├── dto/
    │   ├── ProductRequestDto.java
    │   ├── ProductResponseDto.java
    │   ├── ProductVariantRequestDto.java
    │   ├── ProductVariantResponseDto.java
    │   ├── ProductImageResponseDto.java
    │   ├── ProductAttributeValueResponseDto.java
    │   ├── ReviewResponseDto.java
    │   ├── ReviewMediaResponseDto.java
    │   └── ...
    │
    ├── entity/
    │   ├── Product.java
    │   ├── ProductVariant.java
    │   ├── ProductImage.java
    │   ├── Category.java
    │   ├── Reviews.java
    │   ├── ReviewMedia.java
    │   ├── ReviewMediaType.java
    │   ├── Payment.java
    │   ├── Order.java
    │   ├── OrderItem.java
    │   ├── Cart.java
    │   ├── CartItem.java
    │   ├── Coupon.java
    │   ├── Address.java
    │   └── ...
    │
    ├── mapper/
    │   ├── ProductMapper.java                       <-- RECENTLY CHANGED
    │   ├── ProductImageMapper.java                  <-- CONFIRMED
    │   └── ...
    │
    ├── repository/
    │   ├── ProductRepository.java
    │   ├── ProductVariantRepository.java
    │   ├── ProductImageRepository.java
    │   ├── PaymentRepository.java
    │   └── ...
    │
    ├── service/
    │   ├── ProductService.java
    │   ├── ProductServiceImpl.java
    │   ├── ProductImageService.java                 [verify exact name]
    │   ├── ProductImageServiceImpl.java             [verify exact name]
    │   ├── PaymentService.java
    │   ├── PaymentServiceImpl.java
    │   ├── InventoryService.java
    │   ├── OrderService.java
    │   ├── CartService.java
    │   ├── CouponService.java
    │   ├── ReviewService.java
    │   └── ...
    │
    ├── security/
    │   ├── SecurityConfig.java
    │   ├── Jwt...java
    │   └── ...
    │
    └── exception/
        └── ...
```

**Important:** Names marked `[verify exact name]` are intentionally not asserted as exact filenames. Future AI should
inspect the repository rather than creating duplicate classes.

---

# 20. PRODUCT IMAGE DATA FLOW

The complete current flow is:

```text
Admin / Image Upload
        ↓
ProductImage
        |
        +-- product_id
        +-- product_variant_id (optional)
        +-- image_url
        +-- sort_order
        +-- is_primary
        +-- public_id
        ↓
Product entity relationship
        ↓
ProductMapper.toResponseDto()
        ↓
resolveDisplayImageUrl()
        ↓
Default Variant
        ↓
Variant Images sorted by sortOrder ASC
        ↓
First Image URL
        ↓
ProductResponseDto.primaryImageUrl
        ↓
GET /api/products
        ↓
Flutter ProductCard
```

This is intentionally implemented without introducing a new API field.

---

# 21. PRODUCT DETAIL IMAGE RULE

Product detail has a different requirement from the ProductCard.

For product detail:

```text
Selected Variant
      ↓
Variant-specific images
      ↓
sortOrder ASC
```

If the selected variant has no variant-specific images:

```text
Product-level images
```

are used as fallback.

When the selected variant changes, the frontend resets the selected gallery image to index 0.

This is separate from the ProductCard's effective `primaryImageUrl` calculation.

---

# 22. CURRENT BACKEND DECISIONS — DO NOT REVERSE WITHOUT A REQUIREMENT

### Product image

```text
sortOrder = source of truth for first/display image
```

### ProductCard API

```text
Use existing primaryImageUrl field
```

### Product DTO

```text
Do not add another image URL field unless required
```

### Seeder

```text
Do not use the seeder to repair runtime image behavior
```

### Money

```text
BigDecimal only
```

### Payment confirmation

```text
Backend verification/webhook is authoritative
```

### Inventory

```text
Cart does not permanently deduct stock
```

### Reviews

```text
Multiple review images supported
Video is parked for now
```

---

# 23. CURRENT KNOWN LIMITATIONS / FUTURE WORK

The following remain future or production-hardening tasks unless already implemented elsewhere in the repository:

```text
SEC-01  Final security audit
SEC-02  Externalize/rotate secrets
DB-01   Production database migrations / Flyway
TEST-01 Critical integration/concurrency tests
USER-01 Password reset
ORDER-07 Shipping policy
ADDR-02 Order address snapshot
API-01 Error response consistency
API-06 OpenAPI / Swagger documentation
PAY-12 Payment event history
Inventory movement history
Audit logging
Cloudinary reconciliation
API versioning
```

Do not implement these automatically when working on an unrelated feature.

---

# 24. HOW A FUTURE AI SHOULD START A NEW TASK

When given a new backend task, follow this process:

```text
1. Read this project.md
        ↓
2. Identify the affected module
        ↓
3. Locate the actual current entity
        ↓
4. Locate DTO/request/response
        ↓
5. Locate controller
        ↓
6. Locate service + implementation
        ↓
7. Locate repository
        ↓
8. Inspect security rules if endpoint-related
        ↓
9. Make the smallest required change
        ↓
10. Check compile/API compatibility
        ↓
11. Report exactly which files changed and why
```

Never jump directly to writing a new class when an existing service/controller already handles the required behavior.

---

# 25. CURRENT SESSION HANDOFF — PRODUCT IMAGE ISSUE

The latest issue being solved was:

```text
Flutter ProductCard showed no image.
```

Investigation established:

```text
ProductResponseDto.primaryImageUrl = null
```

The reason is that the product image URL must come from actual `ProductImage` records. The ProductCard itself was
already consuming the existing image field correctly.

The agreed solution was **not** to modify the seeder.

Instead, the backend mapper now derives `primaryImageUrl` from the existing image relationships:

```text
default variant
    ↓
variant images
    ↓
sortOrder ASC
    ↓
first image URL
```

with product-level image fallback.

If the API still returns `null` after this mapper change, inspect the actual database rows and image-upload flow.
Specifically verify:

```text
ProductImage.product_id
ProductImage.product_variant_id
ProductImage.image_url
ProductImage.sort_order
ProductVariant.is_default
```

Do not immediately change Flutter code.

---

# 26. IMPORTANT FILES FOR THE NEXT SESSION

For any future product/image task, start by inspecting:

```text
Product.java
ProductVariant.java
ProductImage.java
ProductResponseDto.java
ProductImageResponseDto.java
ProductMapper.java
ProductImageMapper.java
ProductController.java
ProductImageController.java
ProductService.java
ProductServiceImpl.java
ProductImageService.java
ProductImageServiceImpl.java
ProductRepository.java
ProductImageRepository.java
ProductVariantRepository.java
SecurityConfig.java
```

If any of these names differ in the repository, use the actual existing file instead of creating a duplicate.

For reviews, inspect:

```text
Reviews.java
ReviewMedia.java
ReviewMediaType.java
ReviewResponseDto.java
ReviewMediaResponseDto.java
ReviewController.java
ReviewService.java
ReviewMedia repository/service classes
SecurityConfig.java
```

For payments, inspect:

```text
Payment.java
PaymentRepository.java
PaymentService.java
PaymentServiceImpl.java
PaymentController.java
Razorpay configuration classes
SecurityConfig.java
Order.java
InventoryService.java
```

---

# 27. SOURCE OF TRUTH PRIORITY

When information conflicts, use this priority:

```text
1. Actual current source code
2. Actual database/schema behavior
3. Current API responses
4. This project.md
5. Older project documentation / historical notes
6. AI assumptions — NEVER use assumptions when source code can be inspected
```

This document is a handoff/context document, not a replacement for inspecting the repository.

---

# END OF CURRENT AI HANDOFF
