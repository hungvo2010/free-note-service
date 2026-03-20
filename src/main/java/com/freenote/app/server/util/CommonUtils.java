package com.freenote.app.server.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CommonUtils {
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }
}
