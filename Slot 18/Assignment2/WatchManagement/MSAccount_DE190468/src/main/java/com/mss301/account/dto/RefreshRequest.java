package com.mss301.account.dto;


// [TODO-3.4] Invalid/absent token is handled as an authentication failure.
public record RefreshRequest(String refreshToken) {}
