# Redis Setup and API Testing Guide

This guide explains how to start Redis and test the Product Management API.

## Prerequisites

- Java 21
- Maven
- Docker Desktop
- PostgreSQL running with a database named `redis_demo`
- The Spring Boot application running on port `8080`

## 1. Start Redis with Docker

Create a Redis 7 container:

```bash
docker run -d --name redis-cache -p 6379:6379 redis:7
```
First time it not found in local so it pull form docker hub

<img width="940" height="529" alt="image" src="https://github.com/user-attachments/assets/a38be094-6650-467e-8c03-83ec1464de8c" />

If the container already exists but is stopped:

```bash
docker start redis-cache
```

<img width="263" height="42" alt="image" src="https://github.com/user-attachments/assets/03121162-f72d-41bf-8f9e-0f8f230b7df0" />


Check the container:

```bash
docker ps
```
<img width="939" height="62" alt="image" src="https://github.com/user-attachments/assets/07c659bf-7ce5-4efe-aee2-50a33c0c19f8" />


<img width="941" height="304" alt="image" src="https://github.com/user-attachments/assets/f071658f-1fc5-45c8-b1f5-185a3e61e643" />



Expected port mapping:

```text
0.0.0.0:6379->6379/tcp
```

## 2. Verify Redis connectivity

Using Redis CLI:

```bash
redis-cli PING
```

Expected response:

```text
PONG
```

Using Docker when `redis-cli` is not installed locally:

```bash
docker exec -it redis-cache redis-cli PING
```

On PowerShell, verify that port `6379` is open:

```powershell
Test-NetConnection localhost -Port 6379
```

The result should contain:

```text
TcpTestSucceeded : True
```

## 3. Spring Boot Redis configuration

The application uses Redis at `localhost:6379` by default:

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.cache.type=redis
spring.cache.redis.time-to-live=10m
spring.cache.redis.cache-null-values=false
```

Start the application:

```bash
mvn spring-boot:run
```

The API is available at:

```text
http://localhost:8080
```

If Spring Boot is running inside Docker, `localhost` points to the application container. In that case, configure the Redis service/container name instead:

```properties
spring.data.redis.host=redis-cache
spring.data.redis.port=6379
```

## 4. Postman setup

Create a Postman environment variable:

```text
baseUrl = http://localhost:8080
```

For POST and PUT requests, add this header:

```text
Content-Type: application/json
```

## 5. Test the APIs

### Create a product

```http
POST {{baseUrl}}/api/products
```

Request body:

```json
{
  "productName": "Mechanical Keyboard",
  "price": 89.99
}
```

Expected result: `201 Created`.

Example response:

```json
{
  "id": 1,
  "productName": "Mechanical Keyboard",
  "price": 89.99
}
```

### Get a product by ID

```http
GET {{baseUrl}}/api/products/1
```

First request:

1. Redis cache miss.
2. PostgreSQL `SELECT` executes.
3. The response is stored under `products::1`.

Expected result: `200 OK`.

### Test a cache hit

Send the same request again:

```http
GET {{baseUrl}}/api/products/1
```

Expected behavior:

- Redis returns the cached product.
- The service method is skipped by the Spring cache proxy.
- No PostgreSQL query is executed.

### Get all products

```http
GET {{baseUrl}}/api/products
```

The first request queries PostgreSQL and stores the result under:

```text
productList::all
```

The next request should be served from Redis.

### Update a product

```http
PUT {{baseUrl}}/api/products/1
```

Request body:

```json
{
  "productName": "Wireless Mechanical Keyboard",
  "price": 99.99
}
```

Expected result: `200 OK`.

The update operation:

- Updates PostgreSQL.
- Refreshes `products::1` using `@CachePut`.
- Evicts `productList::all`.

### Delete a product

```http
DELETE {{baseUrl}}/api/products/1
```

Expected result: `204 No Content`.

The delete operation evicts:

```text
products::1
productList::all
```

### Verify the deleted product

```http
GET {{baseUrl}}/api/products/1
```

Expected result: `404 Not Found`.

## 6. Redis cache verification

List product cache keys:

```bash
redis-cli SCAN 0 MATCH products::* COUNT 100
```

List product-list cache keys:

```bash
redis-cli SCAN 0 MATCH productList::* COUNT 100
```

Check whether a key exists:

```bash
redis-cli EXISTS products::1
```

Check the remaining TTL:

```bash
redis-cli TTL products::1
redis-cli TTL productList::all
```

Inspect the number of Redis keys:

```bash
redis-cli DBSIZE
```

Remove one demonstration key only:

```bash
redis-cli DEL products::1
```

Avoid using `FLUSHALL` or `FLUSHDB` against shared Redis environments.

## 7. Validation and error testing

Send an invalid request:

```http
POST {{baseUrl}}/api/products
```

```json
{
  "productName": "",
  "price": -10
}
```

Expected result: `400 Bad Request`.

Test a missing product:

```http
GET {{baseUrl}}/api/products/999
```

Expected result: `404 Not Found`.

Test an update for a missing product:

```http
PUT {{baseUrl}}/api/products/999
```

```json
{
  "productName": "Unknown Product",
  "price": 10.0
}
```

Expected result: `404 Not Found`; no incorrect cache entry is created.

## 8. Redis connection refused troubleshooting

If the application reports `RedisConnectionFailureException` or `Connection refused`, check:

```bash
docker ps
docker start redis-cache
redis-cli PING
```

Also verify that no other process is using the port:

```powershell
Test-NetConnection localhost -Port 6379
```

Confirm that the application properties match the Redis location:

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

Restart Spring Boot after changing the Redis configuration.

## 9. Recommended test order

1. Start PostgreSQL.
2. Start Redis.
3. Confirm Redis returns `PONG`.
4. Start Spring Boot.
5. Create a product.
6. Get the product twice to observe a miss and a hit.
7. Get all products twice to observe list caching.
8. Update the product and verify cache refresh.
9. Delete the product and verify cache eviction.
10. Request the deleted product and verify the `404` response.
