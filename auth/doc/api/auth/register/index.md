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

| Field            | Type   | Required | Example                                           | Notes                 |
| ---------------- | ------ | -------- | ------------------------------------------------- | --------------------- |
| name             | string | Yes      | John Doe                                          | Full name of the user |
| email            | string | Yes      | [newuser@example.com](mailto:newuser@example.com) | Must be unique        |
| password         | string | Yes      | StrongPassword123                                 | Minimum 8 characters  |
| confirm_password | string | Yes      | StrongPassword123                                 | Must match `password` |

**Example Request**

```json
POST /auth/register
{
  "name": "John Doe",
  "email": "newuser@example.com",
  "password": "StrongPassword123",
  "confirm_password": "StrongPassword123"
}
```

### Response

#### Success (201 Created)

```json
{
  "message": "User registered successfully",
  "user": {
    "id": 124,
    "email": "newuser@example.com",
    "name": "John Doe"
  },
  "token": "eyJhbGciOiJIUzI1NiIsInR..."
}
```

#### Error Responses

* **400 Bad Request**

```json
{
  "error": "Email already exists"
}
```

* **422 Validation Error**

```json
{
  "error": "Password must be at least 8 characters"
}
```

---

## Notes & Constraints

* Store token securely (recommended: HttpOnly cookie).
* Passwords are never returned in responses.
* All error messages are standardized in JSON format.
