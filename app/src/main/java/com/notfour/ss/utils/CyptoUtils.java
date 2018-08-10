package com.notfour.ss.utils;

import androidlib.Androidlib;

/**
 * Created with author.
 * Description:
 * Date: 2018-07-03
 * Time: 下午6:42
 */
public class CyptoUtils {
    public static String decode(String text) {
        return Androidlib.aesDecrypt(text);
    }
}
