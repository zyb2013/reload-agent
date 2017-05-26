package com.yaowan.reload.core;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * 启动参数
 * 
 * @author Alias
 *
 */
public class Args {

	/** class文件的存放路径 */
	private static final String CLASSES_PATH = "classes";

	/** jar文件的存放路径 */
	private static final String JARS_PATH = "jars";

	/** 扫描间隔时间 */
	private static final String SCAN_INTERVAL = "interval";

	/** 日志级别 */
	private static final String LOG_LEVEL = "logLevel";

	private String classFolder;

	private String jarFolder;

	private int interval;

	private Level logLevel;

	private Args() {
		this.classFolder = null;
		this.jarFolder = null;
		this.interval = -1;
		this.logLevel = Level.WARNING;
	}

	public Args(String agentArgs) {
		this();
		if (agentArgs != null && agentArgs.length() > 0) {
			if (agentArgs.indexOf("=") != -1) {
				initWithNamedArgs(agentArgs);
			} else {
				initOldArgs(agentArgs);
			}
		}
	}

	public Args(String classFolder, String jarFolder, int period, String logLevel) {
		this();
		setClassFolder(classFolder);
		setJarFolder(jarFolder);
		setLogLevel(logLevel);
		this.interval = period;
	}

	public String getClassFolder() {
		return classFolder;
	}

	public String getJarFolder() {
		return jarFolder;
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
		return classFolder != null;
	}

	private void setClassFolder(String classFolder) {
		this.classFolder = parseFolderPath(classFolder);
	}

	private void setJarFolder(String jarFolder) {
		this.jarFolder = parseFolderPath(jarFolder);
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
		sb.append(CLASSES_PATH).append("=").append(classFolder);
		if (jarFolder != null) {
			sb.append(",").append(JARS_PATH).append("=").append(jarFolder);
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