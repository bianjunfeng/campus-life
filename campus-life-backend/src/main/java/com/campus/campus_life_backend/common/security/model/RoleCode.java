package com.campus.campus_life_backend.common.security.model;

public enum RoleCode {
    GUEST(-1, "ROLE_GUEST"),
    STUDENT(0, "ROLE_STUDENT"),
    MERCHANT(1, "ROLE_MERCHANT"),
    ADMIN(2, "ROLE_ADMIN");

    private final int dbValue;
    private final String authority;

    RoleCode(int dbValue, String authority) {
        this.dbValue = dbValue;
        this.authority = authority;
    }

    public int getDbValue() {
        return dbValue;
    }

    public String authority() {
        return authority;
    }

    public static RoleCode fromDbRole(Integer dbRole) {
        if (dbRole == null) {
            return GUEST;
        }
        for (RoleCode roleCode : values()) {
            if (roleCode.dbValue == dbRole) {
                return roleCode;
            }
        }
        return GUEST;
    }
}
