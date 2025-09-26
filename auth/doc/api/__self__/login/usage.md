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
  "email": "john.doe@example.com",
  "password": "StrongPassword123!"
}
```

### Response

#### Success (200 OK)

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR...",
  "message": "Login successful",
  "claims": {
    "roles": [
      "NORMAL_USER",
      ...
    ],
    "email": "john.doe@example.com"
  }
}
```

#### Error Responses

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

* **403 Forbidden**

```json
{
  "status": 403,
  "message": "<Overview error message, need to check 'errors' for each field's error detail>",
  "timestamp": "2025-09-26T11:24:55Z",
  "errors": [
    // Case 1: When the user's status acount is disabled
    {
      "field": "userStatus",
      "message": "Your account is disabled. Please contact support."
    },
    // Case 2: When the user's status acount is suspended
    {
      "field": "userStatus",
      "message": "Your account is suspended. Please contact support."
    },
  ]
}
```

## Notes & Constraints

* Store token securely (recommended: HttpOnly cookie).
* Passwords are never returned in responses.
* All error messages are standardized in JSON format.
