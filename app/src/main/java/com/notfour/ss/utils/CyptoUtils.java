package com.notfour.ss.utils;

import brookandroid.Brookandroid;

/**
 * Created with author.
 * Description:
 * Date: 2018-07-03
 * Time: 下午6:42
 */
public class CyptoUtils {
    public static final String key = "aljgla.mgh98570fdg;ghjksirl76jnf";

    public static String decode(String text) {
        return Brookandroid.aesDecrypt(text, key);
    }
}
