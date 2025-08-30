package com.pfh.user.service;

import com.pfh.user.dto.RegistrationRequest;
import com.pfh.user.dto.RegistrationResponse;

public interface UserService {
    RegistrationResponse registerUser(RegistrationRequest request);
}
