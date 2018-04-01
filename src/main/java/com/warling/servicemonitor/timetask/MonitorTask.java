package com.warling.servicemonitor.timetask;

import com.warling.servicemonitor.constant.Constant;
import com.warling.servicemonitor.utils.FTPUtils;
import com.warling.servicemonitor.utils.HttpUtil;
import com.warling.servicemonitor.utils.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.TimerTask;

public class MonitorTask extends TimerTask {

	/**
	 * 日志对象
	 */
	protected static Logger logger = LoggerFactory.getLogger(MonitorTask.class);

	private String url;
	private String service;
	private String start;
	private int checkTimes;

	public MonitorTask(String url, String service, String start, int checkTimes) {
		this.url = url;
		this.service = service;
		this.start = start;
		this.checkTimes = checkTimes;
	}

	@Override
	public void run() {
		if ("1".equals(Constant.CHECK_TYPE)) {
			checkHttp(checkTimes);
		} else if ("2".equals(Constant.CHECK_TYPE)) {
			checkFtp(Constant.CHECKTIMES);
		}
	}

	/**
	 * 检查ftp服务
	 * @param times 检查的次数
	 */
	private void checkFtp(int times) {
		boolean reTry = false;
		try {
			FTPUtils fu = new FTPUtils(true);
			boolean connect = fu.connect(Constant.FTP_HOST, Constant.FTP_PORT, Constant.FTP_USER_NAME, Constant.FTP_PASSWORD);
			if (connect) {
				fu.disconnect();
				logger.info("ftp服务运行正常");
				return;
			} else {
				reTry = true;
			}
		} catch (Exception e) {
			logger.error("请求ftp发生异常", e);
			reTry = true;
		}
		if (reTry && times > 1) {
			checkFtp(--times);
		} else {
			restartService();
		}
	}

	/**
	 * 检查http服务
	 * @param times 检查的次数
	 */
	public void checkHttp(int times) {
		boolean reTry = false;
		try {
			Integer responseCode = HttpUtil.getResponseCode(url);
			if (null == responseCode || responseCode >= 400) {
				reTry = true;
			} else {
				return;
			}
		} catch (Exception e) {
			logger.error("请求url发生异常", e);
			reTry = true;
		}
		if (reTry && times > 1) {
			checkHttp(--times);
		} else {
			restartService();
		}
	}

	/**
	 * 检查进程服务
	 * @param times 检查的次数
	 */
	public void restartService() {
		try {
			String sep = "\\|";
			Runtime rt = Runtime.getRuntime();
			// 杀死存在的进程
			if (StringUtils.isNotBlank(service)) {
				String stopCmd = Constant.STOP_CMD;
				String[] stopCmds = null;
				if (StringUtils.isNotBlank(stopCmd)) {
					stopCmds = stopCmd.split(sep);
				}
				int i = 0;
				for (String item : service.split(sep)) {
					if (StringUtils.isBlank(item)) {
						continue;
					}
		//			Process p = rt.exec("tasklist /FI \"username eq administrator\" | find /C \""+service+"\"");
		//			Process p = rt.exec("tasklist | find /C \""+service+"\"");
					String command = "tasklist /nh /FI \"IMAGENAME eq " + item + "\"";
					Process p = rt.exec(command);
		//			int value = p.exitValue();
		//			System.out.println("exitValue:"+value);

					int exitVal = p.waitFor();
					String result = StringUtil.inputream2String(p.getInputStream(), "gbk");
					logger.info(result);
					if (-1 != result.indexOf(item)) {
						String exec = null;
						if (null == stopCmds) {
							exec = "taskkill /f /IM \"" + item + "\"";
						} else {
							exec = stopCmds[i];
						}
						logger.info(exec);
						p = rt.exec(exec);
						log(p);
						logger.info("杀死" + item + "进程");
						p.waitFor();
					}
				}
			}

			// 启动服务
			String startCmd = Constant.START_CMD;
			String[] startCmds = null;
			if (StringUtils.isBlank(startCmd)) {
				if (StringUtils.isNotBlank(start)) {
					for (String item : start.split(sep)) {
						if (StringUtils.isBlank(item)) {
							continue;
						}
						logger.info(item);
						Process p = rt.exec(item);
						log(p);
						logger.info("启动服务" + item);
					}
				}
			} else {
				startCmds = startCmd.split(sep);
				for (String cmd : startCmds) {
					if (StringUtils.isBlank(cmd)) {
						continue;
					}
					logger.info(cmd);
					Process p = rt.exec(cmd);
					log(p);
				}
			}

		} catch (Exception e) {
			logger.error("启动服务发生异常", e);
		}
	}

	public void log(Process p) throws IOException {
		String result = StringUtil.inputream2String(p.getInputStream(), "gbk");
		logger.info(result);
	}

}
