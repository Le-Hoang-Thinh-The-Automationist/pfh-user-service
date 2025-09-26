# API Documentation – auth/register

## Version History

| Version | Release Date | Changes & Updates | Author    |
| ------- | ------------ | ----------------- | --------- |
| 0.0.1   | 2025-09-25   | Initial draft     | L.H.Thinh |

---

## Overview

### Description

This document describes the authentication APIs used for user login and registration.

### Endpoints Covered

* `POST /auth/register`

---

## Common Requirements

### Headers

| Key           | Value                | Required | Notes                         |
| ------------- | -------------------- | -------- | ----------------------------- |
| Content-Type  | application/json     | Yes      | All requests must be JSON     |
| Authorization | Bearer `<JWT_TOKEN>` | Optional | Required for protected routes |

---

## Endpoint: `POST /auth/register`

### Description

Register a new user account and return user details with access token.

### Request

#### Body Parameters

| Field            | Type   | Required | Example                                           | Notes                          |
| ---------------- | ------ | -------- | ------------------------------------------------- | ----------------------------   |
| email            | string | Yes      | [newuser@example.com](mailto:newuser@example.com) | Must be unique                 |
| password         | string | Yes      | StrongPassword123!                                | Minimum 12 characters, must contain at least one uppercase, lowercase, digit, and special character |
| confirmPassword  | string | Yes      | StrongPassword123!                                | Must match `password`          |

**Example Request**

```json
POST /auth/register
{
  "email": "newuser@example.com",
  "password": "StrongPassword123!",
  "confirmPassword": "StrongPassword123!"
}
```

### Response

#### Success (201 Created)

```json
{
  "message": "User registered successfully",
  "id": 124,
  "email": "newuser@example.com"
}
```

#### Error Responses

* **400 Bad Request**

```json
{
  "status": 400,
  "message": "<Overview error message, need to check 'errors' for each field's error detail>",
  "timestamp": "2025-09-26T09:25:43Z",
  "errors": [
    // When the json's format request is invalid
    {
      "field": "syntax",
      "message": "Malformed JSON"
    },
    // When email has invalid format
    {
      "field": "email",
      "message": "Invalid email format"
    },
    // When password does not follow the format
    {
      "field": "password",
      "message": "Password is too weak"
    },
    // When the confirm error does not match the password
    {
      "field": "confirmPassword",
      "message": "Passwords do not match"
    },
    ...
  ]
}
```
---

* **401 Unauthorized**

```json
{
  "status": 401,
  "message": "Invalid credentials",
  "timestamp": "2025-09-26T09:50:12Z",
  "errors": [
    {
      "field": "credential",
      "message": "Invalid credentials"
    }
  ]
}
```

---

* **409 Conflict**

```json
{
  "status": 409,
  "message": "Email already registered: newuser@example.com",
  "timestamp": "2025-09-26T11:24:55Z",
  "errors": [
    {
      "field": "email",
      "message": "Email already registered"
    }
  ]
}
```

---

* **415 Unsupported Media Type**

- When: This error happens because you request `Content-Type` that is NOT `application/json`

```json
{
  "error": "Media is not support"
}
```

---

## Notes & Constraints

* Store token securely (recommended: HttpOnly cookie).
* Passwords are never returned in responses.
* All error messages are standardized in JSON format.
