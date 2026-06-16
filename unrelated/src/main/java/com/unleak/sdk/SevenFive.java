package com.unleak.sdk;

public class SevenFive {
//    Given a string s and an integer k, return the maximum number of vowel letters in any substring of s with length k.
//
//    Vowel letters in English are 'a', 'e', 'i', 'o', and 'u'.
//
//
//
//    Example 1:
//
//    Input: s = "abciiidef", k = 3
//    Output: 3
//    Explanation: The substring "iii" contains 3 vowel letters.
//    Example 2:
//
//    Input: s = "aeiou", k = 2
//    Output: 2
//    Explanation: Any substring of length 2 contains 2 vowels.
//            Example 3:
//
//    Input: s = "leetcode", k = 3
//    Output: 2
//    Explanation: "lee", "eet" and "ode" contain 2 vowels.

    public static void main(String[] args) {
        String s = "leetcode";
        int k = 3;
        int result = -1;
        int count = 0;
        for (int i = 0; i < k; i++) {
            var c = s.charAt(i);
            if ((c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u')) {
                count ++;
            }
            else {
                result = Math.max(result, count);
                count = 0;
            }
        }
        for (int i = k; i < s.length(); i++) {
            var c = s.charAt(i);
            if ((c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u') ) {
                count ++;
            }
            var c2 = s.charAt(i - k);

            if ((c2 == 'a' || c2 == 'e' || c2 == 'i' || c2 == 'o' || c2 == 'u') ) {
                count --;
            }

            result = Math.max(result, count);
        }
    }
}
