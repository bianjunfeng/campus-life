package com.campus.campus_life_backend.common.security.annotation;

import com.campus.campus_life_backend.common.security.model.RoleCode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    RoleCode[] value();
}
