package com.nikyfarm.security;

/** Verified identity claims extracted from a Google ID token. */
public record GoogleUserInfo(String googleId, String email, String name, String pictureUrl) {}