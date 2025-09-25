# API Documentation – auth/login

## Version History

| Version | Release Date | Changes & Updates | Author    |
| ------- | ------------ | ----------------- | --------- |
| 0.0.1   | 2025-09-25   | Initial draft     | L.H.Thinh |

---

## Overview

### Description

This document describes the authentication APIs used for user login.

### Endpoints Covered

* `POST /auth/login`

---

## Common Requirements

### Headers

| Key           | Value                | Required | Notes                         |
| ------------- | -------------------- | -------- | ----------------------------- |
| Content-Type  | application/json     | Yes      | All requests must be JSON     |
| Authorization | Bearer `<JWT_TOKEN>` | Optional | Required for protected routes |

---

## Endpoint: `POST /auth/login`

### Description

Authenticate a user with email and password. Returns an access token and user details.

### Request

#### Body Parameters

| Field    | Type   | Required | Example                                     | Notes                 |
| -------- | ------ | -------- | ------------------------------------------- | --------------------- |
| email    | string | Yes      | [user@example.com](mailto:user@example.com) | Must be a valid email |
| password | string | Yes      | StrongPassword123                           | Minimum 8 characters  |

**Example Request**

```json
POST /auth/login
{
  "email": "user@example.com",
  "password": "StrongPassword123"
}
```

### Response

#### Success (200 OK)

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR...",
  "expires_in": 3600,
  "user": {
    "id": 123,
    "email": "user@example.com",
    "name": "John Doe"
  }
}
```

#### Error Responses

* **401 Unauthorized**

```json
{
  "error": "Invalid email or password"
}
```

* **400 Bad Request**

```json
{
  "error": "Missing required fields"
}
```

## Notes & Constraints

* Store token securely (recommended: HttpOnly cookie).
* Passwords are never returned in responses.
* All error messages are standardized in JSON format.
