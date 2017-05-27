package com.yaowan.reload.core;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * 启动参数
 * 
 * <pre>
 * 在程序启动时，命令行指定参数。
 * 例：
 * -javaagent:${path}/reload-agent-1.0.jar="classes=${classPath}, jars=${jarPath}, interval=1000, logLevel=FINE"
 * reload-agent-1.0.jar就是本项目的jar文件
 * classPath：用于指定要扫描的class文件所在的目录
 * jarPath：用于指定要扫描的jar文件所在的目录
 * interval：扫描文件的时间间隔
 * logLevel：日志级别，级别参照JDK的java.util.logging.Level
 * </pre>
 * 
 * @author zhuyuanbiao
 *
 * @date 2017年5月26日 下午7:42:19
 */
public class StartupArgs {

	/** class文件的存放路径 */
	private static final String CLASSES_PATH = "classPath";

	/** jar文件的存放路径 */
	private static final String JARS_PATH = "jarPath";

	/** 扫描间隔时间 */
	private static final String SCAN_INTERVAL = "interval";

	/** 日志级别 */
	private static final String LOG_LEVEL = "logLevel";

	private String classPath;

	private String jarPath;

	private int interval;

	private Level logLevel;

	private StartupArgs() {
		this.classPath = null;
		this.jarPath = null;
		this.interval = -1;
		this.logLevel = Level.WARNING;
	}

	public StartupArgs(String agentArgs) {
		this();
		if (agentArgs != null && agentArgs.length() > 0) {
			if (agentArgs.indexOf("=") != -1) {
				initWithNamedArgs(agentArgs);
			} else {
				initOldArgs(agentArgs);
			}
		}
	}

	public StartupArgs(String classFolder, String jarFolder, int period, String logLevel) {
		this();
		setClassFolder(classFolder);
		setJarFolder(jarFolder);
		setLogLevel(logLevel);
		this.interval = period;
	}

	public String getClassFolder() {
		return classPath;
	}

	public String getJarFolder() {
		return jarPath;
	}

	public Level getLogLevel() {
		return logLevel;
	}

	public int getInterval() {
		return interval;
	}

	private void initOldArgs(String agentArgs) {
		String[] args = agentArgs.split(",");
		setClassFolder(args[0]);

		if (args.length > 1) {
			setJarFolder(args[1]);
		}

		if (args.length > 2) {
			setInterval(args[2]);
		}

		if (args.length > 3) {
			setLogLevel(args[3]);
		}
	}

	private void initWithNamedArgs(String agentArgs) {
		String[] args = agentArgs.split(",");
		Map<String, String> argsMap = new HashMap<String, String>();
		for (String s : args) {
			String[] param = s.split("=");
			argsMap.put(param[0].trim(), param[1]);
		}

		if (argsMap.containsKey(CLASSES_PATH)) {
			setClassFolder(argsMap.get(CLASSES_PATH));
		}

		if (argsMap.containsKey(JARS_PATH)) {
			setJarFolder(argsMap.get(JARS_PATH));
		}

		if (argsMap.containsKey(SCAN_INTERVAL)) {
			setInterval(argsMap.get(SCAN_INTERVAL));
		}

		if (argsMap.containsKey(LOG_LEVEL)) {
			setLogLevel(argsMap.get(LOG_LEVEL));
		}

	}

	public boolean isValid() {
		return classPath != null;
	}

	private void setClassFolder(String classFolder) {
		this.classPath = parseFolderPath(classFolder);
	}

	private void setJarFolder(String jarFolder) {
		this.jarPath = parseFolderPath(jarFolder);
	}

	private void setLogLevel(String logLevel) {
		try {
			this.logLevel = Level.parse(logLevel.trim());
		} catch (Exception e) {
			this.logLevel = Level.WARNING;
		}
	}

	private void setInterval(String interval) {
		try {
			this.interval = Integer.parseInt(interval.trim());
		} catch (NumberFormatException e) {
			this.interval = -1;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(CLASSES_PATH).append("=").append(classPath);
		if (jarPath != null) {
			sb.append(",").append(JARS_PATH).append("=").append(jarPath);
		}
		sb.append(",").append(SCAN_INTERVAL).append("=").append(interval);
		sb.append(",").append(LOG_LEVEL).append("=").append(logLevel.toString());
		return sb.toString();
	}

	private static String parseFolderPath(String folder) {
		if (folder != null) {
			String trimmed = folder.trim();
			return trimmed.endsWith(File.separator) ? trimmed : trimmed + File.separator;
		}
		return null;
	}

}