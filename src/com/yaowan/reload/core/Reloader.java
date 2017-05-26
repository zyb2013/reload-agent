package com.yaowan.reload.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.yaowan.reload.monitor.FileEvent;
import com.yaowan.reload.monitor.FileModifiedListener;
import com.yaowan.reload.monitor.FileMonitor;
import com.yaowan.reload.monitor.JarEvent;
import com.yaowan.reload.monitor.JarModifiedListener;
import com.yaowan.reload.monitor.JarMonitor;

/**
 * 加载器
 * 
 * @author Alias
 *
 * @date 2017年5月26日 下午5:33:15
 */
public class Reloader implements FileModifiedListener, JarModifiedListener {

	private static final int MONITOR_PERIOD_MIN_VALUE = 500;
	
	private static final Logger log = Logger.getLogger(Reloader.class.getName());
	
	private final Instrumentation inst;
	
	private final String classFolder;
	
	private final String jarFolder;
	
	private final ScheduledExecutorService service;

	private static List<Reloader> loaders = new ArrayList<>();

	public static void premain(String agentArgs, Instrumentation inst) {
		initialize(agentArgs, inst);
	}

	public static void agentmain(String agentArgs, Instrumentation inst) {
		initialize(agentArgs, inst);
	}

	private static void initialize(String agentArgs, Instrumentation inst) {
		Args args = new Args(agentArgs);
		if (!args.isValid()) {
			throw new RuntimeException("Your parameters are invalid! Check the documentation for the correct syntax");
		}
		Reloader reloader = new Reloader(inst, args);
		loaders.add(reloader);
	}

	public static void stopAll() {
		for (Reloader reloader : loaders) {
			reloader.stop();
		}
	}

	public Reloader(Instrumentation inst, Args args) {
		this.inst = inst;
		this.classFolder = args.getClassFolder();
		this.jarFolder = args.getJarFolder();
		int monitorPeriod = MONITOR_PERIOD_MIN_VALUE;
		if (args.getInterval() > monitorPeriod) {
			monitorPeriod = args.getInterval();
		}
		log.setUseParentHandlers(false);
		log.setLevel(args.getLogLevel());
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(args.getLogLevel());
		log.addHandler(consoleHandler);

		service = Executors.newScheduledThreadPool(2);

		FileMonitor fileMonitor = new FileMonitor(classFolder, "class");
		fileMonitor.addModifiedListener(this);
		service.scheduleWithFixedDelay(fileMonitor, 0, monitorPeriod, TimeUnit.MILLISECONDS);

		if (jarFolder != null) {
			JarMonitor jarMonitor = new JarMonitor(jarFolder);
			jarMonitor.addJarModifiedListener(this);
			service.scheduleWithFixedDelay(jarMonitor, 0, monitorPeriod, TimeUnit.MILLISECONDS);
		}

		log.info("watching class folder: " + classFolder);
		log.info("watching jars folder: " + jarFolder);
		log.info("scan interval (ms): " + monitorPeriod);
		log.info("log level: " + log.getLevel());
	}

	public void stop() {
		service.shutdown();
	}

	@Override
	public void fileModified(FileEvent event) {
		reloadClass(getClassName(event.getSource()), event);
	}

	@Override
	public void jarModified(JarEvent event) {
		reloadClass(getClassName(event.getEntryName()), event);
	}

	/**
	 * 重新加载class文件
	 * 
	 * @param className
	 * @param event
	 */
	protected void reloadClass(String className, EventObject event) {
		Class<?>[] loadedClasses = inst.getAllLoadedClasses();
		log.log(Level.FINE, "jvm loaded class size:" + loadedClasses.length);
		for (Class<?> clazz : loadedClasses) {
			if (clazz.getName().equals(className)) {
				try {
					ClassDefinition definition = new ClassDefinition(clazz, getByteArray(event));
					inst.redefineClasses(new ClassDefinition[] { definition });
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, "Reload class: " + clazz.getName());
					}
				} catch (Exception e) {
					log.log(Level.SEVERE, "error", e);
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 返回改变的class文件的字节数组
	 * 
	 * @param event
	 * @return
	 * @throws IOException
	 */
	private byte[] getByteArray(EventObject event) throws IOException {
		if (event instanceof FileEvent) {
			return toByteArray(new FileInputStream(new File(classFolder + event.getSource())));
		} else if (event instanceof JarEvent) {
			JarEvent jarEvent = (JarEvent) event;
			JarFile jar = jarEvent.getSource();
			return toByteArray(jar.getInputStream(getJarEntry(jar, jarEvent.getEntryName())));
		}
		throw new IllegalArgumentException("Event of type " + event.getClass().getName() + " is not supported");
	}

	/**
	 * 转换成类的全路径名称
	 * 
	 * @param fileName
	 * @return
	 */
	private String getClassName(String fileName) {
		return fileName.replace(".class", "").replace('/', '.');
	}

	/**
	 * 获取jar文件中特定的条目
	 * 
	 * @param jar
	 * @param entryName
	 * @return
	 */
	private JarEntry getJarEntry(JarFile jar, String entryName) {
		JarEntry entry = null;
		for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();) {
			entry = entries.nextElement();
			if (entry.getName().equals(entryName)) {
				return entry;
			}
		}
		throw new IllegalArgumentException("EntryName " + entryName + " does not exist in jar " + jar);
	}

	private byte[] toByteArray(InputStream is) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int bytesRead = 0;
		while ((bytesRead = is.read(buffer)) != -1) {
			byte[] tmp = new byte[bytesRead];
			System.arraycopy(buffer, 0, tmp, 0, bytesRead);
			baos.write(tmp);
		}
		byte[] result = baos.toByteArray();
		baos.close();
		is.close();
		return result;
	}
}
