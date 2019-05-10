package org.sonarqube.auth.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;

/**
 * Creator: ChangpingShi0213@gmail.com
 * Date:  20:25 2019/5/6
 * Description:
 */
public enum PermissionType {
    USER,
    SCAN,
    SECURITYHOTSPOTADMIN,
    ISSUEADMIN,
    CODEVIEWER,
    ADMIN;

    private static HashMap<String, PermissionType> valuesMap = new HashMap<>(6);

    static {
        PermissionType[] var0 = values();

        for (PermissionType status : var0) {
            valuesMap.put(status.toValue(), status);
        }

    }

    PermissionType() {
    }

    @JsonCreator
    public static PermissionType forValue(String value) {
        return valuesMap.get(value);
    }

    @JsonValue
    public String toValue() {
        return this.name().toLowerCase();
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
