package com.warling.servicemonitor.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

@SuppressWarnings("rawtypes")
public class IniReader {
	protected HashMap sections = new HashMap();
	private transient String currentSecion;
	private transient Properties current;
	
	private static String GLOBLE = "GLOBLE";

	private static String INIT_FILE = "conf.ini";
	
	/**
	 * 构造函数
	 * @param filename
	 * @throws IOException
	 */
	public IniReader(String filename) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		read(reader);
		reader.close();
	}
	
	public IniReader() throws IOException {
		String dir = System.getProperty("user.dir")+File.separator;
		BufferedReader reader = new BufferedReader(new FileReader(dir+INIT_FILE));
		read(reader);
		reader.close();
	}
	
    /**
     * 读取文件
     * @param reader
     * @throws IOException
     */
	protected void read(BufferedReader reader) throws IOException {
		String line;
		while ((line = reader.readLine()) != null) {
			parseLine(line);
		}
	}

	/**
	 * 解析配置文件行
	 * @param line
	 */
	@SuppressWarnings("unchecked")
	protected void parseLine(String line) {
		line = line.trim();
		if (line.matches("\\[.*\\]")) {
			currentSecion = line.replaceFirst("\\[(.*)\\]", "$1");
			current = new Properties();
			sections.put(currentSecion, current);
		} else if (line.matches(".*=.*")) {
			if (current != null) {
				int i = line.indexOf('=');
				String name = line.substring(0, i);
				String value = line.substring(i + 1);
				current.setProperty(name, value);
			} else {
				current = new Properties();
				sections.put(GLOBLE, current);
				int i = line.indexOf('=');
				String name = line.substring(0, i);
				String value = line.substring(i + 1);
				current.setProperty(name, value);
			}
		}
	}
	
	/**
	 * 获取值
	 * @param section
	 * @param name
	 * @return
	 */
	public String getValue(String section, String name) {
		Properties p = (Properties) sections.get(section);
		if (p == null) {
			return null;
		}
		String value = p.getProperty(name);
		return value;
	}
	/**
	 * 获取值
	 * @param name
	 * @return
	 */
	public String getValue(String name) {
		Properties p = (Properties) sections.get(GLOBLE);
		if (p == null) {
			return null;
		}
		String value = p.getProperty(name);
		return value;
	}
	
    /**
     * 是否包含key
     * @param section
     * @param key
     * @return
     */
	public boolean containsKey(String section,String key) {
		Properties p = (Properties) sections.get(section);
		return p.containsKey(key);
	}

	/**
	 * 是否包含key
	 * @param key
	 * @return
	 */
	public boolean containsKey(String key) {
		Properties p = (Properties) sections.get(GLOBLE);
		return p.containsKey(key);
	}
}