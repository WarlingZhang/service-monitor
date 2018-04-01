package com.warling.servicemonitor.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtil {

	/**
	 * 日志对象
	 */
	protected static Logger logger = LoggerFactory.getLogger(HttpUtil.class);

	/**
	 *
	 * @param httpUrl
	 * @param params
	 * @param method
	 * @return
	 * @throws Exception
	 */
	public static Map<String, Object> http(String httpUrl, Object params, String method) throws Exception{
		return http(httpUrl, params, method, true, null);
	}

	/**
	 *
	 * @param httpUrl
	 * @param params
	 * @param method
	 * @return
	 * @throws Exception
	 */
	public static Map<String, Object> httpNotBody(String httpUrl, Object params, String method) throws Exception{
		return http(httpUrl, params, method, false, null);
	}

	/**
	 *
	 * @param httpUrl
	 * @param params
	 * @param method
	 * @return
	 * @throws Exception
	 */
	public static String httpBody(String httpUrl, Object params, String method) throws Exception{
		Map<String, Object> http = http(httpUrl, params, method);
		return (String) http.get("responseBody");
	}

	/**
	 *
	 * @param httpUrl
	 * @param params
	 * @param method
	 * @param getResult
	 * @param headers
	 * @return
	 * @throws Exception
	 */
	public static Map<String, Object> http(String httpUrl, Object params, String method,
							boolean getResult, String... headers) throws Exception{
		Map<String, Object> result = new HashMap<>();
		URL url = null;
		HttpURLConnection http = null;
		try {
		    url = new URL(httpUrl);
		    http = (HttpURLConnection) url.openConnection();
		    http.setDoInput(true);
		    http.setDoOutput(true);
		    http.setUseCaches(false);
		    http.setConnectTimeout(50000);//设置连接超时
		    //如果在建立连接之前超时期满，则会引发一个 java.net.SocketTimeoutException。超时时间为零表示无穷大超时。
		    http.setReadTimeout(50000);//设置读取超时
		    //如果在数据可读取之前超时期满，则会引发一个 java.net.SocketTimeoutException。超时时间为零表示无穷大超时。
		    http.setRequestMethod(method);
		    // http.setRequestProperty("Content-Type","text/xml; charset=UTF-8");
			if (null != params) {
				http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				http.connect();
				String json = JSON.toJSONString(params);
				OutputStreamWriter osw = new OutputStreamWriter(http.getOutputStream(), "utf-8");
				osw.write("json=" + json);
				osw.flush();
				osw.close();
			}

			int respCode = http.getResponseCode();
			logger.info("http "+method+" "+ respCode+" "+httpUrl);
			StringBuilder resultStr = new StringBuilder();
			result.put("responseCode", respCode);
			if (getResult) {
				BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream(), "utf-8"));
				String inputLine = null;
				while ((inputLine = in.readLine()) != null) {
					resultStr.append(inputLine);
				}
				in.close();
				result.put("responseBody", resultStr.toString());
			}
			if (null != headers && headers.length >= 1) {
				for (String header : headers) {
					Map<String, List<String>> headerFields = http.getHeaderFields();
					result.put(header, headerFields.get(header));
				}
			}
			return result;
		} catch (Exception e) {
			throw e;
		} finally {
		    if (http != null) http.disconnect();
		}
	}

	public static String post(String httpUrl, Object params) throws Exception {
		return httpBody(httpUrl, params, "POST");
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> post2Map(String httpUrl, Object params) throws Exception {
		String responseText = post(httpUrl, params);
		return JSON.parseObject(responseText, Map.class);
	}

	public static String get(String httpUrl, Object params) throws Exception {
		return httpBody(httpUrl, params, "GET");
	}

	public static String get(String httpUrl) throws Exception {
		return get(httpUrl, null);
	}

	public static Integer getResponseCode(String httpUrl) throws Exception {
		Map<String, Object> map = httpNotBody(httpUrl, null, "GET");
		return (Integer) map.get("responseCode");
	}


}
