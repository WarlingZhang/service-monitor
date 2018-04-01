package com.warling.servicemonitor;

import com.warling.servicemonitor.timetask.MonitorTask;
import com.warling.servicemonitor.utils.IniReader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class Main {

	/**
	 * 日志对象
	 */
	protected static Logger logger = LoggerFactory.getLogger(Main.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		try {
			// 读取配置文件
			IniReader iniReader = new IniReader();
			// 获取要检测的url
			String url = iniReader.getValue("url");
			// 需要重启的进程名字
			String process = iniReader.getValue("process");
			String start = iniReader.getValue("start");
			String periodStr = iniReader.getValue("period");
			String checkTimesStr = iniReader.getValue("checkTimes");
			long period = 30000l;
			if (StringUtils.isNotBlank(periodStr)) {
				period = Long.parseLong(periodStr);
			}
			int checkTimes = 3;
			if (StringUtils.isNotBlank(checkTimesStr)) {
				checkTimes = Integer.parseInt(checkTimesStr);
			}
			Timer timer = new Timer();
			TimerTask task = new MonitorTask(url, process, start, checkTimes);
			timer.schedule(task, 0, period);
		} catch (IOException e) {
			logger.error("程序发生异常", e);
		}
	}

}
