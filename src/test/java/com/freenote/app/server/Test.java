package com.freenote.app.server;

public class Test {
    public static void main(String[] args) {
        String s = "5125125";
        int result = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                result = result * 10 + (c - '0');
            } else {
                System.out.println("Invalid character: " + c);
                return;
            }
        }
        int reversedResult = 0;
        int n = 1;
        for (int i = s.length() - 1; i >= 0; i--) {
            char c = s.charAt(i);

            int temp = (c - '0') * n + reversedResult;
            n *= 10;
            reversedResult = temp;
        }
        System.out.println("Parsed integer: " + result);
        System.out.println("Parsed integer reversed: " + reversedResult);
    }
}
