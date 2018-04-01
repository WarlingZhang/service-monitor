package com.warling.servicemonitor.constant;

import com.warling.servicemonitor.utils.IniReader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constant {

    /**
     * 日志对象
     */
    protected static Logger logger = LoggerFactory.getLogger(Constant.class);

    /**
     * 检查类型 1 http 2 ftp
     */
    public static String CHECK_TYPE;

    /**
     * 测试http服务是否正常的url地址
     */
    public static String HTTP_URL;

    /**
     * 测试ftp服务是否正常的地址
     */
    public static String FTP_HOST;

    /**
     * ftp服务端口
     */
    public static Integer FTP_PORT;

    /**
     * ftp服务用户名
     */
    public static String FTP_USER_NAME;

    /**
     * ftp服务密码
     */
    public static String FTP_PASSWORD;

    /**
     * 进程名
     */
    public static String PROCESS;

    /**
     * 服务的绝对路径
     */
    public static String START;

    /**
     * 服务的绝对路径
     */
    public static String START_CMD;

    /**
     * 服务的绝对路径
     */
    public static String STOP_CMD;

    /**
     * 间隔时间 间隔多长时间检查一次
     */
    public static Long PERIOD;

    /**
     * 检查次数 检查几次失败重启服务
     */
    public static int CHECKTIMES;


    static {
        try {
            // 读取配置文件
            IniReader iniReader = new IniReader();
            // 获取要检测的url
            CHECK_TYPE = iniReader.getValue("checkType");
            HTTP_URL = iniReader.getValue("http.url");
            FTP_HOST = iniReader.getValue("ftp.host");
            String ftpPort = iniReader.getValue("ftp.port");
            if (StringUtils.isNotBlank(ftpPort)) {
                FTP_PORT = NumberUtils.toInt(ftpPort.trim());
            }
            FTP_USER_NAME = iniReader.getValue("ftp.username");
            FTP_PASSWORD = iniReader.getValue("ftp.password");
            // 需要重启的进程名字
            PROCESS = iniReader.getValue("process");
            START = iniReader.getValue("start");
            START_CMD = iniReader.getValue("startCmd");
            STOP_CMD = iniReader.getValue("stopCmd");
            String periodStr = iniReader.getValue("period");
            if (StringUtils.isNotBlank(periodStr)) {
                PERIOD = NumberUtils.toLong(periodStr);
            }
            String checkTimesStr = iniReader.getValue("checkTimes");
            if (StringUtils.isNotBlank(checkTimesStr)) {
                CHECKTIMES = NumberUtils.toInt(checkTimesStr);
            }
        } catch (Exception e) {
            logger.error("初始化配置失败", e);
        }
    }



}
