# Nikyfarm — E-Commerce Backend

Reference documentation for the codebase. Last updated: 2026-09-09 (Coupon & Order modules updated; critical production hardening plan added before Payments).

## Overview

Spring Boot REST backend for an e-commerce platform (`com.niky:nikyfarm`). Covers the
product catalog (categories, products, variants, attributes, images), plus accounts,
reviews, wishlist, cart and orders. Category, Product, ProductVariant, Attribute,
AttributeOptions, ProductAttributeValue, User, Reviews, ReviewImage, Wishlist and
Cart/CartItem now all have full service/controller/DTO layers (see "Catalog Module",
"Attributes (EAV) Module" and "Accounts, Reviews, Wishlist & Cart Module" below), secured
by real JWT authentication (see "Security"). Orders and Coupons now have complete service, DTO, mapper and management flows. Orders support customer checkout, cancellation and admin status management with inventory handling and financial calculations; Coupons support lifecycle management, validation and checkout discount calculation. Addresses have a complete REST controller, service and DTO layer with customer ownership checks and default-address management. A separate client
(a Flutter app, per the CORS origin in `SecurityConfig`) consumes the `/api/**` side of
this backend; the Thymeleaf admin panel is this repo's own internal tool.

## Tech Stack

- **Java** 17
- **Spring Boot** 4.1.1 (parent), starters: `data-jpa`, `security`, `webmvc`, `validation`
- **MySQL** via `mysql-connector-j` (database: `nikyfarm_db`)
- **Lombok** for boilerplate (`@Getter/@Setter/@NoArgsConstructor`, `@Data` on DTOs)
- **Cloudinary** (`cloudinary-core`, `cloudinary-http5` 2.3.0) — image hosting for category/product images
- **Thymeleaf** (`spring-boot-starter-thymeleaf`) — server-rendered admin panel (see "Admin Panel" below); templates under `src/main/resources/templates/`, styled with Bootstrap 5 via CDN
- **auth0 java-jwt** 4.2.1 — now wired up: `security/JwtService` issues/verifies tokens (see "Security")
- **Spring Security Crypto** (`BCryptPasswordEncoder`) — password hashing for `User.passwordHash`
- **dotenv-java** 3.0.0 — present, **not yet used**; credentials are still plaintext in `application.properties`
- **commons-io** 2.22.0

## Configuration (`application.properties`)

- `spring.datasource.url/username/password` — MySQL connection to `nikyfarm_db`
- `spring.jpa.hibernate.ddl-auto=update` — schema auto-updates on startup; **no Flyway/Liquibase migrations**
- `cloudinary.cloud-name/api-key/api-secret` — consumed by `CloudinaryConfig`

- `spring.servlet.multipart.max-file-size` / `max-request-size` — both set to `10MB` to
  match the cap `CloudinaryServiceImpl` already enforces in code (Spring Boot's own
  default is 1MB, which would otherwise silently reject anything above that before it
  ever reached the upload validation).
- `jwt.secret` / `jwt.expiration-ms` — HMAC256 signing key (plaintext, same caveat as
  below) and token lifetime (24h) for `security/JwtService`.
- `app.admin.email` / `app.admin.password` — used once, by `config/AdminSeeder`, to
  create the first `ADMIN` user on startup if none exists yet. Change the password after
  first login; this is a dev-only bootstrap convenience, not a real credential rotation
  story.

Not configured: `server.port` (defaults 8080), logging levels, profile-specific files.

⚠️ **Known issue**: real DB credentials and Cloudinary keys are committed in plaintext.
`dotenv-java` is a dependency but not actually used to externalize them — worth fixing.

## Package Structure

```
com.nikyfarm
├── NikyfarmApplication.java        Spring Boot entry point
├── config/
│   ├── SecurityConfig.java         JWT-secured filter chain for /api/**, permitAll for /admin/** (see "Security")
│   ├── CloudinaryConfig.java       Cloudinary SDK client bean
│   ├── WebConfig.java              WebMvcConfigurer — declarative view routes (e.g. "/" → redirect "/admin")
│   └── AdminSeeder.java            CommandLineRunner — creates the first ADMIN user on startup if none exists
├── security/
│   ├── JwtService.java             issues/verifies HMAC256 JWTs (auth0 java-jwt)
│   ├── JwtAuthenticationFilter.java  OncePerRequestFilter — Bearer token → SecurityContext (principal = user id)
│   ├── JsonAuthenticationEntryPoint.java  401s from the filter chain, as ErrorResponse JSON
│   └── JsonAccessDeniedHandler.java       403s from the filter chain, as ErrorResponse JSON
├── controller/
│   ├── CategoryController.java     /api/categories/** — CRUD + image upload
│   ├── ProductController.java      /api/products/** — CRUD + pagination
│   ├── ProductVariantController.java /api/products/{productId}/variants/** — CRUD
│   ├── ProductImageController.java /api/products/{productId}/images/** — image CRUD
│   ├── AttributeController.java    /api/attributes/** — CRUD, optional ?categoryId filter
│   ├── AttributeOptionController.java /api/attributes/{attributeId}/options/** — CRUD
│   ├── ProductAttributeValueController.java /api/products/{productId}/attribute-values/** — CRUD
│   ├── AuthController.java         /api/auth/** — register/login, public
│   ├── UserController.java         /api/users/** — self-service "me" + admin user management
│   ├── ReviewController.java       /api/products/{productId}/reviews + /api/reviews/{id} — no class-level @RequestMapping (mixed base paths)
│   ├── ReviewImageController.java  /api/review-images/** — upload/moderate
│   ├── WishlistController.java     /api/me/wishlist/** — current user's own wishlist
│   ├── CartController.java         /api/me/cart/** — current user's own cart (Cart aggregate, CartItem lines nested under it)
│   ├── AddressController.java      /api/addresses/** — customer addresses + default address
│   ├── OrderController.java        /api/orders/** — checkout, buy-now, order history + cancellation
│   ├── CouponController.java       /api/coupons/** — public checkout coupon validation
│   └── admin/                      server-rendered Thymeleaf admin UI (see "Admin Panel" below) — every
│       │                          controller here delegates to the same service used by the REST API
│       ├── AdminHomeController.java, AdminCategoryController.java
│       ├── AdminProductController.java, AdminProductVariantController.java
│       ├── AdminProductImageController.java (general/non-variant images only)
│       ├── AdminVariantImageController.java (one variant's own images)
│       ├── AdminAttributeController.java
│       ├── AdminAttributeOptionController.java, AdminProductAttributeValueController.java
│       ├── AdminUserController.java, AdminReviewController.java
│       ├── AdminWishlistController.java, AdminCartItemController.java
│       ├── AdminAddressController.java
│       ├── AdminCouponController.java — /admin/coupons/** coupon management
│       ├── AdminOrderController.java — /admin/orders/** order management + status updates
├── dto/
│   ├── CategoryRequestDto.java / CategoryResponseDto.java
│   ├── ProductRequestDto.java / ProductResponseDto.java
│   ├── ProductVariantRequestDto.java / ProductVariantResponseDto.java
│   ├── AttributeRequestDto.java / AttributeResponseDto.java
│   ├── AttributeOptionRequestDto.java / AttributeOptionResponseDto.java
│   ├── ProductAttributeValueRequestDto.java / ProductAttributeValueResponseDto.java
│   ├── ProductImageResponseDto.java
│   ├── RegisterRequestDto.java / LoginRequestDto.java / AuthResponseDto.java
│   ├── UserResponseDto.java / UserSelfUpdateRequestDto.java / UserUpdateRequestDto.java
│   ├── ReviewRequestDto.java / ReviewUpdateRequestDto.java / ReviewResponseDto.java
│   ├── ReviewImageResponseDto.java
│   ├── WishlistRequestDto.java / WishlistResponseDto.java
│   ├── CartResponseDto.java, CartItemRequestDto.java / CartItemResponseDto.java,
│   │   CartItemQuantityUpdateDto.java, CartItemAdminViewDto.java
│   ├── AddressRequestDto.java / AddressResponseDto.java
│   ├── OrderRequestDto.java / OrderResponseDto.java / OrderItemRequestDto.java / OrderItemResponseDto.java
│   ├── CouponRequestDto.java / CouponResponseDto.java
│   ├── CouponValidationRequestDto.java / CouponValidationResponseDto.java
│   ├── ImageUploadResponse.java, ImageUploadResult.java, BulkImageUploadResponse.java
├── mapper/
│   ├── CategoryMapper.java, ProductMapper.java, ProductVariantMapper.java   entity <-> response DTO, static utility classes
│   ├── AttributeMapper.java, AttributeOptionMapper.java, ProductAttributeValueMapper.java
│   ├── ProductImageMapper.java, CouponMapper.java
│   ├── UserMapper.java, ReviewMapper.java, ReviewImageMapper.java, WishlistMapper.java,
│   │   CartMapper.java, CartItemMapper.java, AddressMapper.java, OrderMapper.java, OrderItemMapper.java
├── entity/                         JPA entities (see below)
├── exception/
│   ├── ResourceNotFoundException.java    → 404
│   ├── DuplicateResourceException.java   → 409 (slug/SKU/name/option-value/email already taken)
│   ├── BusinessRuleViolationException.java → 409 (invariant violated, e.g. category still has products)
│   ├── ImageUploadException.java         → 400
│   ├── InvalidCredentialsException.java  → 401 (bad login, or a deactivated account)
│   ├── ErrorResponse.java                shared error payload record
│   └── GlobalExceptionHandler.java       @RestControllerAdvice — single source of truth for error responses, logs unexpected (500) exceptions
├── repository/                     Spring Data JPA repositories
└── service/
    ├── CategoryService.java (+Impl), ProductService.java (+Impl), ProductVariantService.java (+Impl)
    ├── AttributeService.java (+Impl), AttributeOptionService.java (+Impl), ProductAttributeValueService.java (+Impl)
    ├── ProductImageService.java (+Impl)   extracted from ProductImageController so REST + admin UI share one implementation
    ├── AuthService.java (+Impl), UserService.java (+Impl)
    ├── ReviewService.java (+Impl), ReviewImageService.java (+Impl)
    ├── WishlistService.java (+Impl), CartService.java (+Impl), AddressesService.java (+Impl)
    ├── OrderService.java (+Impl) — checkout, buy-now, customer history, cancellation and admin status management
    ├── CouponService.java (+Impl) — coupon CRUD, validation, status and usage management
    └── CloudinaryService.java (+Impl)
```

## Domain Model (Entities)

| Entity | Table | Notable Fields | Relationships |
|---|---|---|---|
| `Product` | products | name, slug, basePrice, discountedPrice, avgRating, reviewCount, isActive | `@ManyToOne Category`; `@OneToMany ProductImage` (cascade ALL); `@OneToMany ProductVariant` (cascade ALL) — deleting a product cascades to its images/variants in the DB |
| `ProductVariant` | product_variants | variantLabel, size, colour, price, stockQuantity, sku, isDefault | `@ManyToOne Product` |
| `Category` | categories | name, slug, imageUrl, imagePublicId (Cloudinary asset id, for delete/replace), sortOrder, isActive | self-referential `parent` / `children` (cascade ALL) |
| `ProductImage` | product_images | publicId, imageUrl, altText, sortOrder, isPrimary | `@ManyToOne Product`; `@ManyToOne ProductVariant` (nullable) |
| `Attribute` | attributes | name, unit, inputType (enum `TEXT/NUMBER/BOOLEAN/SELECT`), sortOrder | `@ManyToOne Category`; `@OneToMany AttributeOptions` (cascade ALL) — deleting an attribute cascades to its own options |
| `AttributeOptions` | attribute_options | value | `@ManyToOne Attribute` |
| `ProductAttributeValue` | product_attribute_value | valueText | `@ManyToOne Product/Attribute/AttributeOptions`; **not** mapped as a collection on `Product` — `ProductService.deleteProduct` must delete these rows explicitly before deleting the product, or the FK constraint blocks the delete |
| `User` | users | name, email, passwordHash, role (enum `CUSTOMER/ADMIN`), isActive | none |
| `Addresses` | addresses | fullName, phone, line1, line2, city, state, postalCode, isDefault, createdAt | `@ManyToOne User`; **PK is `int`, inconsistent with the service/API layer (`Long`)** |
| `Wishlist` | wishlist_items | createdAt | `@ManyToOne User`; `@ManyToOne Product` |
| `Cart` | carts | createdAt, updatedAt | `@OneToOne User` (unique FK — one cart per user); `@OneToMany CartItem` (cascade ALL, orphanRemoval) |
| `CartItem` | cart_items | quantity, addedAt | `@ManyToOne Cart`; `@ManyToOne ProductVariant` — unique constraint on `(cart_id, product_variant_id)` so re-adding a variant already in the cart bumps quantity instead of inserting a duplicate row |
| `Reviews` | reviews | rating, title, comment, isVerifiedPurchase | `@ManyToOne Product/User/ReviewImage` |
| `ReviewImage` | review_images | imageUrl, publicId (added so it can actually be deleted from Cloudinary) | none |
| `Coupon` | coupons | unique `code` (indexed), description, isActive, discountType (enum `PERCENTAGE/FLAT`), `discountValue` (`BigDecimal`), `minOrderValue` (`BigDecimal`), timesUsed, maxUses, validFrom, validUntil | embedded domain logic: `isValid(orderSubtotal)`, `calculateDiscount(orderSubtotal)`, `incrementTimesUsed()`; indexed code lookup |
| `Order` | orders | orderNumber, lifecycle status (enum `PENDING/PROCESSING/SHIPPED/DELIVERED/CANCELLED`), paymentStatus (enum `UNPAID/PAID/REFUNDED/FAILED`), `subtotal`, `discount`, `shippingFee`, `totalAmount` (`BigDecimal`) | `@ManyToOne Coupon/User`; billing/shipping address references retained with order metadata |
| `OrderItems` | order_items | `productNameSnapshot`, `variantLabelSnapshot`, `unitPrice` (`BigDecimal`), quantity, `lineTotal` (`BigDecimal`) | `@ManyToOne Order/Product/ProductVariant`; historical item snapshots preserve order-time product details |
| `Payments` | payments | provider, transactionId, amount, status (enum `INITIATED/SUCCESS/FAILED/REFUNDED`) | `@ManyToOne Order` |

Order lifecycle and payment states are modeled explicitly to support controlled status transitions and checkout/cancellation workflows. Order financial values use `BigDecimal` for currency precision, and order items retain product/variant snapshots so historical orders are not affected by later catalog changes. `Addresses.id` remains `int` while much of the service/API layer uses `Long`.

**Cart was refactored from a flat `CartItems` entity to a `Cart`/`CartItem` aggregate**
(a `Cart` per user owning its `CartItem` lines) — the standard e-commerce shape, and a
prerequisite for things like per-cart totals or a future "abandoned cart" job that a flat
`CartItems -> User` table doesn't support cleanly. `CartItem` deliberately does not
snapshot unit price (unlike `OrderItems`, which does) — a cart should always reflect the
variant's live price/stock; snapshotting belongs at checkout time. `ddl-auto=update` does
not migrate old `cart_items` rows (`user_id` FK) onto the new schema (`cart_id` FK) — see
Known Gaps.

## REST Endpoints

### CategoryController — `/api/categories`
| Method | Path | Purpose |
|---|---|---|
| POST | `` (multipart: `request` + optional `file`) | Create category (validated `CategoryRequestDto`), optional Cloudinary image upload, optional `parentId` for hierarchy |
| PUT | `/{id}` (multipart: `request` + optional `file`) | Update category fields; if `file` present, replaces the Cloudinary image (tracked via `imagePublicId`) |
| GET | `/{id}` | Get category by id → `CategoryResponseDto` |
| GET | `` | List all categories |
| DELETE | `/{id}` | Deletes the category + its Cloudinary image. Blocked (409) if it has subcategories or products still assigned |

### ProductController — `/api/products`
| Method | Path | Purpose |
|---|---|---|
| POST | `` (JSON body) | Create product (validated `ProductRequestDto`, requires existing `categoryId`) |
| PUT | `/{id}` (JSON body) | Update product |
| GET | `/{id}` | Get product by id → `ProductResponseDto` (includes `primaryImageUrl` derived from its images and `variantCount`) |
| GET | `` | Paginated list — query params `categoryId` (optional), `page`, `size`, `sortBy`, `direction` |
| DELETE | `/{id}` | Deletes product; best-effort deletes its Cloudinary images first, then cascades to `product_images`/`product_variants` rows |

### ProductVariantController — `/api/products/{productId}/variants`
| Method | Path | Purpose |
|---|---|---|
| POST | `` (JSON body) | Create a variant under a product (validated `ProductVariantRequestDto`, unique `sku`) |
| PUT | `/{variantId}` (JSON body) | Update a variant |
| GET | `/{variantId}` | Get one variant (must belong to `productId`) |
| GET | `` | List all variants for a product |
| DELETE | `/{variantId}` | Deletes the variant's images from Cloudinary/DB, then the variant |

Setting `isDefault: true` on a variant automatically clears the default flag on the
product's other variants (`ProductVariantRepository.clearDefaultForProductExcept`).

### ProductImageController — `/api/products/{productId}/images`
| Method | Path | Purpose |
|---|---|---|
| GET | `` | List every image for a product (general + variant-scoped) → `ProductImageResponseDto[]` |
| POST | `` (multipart `file`) | Upload one product-level image |
| POST | `/bulk` (multipart `files`) | Bulk upload product images (max 10/batch) |
| POST | `/variant/{variantId}` | Upload one image for a specific variant |
| POST | `/variant/{variantId}/bulk` | Bulk upload for a variant |
| PUT | `/{imageId}` | Replace an existing image's file (keeps DB row/order/primary flag) |
| DELETE | `/{imageId}` | Delete image from Cloudinary + DB |

All of the above now delegate to `ProductImageService`/`ProductImageServiceImpl` (extracted
from what used to be inline controller logic) — the admin UI's image screen calls the same
service directly.

### AttributeController — `/api/attributes`
| Method | Path | Purpose |
|---|---|---|
| POST | `` (JSON body) | Create an attribute (validated `AttributeRequestDto`; `name` unique per `categoryId`) |
| PUT | `/{id}` (JSON body) | Update an attribute |
| GET | `/{id}` | Get one attribute → `AttributeResponseDto` (includes `optionCount`) |
| GET | `` | List attributes; optional `?categoryId=` filter |
| DELETE | `/{id}` | Deletes the attribute + its options (cascade). Blocked (409) if any product has a value set for it |

### AttributeOptionController — `/api/attributes/{attributeId}/options`
| Method | Path | Purpose |
|---|---|---|
| POST | `` (JSON body) | Add an option (validated `AttributeOptionRequestDto`; `value` unique per attribute, case-insensitive) |
| PUT | `/{optionId}` (JSON body) | Update an option's value |
| GET | `/{optionId}` | Get one option (must belong to `attributeId`) |
| GET | `` | List all options for an attribute |
| DELETE | `/{optionId}` | Delete an option. Blocked (409) if any product's attribute value references it |

### ProductAttributeValueController — `/api/products/{productId}/attribute-values`
| Method | Path | Purpose |
|---|---|---|
| POST | `` (JSON body) | Assign a value to a product for a given `attributeId` (one value per product+attribute) |
| PUT | `/{valueId}` (JSON body) | Update a value |
| GET | `/{valueId}` | Get one value (must belong to `productId`) |
| GET | `` | List all attribute values for a product |
| DELETE | `/{valueId}` | Delete a value |

The shape of the request (`ProductAttributeValueRequestDto`: `attributeId`, `attributeOptionsId`,
`valueText`) is validated against the target attribute's `inputType` in the service, not
via bean validation (it's cross-field/contextual): `SELECT` requires `attributeOptionsId`
pointing at an option that belongs to that attribute; every other type requires a
non-blank `valueText` (`NUMBER` must parse as a double, `BOOLEAN` must be `true`/`false`
case-insensitive) and forbids `attributeOptionsId`. Assigning also checks the attribute's
`categoryId` matches the product's category (skipped if the attribute has no category).

### AuthController — `/api/auth` (public)
| Method | Path | Purpose |
|---|---|---|
| POST | `/register` | Create a `CUSTOMER` account (validated `RegisterRequestDto`; email must be unique) → `AuthResponseDto` with a JWT |
| POST | `/login` | Validate email/password (BCrypt) → `AuthResponseDto`. 401 for wrong password, unknown email, or a deactivated account — same message for all three, so a caller can't tell which |

### UserController — `/api/users`
| Method | Path | Purpose |
|---|---|---|
| GET | `/me` | Your own profile (authenticated) |
| PUT | `/me` | Update your own name/email — `UserSelfUpdateRequestDto` has no `role`/`isActive` fields at all, so self-escalation is structurally impossible, not just service-checked |
| GET | `` / `/{id}` | List/get any user (admin) |
| PUT | `/{id}` | Full update including `role`/`isActive` (admin) — `UserUpdateRequestDto` |
| DELETE | `/{id}` | Delete a user (admin) |

### ReviewController — mixed base paths, no class-level `@RequestMapping`
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/products/{productId}/reviews` | Public — all reviews for a product |
| POST | `/api/products/{productId}/reviews` | Authenticated — create a review as yourself (one per user per product; `isVerifiedPurchase` always starts `false`, there's no Order-tracking service to check against yet) |
| PUT | `/api/reviews/{id}` | Owner or admin (`ReviewUpdateRequestDto` — no `productId`, a review can't be reassigned; `isVerifiedPurchase` in the body only has effect for an admin caller) |
| DELETE | `/api/reviews/{id}` | Owner or admin. A non-owner, non-admin caller gets 404, not 403 — doesn't confirm a review id they don't own exists |

### ReviewImageController — `/api/review-images`
| Method | Path | Purpose |
|---|---|---|
| POST | `` (multipart `file`) | Authenticated — upload one image (Cloudinary folder `reviews`), then reference the returned id in a review's `reviewImageId` |
| GET | `/{id}` | Public (so the URL can be rendered) |
| GET | `` | Admin — every uploaded review image (moderation) |
| DELETE | `/{id}` | Admin — blocked (409) if a review still references it |

### WishlistController — `/api/me/wishlist` (all authenticated, all scoped to the caller)
| Method | Path | Purpose |
|---|---|---|
| GET | `` | Your wishlist |
| POST | `` | Add a product (`WishlistRequestDto`; 409 if already there) |
| DELETE | `/{id}` | Remove an item — 404 if it isn't yours |

### CartController — `/api/me/cart` (all authenticated, all scoped to the caller)
| Method | Path | Purpose |
|---|---|---|
| GET | `` | Your cart → `CartResponseDto` (items + computed `totalItemCount`/`totalPrice`). Returns an empty, unpersisted cart view rather than 404 if you've never added anything |
| POST | `/items` | Add a variant+quantity (`CartItemRequestDto`) — creates your `Cart` row on first use, merges into an existing line for that variant instead of duplicating (unique `(cart_id, product_variant_id)`) |
| PUT | `/items/{itemId}` | Set an absolute quantity (`CartItemQuantityUpdateDto` body) — 404 if the item isn't yours |
| DELETE | `/items/{itemId}` | Remove one line item — 404 if it isn't yours |
| DELETE | `` | Clear every item from your cart |

Stock is not hard-enforced on add/update (no 409) — instead `CartItemResponseDto.stockInsufficient`
flags a line whose `quantity` exceeds the variant's current `stockQuantity`, so the client
can warn before checkout without blocking the cart itself from going over momentarily
(e.g. someone else buying the last units after it was added).

### AddressController — `/api/addresses`
| Method | Path | Purpose |
|---|---|---|
| POST | `` | Create an address for the authenticated customer; request `userId` is overwritten from JWT for non-admin callers |
| PUT | `/{id}` | Update an address; ownership checked |
| GET | `/{id}` | Get one address; owner/admin access |
| GET | `/me` | List the authenticated customer's addresses |
| GET | `/me/default` | Get the authenticated customer's default address |
| GET | `/user/{userId}` | List a user's addresses (admin only) |
| PUT | `/{id}/default` | Make an address the default and clear the user's previous default |
| DELETE | `/{id}` | Delete an address; ownership checked |

`AddressRequestDto` validates full name, 10-digit phone number, address fields and a 5–6 digit postal code. Only one default address is maintained per user: creating/updating an address with `isDefault=true`, or explicitly calling the default endpoint, clears the existing default first. Ownership failures return 404 to avoid exposing another user's address id.

### CouponController — `/api/coupons`
| Method | Path | Purpose |
|---|---|---|
| POST | `/validate` | Public checkout coupon verification using `CouponValidationRequestDto` (`code`, `orderSubtotal`) → returns validity, calculated discount amount and validation message |

**Coupon validation** checks normalized code lookup, activation status, validity window, maximum usage bounds and minimum order subtotal before returning a `CouponValidationResponseDto`. Discount calculation is delegated to the `Coupon` domain model so percentage and flat discounts share one authoritative implementation.

### OrderController — `/api/orders` (authenticated customer / shared)
| Method | Path | Purpose |
|---|---|---|
| POST | `/checkout` | Place an order from the authenticated user's active shopping cart; converts cart items into order items and clears the cart after successful checkout |
| POST | `/buy-now` | Place a direct order with custom item quantities; validates live inventory, decrements stock and applies coupon discount calculation when applicable |
| GET | `/me` | Retrieve the authenticated customer's paginated order history |
| GET | `/{id}` | Fetch specific order details with strict ownership verification (or authorized shared access where applicable) |
| POST | `/{id}/cancel` | Cancel an active, unshipped order and automatically restore inventory for active product variants |

**Checkout and inventory rules**: `createOrderFromCart` converts the current cart into an order using order-time product and variant snapshots, calculates precise financial totals and clears the cart only after successful order creation. `createDirectOrder` performs real-time stock validation before decrementing variant inventory and calculates applicable coupon discounts. Cancellation is lifecycle-aware: only eligible active, unshipped orders can be cancelled, and cancellation automatically restores stock for active product variants.


All not-found/validation/business-rule errors across every controller are funneled
through `GlobalExceptionHandler` (`@RestControllerAdvice`), which returns a consistent
`ErrorResponse` JSON body (`timestamp`, `status`, `error`, `message`, `path`, optional
`fieldErrors`). Per-controller `@ExceptionHandler`s have been removed in favor of this.

| Exception | HTTP Status |
|---|---|
| `ResourceNotFoundException` | 404 |
| `DuplicateResourceException` | 409 |
| `BusinessRuleViolationException` | 409 |
| `ImageUploadException` | 400 |
| `InvalidCredentialsException` | 401 |
| `MethodArgumentNotValidException` (bean validation) | 400, with `fieldErrors` map |
| `MissingServletRequestPartException` / `HttpMessageNotReadableException` | 400 |
| anything else | 500 |

## Services

Category, Product and ProductVariant each follow interface + `*Impl` (`@Service`,
constructor injection via Lombok `@RequiredArgsConstructor`), matching the existing
`CloudinaryService` pattern. Entity ↔ response-DTO mapping is done by static mapper
classes in `mapper/` rather than inline in the services.

- **CategoryService** — CRUD; validates slug uniqueness; resolves optional `parentId`
  (rejects self-parenting and circular hierarchies by walking the ancestor chain);
  uploads/replaces/deletes the Cloudinary image, tracking `imagePublicId` on the entity
  so replace/delete target the correct asset; delete is blocked (`BusinessRuleViolationException`)
  if the category has subcategories or products still pointing at it.
- **ProductService** — CRUD with pagination (`Page<ProductResponseDto>`, optional
  `categoryId` filter); validates slug uniqueness and that `discountedPrice <= basePrice`;
  resolves and validates `categoryId`; delete best-effort removes all of the product's
  Cloudinary images before deleting the row (DB cascade handles `product_images`/`product_variants`).
- **ProductVariantService** — CRUD scoped under a product; validates SKU uniqueness and
  `discountedPrice <= price`; enforces "only one default variant per product" by clearing
  the flag on siblings when a variant is marked default; delete removes the variant's
  Cloudinary images first (their `product_variant_id` FK would otherwise block the delete).
- **CloudinaryService / CloudinaryServiceImpl** — `uploadImage`, `uploadImages` (bulk, max 10, partial-failure tolerant), `deleteImage`, `replaceImage` (delete-then-upload, tolerates old-asset delete failure). Validates content type (jpeg/jpg/png/webp) and size (≤10MB).
- **AuthService** — `register` (email uniqueness, BCrypt hash, always `CUSTOMER`), `login`
  (BCrypt compare, checks `isActive`). Both return `AuthResponseDto` via a shared
  `buildAuthResponse` that calls `JwtService.generateToken`.
- **UserService** — `getById`/`getAll`, `updateSelf` (name/email only), `updateByAdmin`
  (name/email/role/isActive), `deleteUser`. Email uniqueness re-checked on every update.
- **ReviewService** — `createReview` (one per user per product, `isVerifiedPurchase`
  always starts false), `updateReview`/`deleteReview` (owner-or-admin via an explicit
  `currentUserId`+`isAdmin` pair passed down from the controller, not Spring method
  security), `getReviewsByProduct`/`getAllReviews`, `removeImage` (detaches then deletes
  the linked `ReviewImage` — needed because the update DTO's "null `reviewImageId`" means
  "leave unchanged", so there's no other way to explicitly clear it).
- **ReviewImageService** — thin wrapper around `CloudinaryService` (folder `"reviews"`)
  plus the delete guard (`ReviewsRepository.existsByReviewImageId`).
- **WishlistService** — `add`/`getMine`/`remove` (customer-facing, ownership-checked) plus
  `getAll(userId)`/`removeByAdmin` (admin, no ownership check) for the admin screen.
- **CartService** — `getCart` (never 404s for a user with no cart yet — returns an empty
  view); `addItem` (lazily creates the `Cart` row on first use via `UserRepository`,
  merges into the existing `CartItem` for that variant instead of duplicating);
  `updateItemQuantity`/`removeItem` (ownership-checked via `findByIdAndCartId`);
  `clearCart`; plus `listAllItems(userId)`/`deleteItemAsAdmin` for the admin screen
  (no ownership check, matches every other admin path in this module).
- **AddressesService** — CRUD with owner/admin access checks; customer requests are scoped to the JWT principal, while admins can query another user's addresses. Supports listing addresses, retrieving the default address and setting exactly one default address per user. Default switching clears the previous default inside a transaction.
- **CouponService** — `createCoupon`, `updateCoupon`, `getCouponById`, `getCouponByCode`, `getAllCoupons`, `toggleCouponStatus` and `deleteCoupon`; normalizes codes through `CouponMapper` and applies dynamic validation for activation, validity dates, maximum usage and minimum subtotal thresholds. `CouponRequestDto` provides form validation with constraints such as `@NotBlank`, `@DecimalMin` and `@DateTimeFormat`; `CouponResponseDto` exposes formatted UI data, usage statistics and validation status.
- **OrderService** — customer checkout through `createOrderFromCart` and `createDirectOrder`, customer history/detail retrieval with ownership verification, lifecycle-aware cancellation with stock restoration, and admin operations `getAllOrders`, `getOrderById` and `updateOrderStatus`. Order creation uses `BigDecimal` financial calculations, item snapshots and live inventory validation.


## Repositories

Beyond inherited CRUD, the catalog repositories now expose:
- **CategoryRepository** — `existsBySlug`, `existsBySlugAndIdNot`, `existsByParentId`
- **ProductRepository** — `existsBySlug`, `existsBySlugAndIdNot`, `existsByCategoryId`, `findByCategoryId(Pageable)`
- **ProductVariantRepository** — `existsBySku`, `existsBySkuAndIdNot`, `findByProductId`, `clearDefaultForProduct`/`clearDefaultForProductExcept` (bulk `@Modifying` updates)
- **ProductImageRepository**:
  - `countByProductId(Long)`
  - `findByProductIdAndVariantIsNull(Long)` — product-level gallery only
  - `findByVariantId(Long)`
  - `findByProductId(Long)`
- **AttributeRepository** — `existsByCategoryIdAndNameIgnoreCase`, `existsByCategoryIdAndNameIgnoreCaseAndIdNot`, `findByCategoryId`
- **AttributeOptionsRepository** — `existsByAttributeIdAndValueIgnoreCase`, `existsByAttributeIdAndValueIgnoreCaseAndIdNot`, `findByAttributeId`
- **ProductAttributeValueRepository** — `existsByAttributeId`, `existsByAttributeOptionsId`, `existsByProductIdAndAttributeId`, `existsByProductIdAndAttributeIdAndIdNot`, `findByProductId`
- **UserRepository** — `findByEmail`, `existsByEmail`, `existsByEmailAndIdNot`
- **ReviewsRepository** — `findByProductId`, `existsByProductIdAndUserId` (one review per user per product), `existsByReviewImageId` (delete guard)
- **WishlistRepository** — `findByUserId`, `existsByUserIdAndProductId`
- **CartRepository** — `findByUserId` (`@EntityGraph` on `items`/`items.productVariant`/
  `items.productVariant.product` — the "view my cart" read in one query), `existsByUserId`
- **CartItemRepository** — `findByCartId`, `findByCartIdAndProductVariantId` (merge-on-add
  lookup), `findByIdAndCartId` (ownership-scoped lookup), `deleteByCartId`,
  `deleteByCartIdAndProductVariantId`, plus `@EntityGraph`'d
  `findAllByOrderByAddedAtDesc`/`findByCart_User_IdOrderByAddedAtDesc` for the admin table
- **AddressesRepository** — `findByUserId`, `findByUserIdAndIsDefaultTrue` (address listing and default-address lookup)

`ReviewImageRepository` and `PaymentsRepository` retain their supporting repository roles. **CouponRepository** supports normalized code lookup and coupon management/validation. **OrderRepository** and **OrderItemsRepository** support the order service's checkout, ownership-scoped history/detail retrieval, status management and cancellation workflows.

## Security (`SecurityConfig` + `security/`)

Two trust zones, deliberately handled differently:

- **`/admin/**`** (the Thymeleaf admin panel) — still fully `permitAll()`. It's
  server-rendered multi-page HTML with no login page or cookie/session plumbing;
  bearer-token JWT doesn't apply to it without a materially different mechanism
  (JWT-in-cookie + a login form). That's a real gap (see Known Gaps), not an oversight.
- **`/api/**`** (consumed by an external client — see the Flutter CORS origin below) —
  real JWT auth, stateless (`SessionCreationPolicy.STATELESS`), no CSRF (there's no
  cookie-based session to forge).

**Route rules** (`SecurityConfig.filterChain`, evaluated in order, first match wins):
1. `/admin/**`, static assets (`/css/**`, `/js/**`, `/favicon.png`) → `permitAll`
2. `/api/auth/**` → `permitAll`
3. `GET /api/categories/**`, `/api/products/**`, `/api/attributes/**`, `/api/review-images/*`
   (single segment — deliberately excludes the bare `/api/review-images` admin list) → `permitAll`
4. `/api/me/**`, `POST /api/products/*/reviews`, `PUT`/`DELETE /api/reviews/**`,
   `POST /api/review-images`, `GET`/`PUT /api/users/me` → `authenticated`
5. `/api/users/**` (everything else — admin user management) → `hasRole('ADMIN')`
6. `/api/addresses/**` → controller-level `@PreAuthorize` and service ownership checks scope customer operations to their own addresses; `/user/{userId}` requires `ADMIN`
7. `/api/**` (everything else — catalog writes, review moderation) → `hasRole('ADMIN')`
8. `anyRequest` → `authenticated` (fallback)

**JWT mechanics**: `JwtService` signs/verifies HMAC256 tokens (`jwt.secret`) with the
user id as the subject and a `role` claim, 24h expiry (`jwt.expiration-ms`).
`JwtAuthenticationFilter` (an `OncePerRequestFilter` registered before
`UsernamePasswordAuthenticationFilter`) reads `Authorization: Bearer <token>` and, if
valid, sets a `UsernamePasswordAuthenticationToken` whose **principal is the raw `Long`
user id** — not a `UserDetails` — so there's no DB lookup per request and controllers
just declare `@AuthenticationPrincipal Long userId`. A missing/invalid token isn't itself
an error; it just leaves the request unauthenticated, and the route rules above decide if
that matters. `JsonAuthenticationEntryPoint`/`JsonAccessDeniedHandler` write the same
`ErrorResponse` JSON shape `GlobalExceptionHandler` uses for 401/403s raised by the filter
chain itself (which never reach a controller, so the `@RestControllerAdvice` can't handle them).

**Ownership pattern**: `Wishlist`/`Cart`/`Reviews` never accept a `userId` in the
request body — it always comes from the JWT (`@AuthenticationPrincipal`). Update/delete on
someone else's item returns **404, not 403** — a deliberate choice so a caller can't use
the status code to confirm an id they don't own actually exists.

**Bootstrapping**: registration always creates a `CUSTOMER`. `AdminSeeder`
(`CommandLineRunner`) creates one `ADMIN` account on first startup from
`app.admin.email`/`app.admin.password` — otherwise nobody could ever call an admin-only
endpoint.

- CORS allows `http://localhost:3000`, `http://localhost:5173` (React/Vite dev origins)
  and `http://localhost:38790` (Flutter's default dev server — added outside this
  session, confirming a separate client app targets this API), all common HTTP methods,
  all headers, credentials allowed. TODO: replace with real deployed origins.

## Catalog Module Status

Category, Product and ProductVariant now have complete, validated CRUD service +
controller + request/response DTO layers (see Endpoints/Services above), fixing the
earlier bugs (broken image-URL extraction, `replaceImage` arg mismatch, non-functional
category delete stub, no global exception handling). `ImageUploadResponse`'s `publicId`
field is still mislabeled — it actually holds the DB row id, not Cloudinary's public id
— left as-is since it's outside this module's scope.

## Attributes (EAV) Module

Attribute / AttributeOptions / ProductAttributeValue implement a simple EAV pattern for
category-scoped product specs (e.g. Color, Height, Pet-Safe for a plant category).
Services: `AttributeService`, `AttributeOptionService`, `ProductAttributeValueService`
(each interface + `*Impl`, same pattern as the catalog services).

- **AttributeService** — CRUD; `name` unique per category (case-insensitive); `inputType`
  is the entity's nested enum `Attribute.InputType { TEXT, NUMBER, BOOLEAN, SELECT }`
  (`@Enumerated(STRING)`); delete cascades to the attribute's own `AttributeOptions` but
  is blocked (409) if any `ProductAttributeValue` still references the attribute.
- **AttributeOptionService** — CRUD nested under an attribute; `value` unique per
  attribute (case-insensitive); delete is blocked (409) if any `ProductAttributeValue`
  references the option.
- **ProductAttributeValueService** — assigns/updates a value on a product for a given
  attribute (at most one value per product+attribute pair — enforced, not a multi-valued
  model); validates the value's shape against the attribute's `inputType` and that the
  attribute applies to the product's category. See the controller table above for the
  exact validation rules.

⚠️ **Bug found and fixed during this module's build**: `ProductAttributeValue` isn't
mapped as a `@OneToMany` collection on `Product` (unlike `ProductImage`/`ProductVariant`),
so there was no cascade path — deleting a product that had attribute values threw a raw
FK-constraint `DataIntegrityViolationException`, surfaced by `GlobalExceptionHandler`'s
catch-all as an unhelpful generic 500. Fixed by having `ProductServiceImpl.deleteProduct`
explicitly delete the product's `ProductAttributeValue` rows before deleting the product,
and by adding `log.error(...)` to the catch-all handler so future 500s are traceable
instead of silent.

## Accounts, Reviews, Wishlist & Cart Module

Built alongside real JWT auth (see "Security") since these are the first entities that
actually need a concept of "who's asking" — every write here is either "act as yourself"
(wishlist, cart, your own reviews/profile) or admin-only (user management, moderation).

⚠️ **Gap found and fixed during this module's build**: `ReviewImage` had no `publicId`
column (unlike `Category`/`ProductImage`, which both learned this lesson earlier) — there
was no way to actually delete the asset from Cloudinary, only the DB row. Added `publicId`
and wired `ReviewImageServiceImpl` to populate/use it, matching the established pattern.

Notable design points not already covered under Security/Services/Endpoints above:
- `CartItemResponseDto.lineTotal` uses the variant's `discountedPrice` when set, else
  `price` — the same "charge the discounted price if present" rule used elsewhere.
- A review/wishlist/cart item's *existence* is never hidden behind ownership for
  an admin — every admin service method (`getAllReviews`, `WishlistService.getAll(userId)`,
  `CartService.listAllItems(userId)`/`deleteItemAsAdmin`, `updateReview(..., isAdmin=true, ...)`)
  bypasses the ownership check entirely rather than reusing the customer-facing method
  with a bypass flag threaded through, keeping the two call paths easy to reason about
  independently.
- **Cart is now an aggregate, not a flat table**: `CartItems` (quantity + `ProductVariant`
  + `User`, directly) was replaced by `Cart` (one per `User`) owning a list of `CartItem`
    (`ProductVariant` + `quantity`). `CartController` moved from flat cart-item CRUD at
    `/api/me/cart` to a "my cart" resource with items nested under it
    (`/api/me/cart/items/**`) — see Endpoints above. `AdminCartItemController`'s HTTP shape
    and template (`admin/cart-items/list.html`) were left unchanged; only the service layer
    underneath now reads through `Cart`.

## Admin Panel (Thymeleaf)

A server-rendered admin UI lives at `/admin` and covers **every** route in the backend —
Category, Product, ProductVariant, Product Images (product-level and per-variant,
including bulk upload/replace), Attribute, AttributeOptions, ProductAttributeValue, Users,
Reviews, Wishlist, Cart, Addresses, Coupons and Orders. Every admin controller delegates to the same service used by
the REST API (called directly — no HTTP hop). Views are under
`src/main/resources/templates/admin/`, static CSS under
`src/main/resources/static/css/admin.css`; styled with Bootstrap 5 + Bootstrap Icons via
CDN (no bundler/build step). `WebConfig` (`config/WebConfig.java`) registers
`addRedirectViewController("/", "/admin")` so visiting the site root lands on the
dashboard instead of a 404, and the shared `<head>` fragment links `static/favicon.png`
as the site favicon.

**Visual identity**: restyled with the `frontend-design` skill's principles — a
deliberate "nursery-ledger" palette/type system instead of default Bootstrap blue,
applied site-wide via CSS custom properties in `admin.css` that override Bootstrap
5.3's own variables (`--bs-primary`, `--bs-body-bg`, `--bs-border-color`, etc.), plus
`Fraunces` (headings) + `Inter` (body/UI) via Google Fonts, linked in
`admin/fragments/head.html`. Flat panels (no card shadows), a hairline sage border
instead of grey drop-shadows, squared-off buttons/inputs, and a single clay accent color
used sparingly for primary actions — not verified with an actual screenshot (no browser
tooling available in this environment), only via rendered-HTML/CSS inspection over curl.

| Path | Purpose |
|---|---|
| `GET /admin` | Dashboard — category/product counts, quick links |
| `GET/POST /admin/categories`, `/new`, `/{id}/edit`, `/{id}` (update), `/{id}/delete` | Category CRUD, multipart form with optional image upload |
| `GET/POST /admin/products`, `/new`, `/{id}/edit`, `/{id}` (update), `/{id}/delete` | Product CRUD, paginated list |
| `GET/POST /admin/products/{productId}/variants`, `/new`, `/{variantId}/edit`, `/{variantId}` (update), `/{variantId}/delete` | Variant CRUD, nested under a product |
| `GET /admin/products/{productId}/images`, `POST` (upload), `POST /{imageId}/replace`, `POST /{imageId}/delete` | Product's **general** (non-variant) images only |
| `GET /admin/products/{productId}/variants/{variantId}/images`, `POST` (upload), `POST /{imageId}/replace`, `POST /{imageId}/delete` | One variant's own images — the primary way images get managed |
| `GET/POST /admin/attributes`, `/new`, `/{id}/edit`, `/{id}` (update), `/{id}/delete` | Attribute CRUD, with a `?categoryId=` filter on the list |
| `GET/POST /admin/attributes/{attributeId}/options`, `/new`, `/{optionId}/edit`, `/{optionId}` (update), `/{optionId}/delete` | AttributeOptions CRUD, nested under an attribute |
| `GET/POST /admin/products/{productId}/attribute-values`, `/new`, `/{valueId}/edit`, `/{valueId}` (update), `/{valueId}/delete` | Assign/edit/remove EAV attribute values on a product |
| `GET /admin/users`, `/{id}/edit`, `POST /{id}` (update), `/{id}/delete` | User management — **no create form** (registration owns that); edit only touches role/active-status/name/email, never password |
| `GET /admin/reviews`, `/{id}/edit`, `POST /{id}` (update), `/{id}/delete`, `/{id}/remove-image` | Review moderation — edit bypasses the ownership check a customer is subject to; the linked image (if any) shows inline with its own remove action, no standalone image screen |
| `GET /admin/wishlist` (optional `?userId=`), `POST /{id}/delete` | View + remove only — admins don't add items to someone else's wishlist |
| `GET /admin/cart-items` (optional `?userId=`), `POST /{id}/delete` | Same shape as Wishlist — view + remove only |
| `GET /admin/coupons` | Paginated coupon listing with usage ratios, dynamic status badges, toggle controls and pagination |
| `GET/POST /admin/coupons/create` | Coupon creation form with Bootstrap validation, date-time inputs and error handling |
| `GET/POST /admin/coupons/{id}/edit` | Coupon edit form and update submission |
| `POST /admin/coupons/{id}/toggle` | Instant coupon activation/deactivation |
| `POST /admin/coupons/{id}/delete` | Coupon deletion |
| `GET /admin/coupons/check-code` | AJAX helper for coupon code uniqueness checking |
| `GET /admin/orders` | Paginated order overview with payment status indicators and quick access links |
| `GET /admin/orders/{id}` | Detailed order view with customer information, billing/shipping addresses, item snapshots and current statuses |
| `POST /admin/orders/{id}/status` | Administrative order status state transition (`PENDING`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`) and payment status update |
| `POST /admin/orders/{id}/cancel` | Administrative cancellation with automatic inventory rollback |

### Admin Coupon Management — `/admin/coupons`

`AdminCouponController` is protected with `@PreAuthorize("hasRole('ADMIN')")` and provides the complete Thymeleaf coupon workflow. The list view at `templates/admin/coupons/list.html` includes usage ratios, status badges, toggle controls and pagination. The shared create/edit view at `templates/admin/coupons/form.html` includes Bootstrap 5 validation states, date-time inputs and error handlers. The controller also exposes an AJAX code-uniqueness helper at `GET /admin/coupons/check-code`.

### Admin Order Management — `/admin/orders`

The admin dashboard includes centralized order management through `AdminOrderController`, protected with `@PreAuthorize("hasRole('ADMIN')")`. `GET /admin/orders` provides a filterable, paginated listing across lifecycle statuses. `GET /admin/orders/{id}` exposes the complete order record, including customer information, billing/shipping addresses, immutable product/variant snapshots and financial totals. Administrators can update both order and payment status through `POST /admin/orders/{id}/status`, or cancel an order through `POST /admin/orders/{id}/cancel`; administrative cancellation uses the same inventory rollback rules as customer cancellation. `templates/admin/orders/list.html` provides consolidated tracking with payment status indicators and quick links, while `templates/admin/orders/detail.html` provides line items, pricing breakdown (subtotal, coupon discount and grand total), customer details and a status-update sidebar.

**Image screens are split by scope**, not combined behind a dropdown:
`templates/admin/images/list.html` (general product gallery, linked from the product
list as "General Images") and `templates/admin/images/variant-list.html` (one variant's
photos, linked from that variant's row on the variant list as "Images"). Both post to
`ProductImageService`'s bulk upload methods — a single file is just a batch of one, so
there's no separate single-upload control — and each image row has its own inline
"Replace" mini-form plus a delete button. Two controllers back this:
`AdminProductImageController` (general only, via `getGeneralImages`) and
`AdminVariantImageController` (one variant, via `getImagesByVariant`).

**Every product should have at least one variant** (even a single fixed size/pack counts
as its one variant) — this isn't a hard DB constraint (existing legacy products may have
zero), but the UI nudges toward it: creating a product redirects straight into "add a
variant" instead of back to the list, and the variant list shows a warning banner while
empty. `ProductResponseDto.variantCount` surfaces this on the product list too.

**Attribute value screen** (`templates/admin/attribute-values/form.html`): the visible
input depends on the chosen attribute's `inputType`, which isn't known until the user
picks one — solved with a small vanilla-JS block (`AdminProductAttributeValueController`
serializes each of the product category's attributes' `inputType` + SELECT options into
`attributeMetaJson` via Thymeleaf's `th:inline="javascript"`; the script shows a free-text
input or rebuilds an option `<select>` on attribute-change, and pre-selects the current
option when editing).

**Slug auto-fill**: `static/js/slug-autofill.js` (shared by the Category and Product
forms) slugifies the `#name` field into `#slug` as the user types, and stops as soon as
the user edits `#slug` directly — or immediately in edit forms, where the slug already
has a value on page load.

Design notes:
- HTML forms only support GET/POST, so updates and deletes use dedicated POST routes
  (`POST /{id}` for update, `POST /{id}/delete` for delete) rather than PUT/DELETE.
- `GlobalExceptionHandler` is a `@RestControllerAdvice` (JSON-only), so it would return a
  raw JSON body if an admin `@Controller` let an exception escape. Each admin controller
  therefore catches `ResourceNotFoundException` / `DuplicateResourceException` /
  `BusinessRuleViolationException` itself and re-renders the form/list with a flash or
  inline error message instead. `ImageUploadException` (e.g. an unsupported file type)
  was originally missing from `AdminCategoryController`'s create/update catch clauses —
  a real bug, since Cloudinary validation failures crashed to a raw JSON error page
  instead of the form; fixed by adding it there (it was already caught correctly in the
  image controllers).
- Bean Validation errors (`@Valid` + `BindingResult`) are rendered inline per-field via
  Thymeleaf `th:errors`, matching the same validation rules as the REST DTOs.
- `/admin/**` is still `permitAll()` even though `/api/**` is now JWT-secured — see
  "Security" above for why, and Known Gaps for the follow-up this implies.

## Critical Stabilization Plan — Must Complete Before Payments

The project will **not move directly into Razorpay integration**. The following issues affect security, money, inventory or order correctness and must be addressed first. These items will be completed **one by one**, with each change reviewed before moving to the next.

| Priority | Critical Change | Why It Must Be Fixed | Status |
|---|---|---|---|
| P0 | Secure `/admin/**` | The current admin panel is reachable without authentication, allowing unauthorized management of catalog, users, coupons and orders. | TODO — Critical |
| P0 | Externalize secrets | Database, Cloudinary, JWT and admin bootstrap credentials must not remain plaintext in source-controlled configuration. | TODO — Critical |
| P0 | Introduce database migrations | `ddl-auto=update` is unsafe as the production schema change mechanism. Flyway/Liquibase is required before payment-related schema changes. | TODO — Critical |
| P0 | Standardize all monetary values on `BigDecimal` | Product and variant prices currently use `Double` while orders use `BigDecimal`. Floating-point values must not enter checkout or payment calculations. | TODO — Critical |
| P0 | Make inventory updates concurrency-safe | Current read-check-decrement stock logic can oversell when simultaneous checkouts read the same stock. Stock decrement/restoration must be atomic and idempotent. | TODO — Critical |
| P0 | Validate address ownership during checkout | A valid address ID alone must not be accepted; shipping and billing addresses must belong to the authenticated customer. | TODO — Critical |
| P0 | Enforce coupon validation and usage atomically | Checkout must call coupon validity rules before discounting, prevent usage-limit races and increment usage only at the correct confirmed lifecycle point. | TODO — Critical |
| P0 | Enforce order status transitions centrally | Admin status updates must not freely set invalid states or bypass cancellation/restocking rules. Cancellation and stock rollback must be idempotent. | TODO — Critical |
| P1 | Redesign Payment entity before Razorpay | `Payments` currently uses `Double` and lacks provider order IDs, payment IDs, signature/webhook metadata, failure data and idempotency fields required for a safe gateway workflow. | TODO — Next module |
| P1 | Build Razorpay verification + webhook flow | Client success callbacks alone must never mark an order paid. Backend signature verification and webhook verification are required. | BLOCKED by Payment redesign |

### Required Completion Order

1. **Secure administration and configuration**
  - Protect `/admin/**`.
  - Externalize secrets and remove production credentials from tracked configuration.

2. **Make database changes production-safe**
  - Introduce migrations before changing money, order and payment schemas.

3. **Fix money and catalog invariants**
  - Convert product and variant price fields from `Double` to `BigDecimal`.
  - Add required database constraints for SKU, stock and active flags where missing.

4. **Fix checkout correctness**
  - Validate product and variant availability.
  - Validate shipping/billing address ownership.
  - Validate coupon rules server-side.
  - Use atomic inventory operations.

5. **Fix order lifecycle correctness**
  - Define allowed status transitions.
  - Prevent direct `CANCELLED` status updates from bypassing rollback logic.
  - Ensure stock restoration can happen only once.

6. **Redesign Payments and implement Razorpay**
  - Replace `Payments` with a production-oriented payment model.
  - Create provider order.
  - Verify payment signature server-side.
  - Process verified webhooks idempotently.
  - Update payment and order states transactionally.

### Checkout Rule Going Forward

The backend remains the source of truth for all financial and inventory decisions:

```text
Client Cart / Buy Now
        ↓
Backend reloads live ProductVariant data
        ↓
Validate product + variant active
        ↓
Validate address ownership
        ↓
Atomically validate/reserve inventory
        ↓
Calculate subtotal using BigDecimal
        ↓
Validate coupon server-side
        ↓
Create Order snapshot
        ↓
Create Payment provider order
        ↓
Client completes Razorpay payment
        ↓
Backend verifies signature
        ↓
Webhook provides independent confirmation
        ↓
Update Payment + Order transactionally
```

## Known Gaps / Next Steps

### Critical — scheduled before Payments

1. Plaintext DB/Cloudinary/JWT/admin-seed credentials in `application.properties`.
2. `/admin/**` currently lacks real authentication.
3. No production migration mechanism; `spring.jpa.hibernate.ddl-auto=update` is still used.
4. Product and ProductVariant monetary fields need conversion from `Double` to `BigDecimal`.
5. Inventory decrement/restoration is not yet safe against concurrent checkout overselling.
6. Checkout must explicitly verify address ownership.
7. Coupon application and usage counting must be integrated into an atomic order/payment lifecycle.
8. `updateOrderStatus` needs controlled transitions and must not bypass cancellation inventory rollback.
9. The Payment module is not production-ready and Razorpay is not yet implemented.

### Non-blocking / Later

10. `Addresses.id` remains `int` while much of the service/API layer uses `Long`; address-reference modeling can be further normalized if needed.
5. `Product.name` was added after `monstera-deliciosa`, `snake-plant` and `marigold`
   were already created — those rows have `name = NULL` until edited once through the
   form (harmless: reads still work, only create/update require it going forward).
6. "At least one variant per product" is a UI nudge only, not enforced in `ProductService`
   — a product can still end up with zero variants (e.g. if its only variant is deleted).
7. No password-change/reset flow — `UserSelfUpdateRequestDto` only covers name/email.
   An admin can't set/reset a password either (deliberately — `UserUpdateRequestDto` has
   no password field); the only way in is registration.
8. `AdminSeeder` checks for *any* `ADMIN` user, not specifically one matching
   `app.admin.email` — if you change that property after the first admin already exists,
   nothing re-seeds; you'd promote a user to `ADMIN` via `PUT /api/users/{id}` instead.
9. The Postman collection (`postman/Nikyfarm-API.postman_collection.json`) has no
   Attribute/AttributeOptions/ProductAttributeValue folders — those REST modules were
   built after the collection and never backfilled. It does now cover Auth/Users/Reviews/
   Review Images/Wishlist/Cart, with every catalog write request updated to send
   `Authorization: Bearer {{authToken}}` now that those routes are admin-only.
10. A request with an HTTP method nothing is mapped to (e.g. `DELETE` on a `POST`-only
    admin route) returns a raw 500, not 405 — `GlobalExceptionHandler`'s catch-all doesn't
    special-case `HttpRequestMethodNotSupportedException`. Pre-existing, unrelated to auth.
11. The `CartItems -> Cart`/`CartItem` migration is schema-only, not data-preserving:
    `ddl-auto=update` creates the new `carts` table and alters `cart_items` to add the
    `cart_id` FK, but it does not backfill a `Cart` row per user or move old
    `cart_items.user_id` rows onto it. In dev, drop the old `cart_items` table and let it
    recreate empty rather than trying to carry old cart data forward. Stock is also not
    hard-enforced on add/update (see CartController above) — only flagged via
    `stockInsufficient` for the client to act on.
---

# Complete Project Issue Register & Production Completion Roadmap

**Audit status:** 2026-09-09

This section is the master issue register for the project based on the modules and implementation state documented in this `project.md`, together with the Order, Coupon, Cart, Product, ProductVariant, Payment, Security and configuration files reviewed during the current work.

The items below intentionally include **critical, important and later improvements** so that the project has one checklist instead of losing issues as new modules are implemented. Items are classified by production impact; not every item blocks the current module.

## Severity Definitions

| Severity | Meaning | Rule |
|---|---|---|
| **P0 — Critical** | Security, money, inventory, payment or data-integrity failure | Must be fixed before production/payment integration where applicable |
| **P1 — Required** | Important production correctness or missing workflow | Must be completed before project production release |
| **P2 — Important** | Reliability, maintainability or API correctness improvement | Complete before or shortly after first production release |
| **P3 — Later** | UX, administration, observability or technical-debt improvement | Planned but not blocking |

---

## A. Security & Access Control

| ID | Severity | Issue | Current Risk / Required Change | Status |
|---|---|---|---|---|
| SEC-01 | P0 | `/admin/**` is permit-all | Any unauthenticated visitor can potentially access administrative management routes. Require authentication and `ADMIN` authorization consistently. | TODO |
| SEC-02 | P0 | Secrets stored in configuration | DB, Cloudinary, JWT and admin bootstrap credentials must be externalized and removed from tracked configuration. Rotate any credentials already committed. | TODO |
| SEC-03 | P1 | Production CORS origins are not finalized | Replace localhost development origins with explicit deployed app origins and review credential usage. | TODO |
| SEC-04 | P1 | JWT lifecycle is minimal | Add a defined production policy for token expiry, logout/revocation strategy if required, secret rotation and invalidated/deactivated users. | TODO |
| SEC-05 | P1 | Admin bootstrap is development-oriented | `AdminSeeder` is not a production credential lifecycle. Use a controlled initial-admin provisioning process. | TODO |
| SEC-06 | P2 | No documented rate limiting / abuse controls | Public auth, coupon validation and future payment endpoints need abuse protection appropriate to deployment infrastructure. | TODO |

## B. Database & Schema Management

| ID | Severity | Issue | Current Risk / Required Change | Status |
|---|---|---|---|---|
| DB-01 | P0 | `ddl-auto=update` is used as schema migration | Introduce Flyway or Liquibase before money/order/payment schema changes. | TODO |
| DB-02 | P0 | Existing schema changes may be data-destructive or incomplete | Write explicit migrations for the Cart aggregate conversion and all upcoming entity changes. | TODO |
| DB-03 | P1 | `Addresses.id` type differs from most entity IDs | Normalize the identifier strategy or explicitly preserve compatibility through a migration plan. | TODO |
| DB-04 | P1 | Required database constraints need review | Enforce SKU uniqueness, required stock fields, non-null active flags and other service invariants at DB level where appropriate. | TODO |
| DB-05 | P1 | Order/payment indexes are incomplete for provider workflows | Add indexes/unique constraints for order number and future provider order/payment IDs through migrations. | TODO |
| DB-06 | P2 | Audit/history strategy is incomplete | Define whether sensitive lifecycle changes require actor/time/history records beyond `createdAt`/`updatedAt`. | TODO |

## C. Money & Pricing

| ID | Severity | Issue | Current Risk / Required Change | Status |
|---|---|---|---|---|
| MONEY-01 | P0 | Product and ProductVariant use `Double` for money | Convert catalog prices and related DTO/mapper/calculation code to `BigDecimal`. | TODO |
| MONEY-02 | P0 | Money representation is inconsistent across catalog and orders | Establish one rounding and scale policy for all prices, discounts, totals and payments. | TODO |
| MONEY-03 | P1 | Payment entity amount uses `Double` | Replace with `BigDecimal`; Razorpay conversion to smallest currency unit must occur only at the provider boundary. | TODO |
| MONEY-04 | P1 | Selling-price rule must be centralized | The `discountedPrice != null ? discountedPrice : price` rule should be reusable and validated consistently. | TODO |
| MONEY-05 | P1 | Discount validation needs final server-side enforcement | Coupon discounts must never make totals negative and percentage/flat rules must be validated against current subtotal. | TODO |
| MONEY-06 | P2 | Currency is not modeled explicitly | Define the supported currency policy before payment integration; persist currency where payment/provider records require it. | TODO |

## D. Product & Variant Integrity

| ID | Severity | Issue | Current Risk / Required Change | Status |
|---|---|---|---|---|
| CAT-01 | P0 | Product/variant availability is not guaranteed during checkout | Checkout must reject inactive products and inactive variants. | TODO |
| CAT-02 | P1 | Stock can be null/negative without complete invariant enforcement | Add validation and DB constraints; stock must never become negative. | TODO |
| CAT-03 | P1 | SKU uniqueness must be enforced by DB | Repository checks alone race; add a database unique constraint/index. | TODO |
| CAT-04 | P1 | ProductVariant `ManyToOne` loading should be reviewed | Use explicit `LAZY` loading where appropriate to avoid accidental eager fetch behavior. | TODO |
| CAT-05 | P1 | Active/default booleans require null-safe defaults | Prevent tri-state `Boolean` values when business logic expects true/false. | TODO |
| CAT-06 | P2 | Product-level versus variant-level price ownership is ambiguous | Define whether products without variants are supported; otherwise make variant price/stock the selling source of truth. | TODO |
| CAT-07 | P2 | A product can have zero variants | Enforce the business rule or define a valid non-variant purchasing path. | TODO |
| CAT-08 | P2 | Legacy product rows may contain null `name` values | Clean existing data through migration/admin correction before stricter production constraints. | TODO |

## E. Cart Integrity

| ID | Severity | Issue | Current Risk / Required Change | Status |
|---|---|---|---|---|
| CART-01 | P0 | Cart quantity can exceed live stock | Validate requested total quantity on add/update; checkout must revalidate independently. | TODO |
| CART-02 | P0 | Cart item may reference inactive/unavailable catalog data | Reject inactive product/variant additions and quantity updates. | TODO |
| CART-03 | P1 | Quantity invariants require complete validation | Enforce minimum quantity of 1 and reject invalid values. | TODO |
| CART-04 | P1 | Old CartItems-to-Cart migration is not data-preserving | Decide migration/backfill strategy or explicitly reset development-only cart data. | TODO |
| CART-05 | P1 | Cart should not reserve stock | Preserve current lightweight cart design; reservation belongs to checkout/order lifecycle. | DESIGN CONFIRMED |
| CART-06 | P1 | Cart clearing currently happens too early for payment UX | Clear the cart after verified successful payment, not merely after creating an unpaid order. | TODO |
| CART-07 | P2 | Concurrent cart edits are not explicitly protected | Consider optimistic locking/versioning if multi-device updates become important. | TODO |
| CART-08 | P3 | Admin cart mutation is questionable | Prefer analytics/support visibility over arbitrary removal of customer cart items unless required. | REVIEW LATER |

## F. Address & Customer Ownership

| ID | Severity | Issue | Current Risk / Required Change | Status |
|---|---|---|---|---|
| ADDR-01 | P0 | Checkout loads addresses by ID without final ownership validation | Shipping and billing addresses must belong to the authenticated user unless an explicitly authorized admin workflow exists. | TODO |
| ADDR-02 | P1 | Order address history relies on live address relations | Decide whether order-time address snapshots are required so later customer edits do not alter historical fulfillment records. | TODO |
| ADDR-03 | P2 | Identifier type inconsistency | See DB-03; normalize `int`/`Long` usage across entity, repository, DTO and API boundaries. | TODO |

## G. Inventory & Concurrency

| ID | Severity | Issue | Current Risk / Required Change | Status |
|---|---|---|---|---|
| INV-01 | P0 | Read-check-decrement stock can oversell | Use atomic conditional updates, pessimistic locking, or another deliberate concurrency strategy. | TODO |
| INV-02 | P0 | Stock restoration must be idempotent | A cancelled order must restore each reserved quantity exactly once. | TODO |
| INV-03 | P0 | Inventory reservation lifecycle is undefined for unpaid orders | Define reservation expiry/release when a payment is abandoned, fails or expires. | TODO |
| INV-04 | P1 | Direct order and cart checkout must share inventory logic | Centralize validation/reservation so flows cannot diverge. | TODO |
| INV-05 | P1 | Product/variant deletion with historical orders needs policy | Historical `OrderItems` snapshots are good, but decide how active order references to variants are handled. | TODO |
| INV-06 | P2 | No inventory audit trail | Consider stock movement/history records if operational reconciliation is required. | TODO |

## H. Coupon Correctness

| ID | Severity | Issue | Current Risk / Required Change | Status |
|---|---|---|---|---|
| COUPON-01 | P0 | Order flow calculates discount without guaranteed full `isValid()` lifecycle enforcement | Validate active state, dates, max usage and minimum subtotal before applying. | TODO |
| COUPON-02 | P0 | Usage counting can race | Make usage increment conditional/atomic against maxUses. | TODO |
| COUPON-03 | P0 | Coupon consumption point is not finalized | Do not permanently consume a coupon for an abandoned/failed payment unless business rules explicitly require it. Tie consumption to confirmed order/payment lifecycle. | TODO |
| COUPON-04 | P1 | Coupon deletion/history policy needs definition | Existing orders should retain historical discount results even if a coupon is later disabled/deleted. | TODO |
| COUPON-05 | P2 | Code normalization must be universal | Ensure create, update, lookup and checkout all use the same trim/uppercase policy. | TODO |

## I. Order Lifecycle & Checkout

| ID | Severity | Issue | Current Risk / Required Change | Status |
|---|---|---|---|---|
| ORDER-01 | P0 | Status updates currently allow arbitrary assignment | Define an explicit state-transition matrix and reject invalid transitions. | TODO |
| ORDER-02 | P0 | Admin can set `CANCELLED` through status update and bypass cancellation logic | Route all cancellations through one cancellation operation with inventory/payment handling. | TODO |
| ORDER-03 | P0 | Unpaid-order lifecycle is incomplete | Define pending-payment expiry, retry behavior and stock release. | TODO |
| ORDER-04 | P0 | Checkout must recalculate all totals on backend | Never trust Flutter/client cart totals, discounts or amounts. | DESIGN REQUIRED IN CODE |
| ORDER-05 | P0 | Duplicate checkout/idempotency protection is absent | Prevent repeated client retries from creating duplicate orders/reservations. | TODO |
| ORDER-06 | P1 | Order number generation should rely on DB uniqueness/retry safety | Keep the unique constraint and define collision/retry handling. | TODO |
| ORDER-07 | P1 | Shipping fee policy is currently hardcoded to zero | Define calculation source and snapshot it into the order when shipping is implemented. | TODO |
| ORDER-08 | P1 | Payment status should not be freely changed by admin UI | Payment status should normally be driven by verified payment/refund events; admin override needs an audited exceptional path. | TODO |
| ORDER-09 | P1 | Cart clearing timing must follow verified payment | Preserve cart for failed/abandoned attempts and clear only on confirmed success according to final flow. | TODO |
| ORDER-10 | P2 | Order address snapshot policy is incomplete | See ADDR-02; historical fulfillment data should not unexpectedly change. | TODO |
| ORDER-11 | P2 | Order status/payment status are separate but coupled | Document allowed combinations, e.g. whether `CANCELLED + PAID` requires refund state. | TODO |

### Proposed Order Transition Matrix

This is the target to implement, not a claim that it already exists:

| From | Allowed To |
|---|---|
| `PENDING` | `PROCESSING`, `CANCELLED` |
| `PROCESSING` | `SHIPPED`, `CANCELLED` only if fulfillment rules permit |
| `SHIPPED` | `DELIVERED` |
| `DELIVERED` | No normal lifecycle transition |
| `CANCELLED` | No normal lifecycle transition |

Payment transitions will be defined separately by verified gateway/refund events.

## J. Payment & Razorpay

| ID | Severity | Issue | Current Risk / Required Change | Status |
|---|---|---|---|---|
| PAY-01 | P0 | Current `Payments` model is not production-ready | Redesign entity/model before Razorpay implementation. | TODO |
| PAY-02 | P0 | Payment amount uses `Double` | Convert to `BigDecimal` and apply exact provider amount conversion. | TODO |
| PAY-03 | P0 | Provider order/payment identifiers are missing | Persist Razorpay order ID and payment ID with uniqueness rules. | TODO |
| PAY-04 | P0 | Client callback cannot be trusted as payment confirmation | Verify Razorpay signature on backend. | TODO |
| PAY-05 | P0 | Webhook verification is not implemented | Verify webhook signature using raw request body and process events safely. | TODO |
| PAY-06 | P0 | Payment processing needs idempotency | Duplicate callbacks/webhooks/retries must not double-update orders, stock or coupons. | TODO |
| PAY-07 | P0 | Unpaid/failed payment retry lifecycle is undefined | Define whether an existing provider order/payment attempt is reused or a new attempt is created. | TODO |
| PAY-08 | P1 | Failure information is missing | Persist provider failure code/message where appropriate for support and retry decisions. | TODO |
| PAY-09 | P1 | Currency is missing from payment model | Persist explicit currency required by provider records. | TODO |
| PAY-10 | P1 | Refund lifecycle is incomplete | Define refund request, provider confirmation, idempotency and order/payment state changes. | TODO |
| PAY-11 | P1 | Webhook endpoint security must coexist with JWT | Exclude only the verified provider webhook endpoint from JWT authentication; do not leave unrelated endpoints public. | TODO |
| PAY-12 | P2 | Payment attempt history is not modeled | Consider separate attempts/events if retries and reconciliation require them. | TODO |

## K. API & Error Handling

| ID | Severity | Issue | Current Risk / Required Change | Status |
|---|---|---|---|---|
| API-01 | P1 | Unsupported HTTP methods can fall into generic 500 handling | Return proper 405 via explicit exception handling. | TODO |
| API-02 | P1 | Admin and REST error handling differ | Define a consistent strategy for HTML error pages versus JSON API errors without leaking internal exceptions. | REVIEW |
| API-03 | P1 | Pagination limits need policy | Enforce maximum page size to prevent expensive unrestricted queries. | TODO |
| API-04 | P2 | Sorting inputs need allow-listing | Restrict sort fields to supported entity properties rather than arbitrary client values. | TODO |
| API-05 | P2 | API versioning is not defined | Decide whether `/api/v1` is needed before public production clients depend on the contract. | REVIEW |
| API-06 | P2 | Postman collection is incomplete | Backfill Attributes/EAV and all newer Order/Coupon/Payment endpoints. | TODO |

## L. Reviews, Wishlist & User Accounts

| ID | Severity | Issue | Current Risk / Required Change | Status |
|---|---|---|---|---|
| USER-01 | P1 | No password change/reset flow | Add secure password change/reset process before production launch. | TODO |
| USER-02 | P1 | Admin password reset workflow is absent | Define controlled reset/provisioning behavior. | TODO |
| USER-03 | P2 | Review verified-purchase lifecycle is not integrated with orders | Implement verification only if the feature is required for launch. | TODO |
| USER-04 | P2 | User deletion and historical references need retention policy | Avoid breaking orders/reviews/audit data through destructive deletes. Prefer a deliberate lifecycle/retention policy. | TODO |
| USER-05 | P3 | Admin manual cart/wishlist operations need business policy | Keep only operations that have a clear support/admin purpose. | REVIEW |

## M. Cloudinary & File Management

| ID | Severity | Issue | Current Risk / Required Change | Status |
|---|---|---|---|---|
| FILE-01 | P1 | Database and Cloudinary operations are not one atomic transaction | Upload/delete failures can leave orphaned provider assets or DB rows. Add compensation/reconciliation strategy for production. | TODO |
| FILE-02 | P1 | Image cleanup needs operational reconciliation | Provide a way to detect orphaned Cloudinary assets and stale DB references. | TODO |
| FILE-03 | P2 | `ImageUploadResponse.publicId` is mislabeled | Correct API naming or version/document the actual meaning before clients rely on it. | TODO |
| FILE-04 | P2 | Upload validation should be defense-in-depth | Continue validating size/type server-side and consider content inspection where deployment risk requires it. | REVIEW |

## N. Admin Panel

| ID | Severity | Issue | Current Risk / Required Change | Status |
|---|---|---|---|---|
| ADMIN-01 | P0 | Admin routes are publicly reachable | Same as SEC-01; fix first. | TODO |
| ADMIN-02 | P1 | Order payment status can be manually changed | Prevent normal admin UI from forging gateway state; use verified payment/refund operations. | TODO |
| ADMIN-03 | P1 | No explicit admin audit trail | Record who changed sensitive order, coupon, user and payment state where required. | TODO |
| ADMIN-04 | P2 | Admin exception handling is controller-specific | Consider a dedicated MVC exception handler for consistent HTML error behavior. | TODO |
| ADMIN-05 | P3 | Dashboard operational metrics are basic | Add meaningful order/payment/inventory metrics after core correctness is complete. | LATER |

## O. Observability, Testing & Deployment

| ID | Severity | Issue | Current Risk / Required Change | Status |
|---|---|---|---|---|
| OPS-01 | P1 | Production logging policy is not documented | Configure structured/appropriate logging and ensure secrets/payment data are never logged. | TODO |
| OPS-02 | P1 | Health/readiness checks are not documented | Add production health monitoring appropriate to deployment. | TODO |
| OPS-03 | P1 | Backup and restore procedure is not documented | Define MySQL backup, restore and disaster recovery before production. | TODO |
| OPS-04 | P1 | Test coverage requirements are not defined | Add tests for checkout, stock concurrency, coupons, authorization and payment idempotency. | TODO |
| OPS-05 | P1 | No documented staging/test payment environment workflow | Use Razorpay test mode and separate environment configuration before live credentials. | TODO |
| OPS-06 | P2 | Deployment profiles are not documented | Separate development, staging and production configuration. | TODO |
| OPS-07 | P2 | No documented monitoring/alerting policy | Add alerts for application failures, payment webhook failures and inventory anomalies. | TODO |
| OPS-08 | P2 | No reconciliation jobs defined | Plan reconciliation for payments, expired reservations and orphaned files. | TODO |

---

# Missing / Incomplete Functional Areas

The following are not all bugs; they are functional areas that are either incomplete, intentionally absent or require a business decision before production.

| Area | Current State | Required Decision / Work |
|---|---|---|
| Payments | Entity exists but production gateway flow not implemented | Complete Payment redesign + Razorpay |
| Refunds | Payment status has `REFUNDED` but full provider workflow is absent | Implement after payment success flow |
| Password reset | Not implemented | Required before public production launch |
| Shipping | `shippingFee` exists but is currently zero | Define shipping calculation/delivery policy |
| Inventory reservation expiry | Not implemented | Required for online payment abandonment |
| Payment retries | Not defined | Required for failed/interrupted checkout |
| Order address snapshots | Not finalized | Decide before production fulfillment |
| Audit logs | Not implemented | Recommended for sensitive admin/payment actions |
| Product stock history | Not implemented | Recommended for operational inventory control |
| API documentation | Not documented as OpenAPI/Swagger in current project documentation | Add if public/mobile client maintenance benefits |
| Automated tests | No complete test strategy documented | Required for critical flows |
| Production profiles | Not documented | Required for safe environment separation |

---

# Master Completion Order

The project should now be completed in this order to avoid rework:

## Phase 1 — Security & Schema Foundation

1. **SEC-01 / ADMIN-01:** Secure `/admin/**`.
2. **SEC-02:** Externalize and rotate secrets.
3. **DB-01 / DB-02:** Introduce migrations and create a baseline migration strategy.
4. **DB-03:** Decide and normalize address identifier migration strategy.

## Phase 2 — Catalog, Money & Cart Integrity

5. **MONEY-01 / MONEY-02 / MONEY-03:** Convert all monetary values to `BigDecimal`.
6. **CAT-02 / CAT-03 / CAT-05:** Enforce stock, SKU and boolean invariants.
7. **CART-01 / CART-02 / CART-03:** Validate quantity, stock and availability in cart operations.
8. **CAT-01:** Revalidate active product/variant state at checkout.

## Phase 3 — Checkout, Inventory, Coupon & Orders

9. **ADDR-01:** Validate address ownership.
10. **INV-01 / INV-02:** Implement safe atomic inventory reserve/restore.
11. **INV-03 / ORDER-03:** Implement unpaid-order reservation expiry/release design.
12. **COUPON-01 / COUPON-02 / COUPON-03:** Make coupon validation and consumption lifecycle safe.
13. **ORDER-01 / ORDER-02 / ORDER-04 / ORDER-05:** Implement state transitions, centralized cancellation, server-side totals and checkout idempotency.
14. **CART-06 / ORDER-09:** Move cart clearing to verified payment success flow.

## Phase 4 — Payment & Razorpay

15. **PAY-01 through PAY-11:** Redesign payment model and implement provider order creation, signature verification, webhook verification, idempotency and failure handling.
16. Implement payment retry behavior.
17. Implement refund workflow after the core success flow is stable.

## Phase 5 — Production Release Requirements

18. Password change/reset.
19. Production CORS and environment profiles.
20. Error handling and API hardening.
21. Tests for all P0/P1 checkout and payment scenarios.
22. Logging, health checks, backup/restore and monitoring.
23. Complete API/Postman documentation.

## Phase 6 — Important Enhancements

24. Address snapshots.
25. Audit logs.
26. Stock movement history.
27. Cloudinary reconciliation.
28. Admin dashboard metrics.
29. API versioning and OpenAPI documentation.

---

# Immediate Next Work List — Do Not Skip

The following is the exact active queue before starting Razorpay:

- [ ] **1. Secure `/admin/**`**
- [ ] **2. Externalize and rotate secrets**
- [ ] **3. Introduce database migrations**
- [✓] **4. Convert Product/ProductVariant/Payment money fields to `BigDecimal`**
- [ ] **5. Add catalog database and entity invariants (SKU, stock, active/default values)**
- [✓] **6. Fix cart quantity, stock and availability validation**
- [ ] **7. Validate checkout address ownership**
- [ ] **8. Implement concurrency-safe inventory reservation/restoration**
- [ ] **9. Make coupon validation/usage lifecycle atomic**
- [ ] **10. Enforce order transitions, centralized cancellation and checkout idempotency**
- [ ] **11. Define unpaid-order expiry and stock release**
- [ ] **12. Redesign Payment entity**
- [ ] **13. Implement Razorpay order creation, backend verification and webhooks**
- [ ] **14. Add payment retry/refund behavior**
- [ ] **15. Complete production testing and operations requirements**

## Rule for Future Changes

When a new issue is found during implementation, it must be added to this master issue register with:

- Issue ID
- Severity
- Affected module
- Concrete risk
- Required change
- Status
- Dependencies

This prevents discovered issues from being lost between modules.
