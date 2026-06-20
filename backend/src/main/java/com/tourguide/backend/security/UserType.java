package com.tourguide.backend.security;

/** Which population a token/principal belongs to. */
public enum UserType {
    /** Mini-program user (tourist/guide), authenticated via WeChat. */
    APP,
    /** PC admin, authenticated via username/password. */
    ADMIN
}
