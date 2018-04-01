package com.warling.servicemonitor.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by zwl on 2017/3/27.
 */
public class StringUtil {

    /**
     * inputstream转String
     * @param inputStream
     * @param charset
     * @return
     * @throws IOException
     */
    public static String inputream2String(InputStream inputStream, String charset) throws IOException {
        StringBuilder result = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, charset));
        String inputLine = null;
        while ((inputLine = in.readLine()) != null) {
            result.append(inputLine);
        }
        in.close();
        return result.toString();
    }

    /**
     * inputstream转String
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static String inputream2String(InputStream inputStream) throws IOException {
        return inputream2String(inputStream, "UTF-8");
    }
}
