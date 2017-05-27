package com.yaowan.reload.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.yaowan.reload.monitor.FileAlterationListenerAdaptor;
import com.yaowan.reload.monitor.FileAlterationMonitor;
import com.yaowan.reload.monitor.ClassFileEvent;
import com.yaowan.reload.monitor.JarFileEvent;
import com.yaowan.reload.monitor.JarFileMonitor;

/**
 * 加载器
 * 
 * @author Alias
 *
 * @date 2017年5月26日 下午5:33:15
 */
public class Reloader extends FileAlterationListenerAdaptor {

	/** 默认扫描间隔时间 */
	private static final int DEFAULT_SCAN_INTERVAL = 500;

	private static final Logger log = Logger.getLogger(Reloader.class.getName());

	private final Instrumentation instrumentation;

	/** 要监视的class文件路径 */
	private final String classPath;

	/** 要监视的jar文件路径 */
	private final String jarPath;

	/** 监视线程池 */
	private final ScheduledExecutorService executor;

	/** 存放已被JVM加载的类 */
	private static Map<String, Class<?>> loadedClasses = new ConcurrentHashMap<>();

	private volatile boolean loaded;

	public static void premain(String agentArgs, Instrumentation inst) {
		init(agentArgs, inst);
	}

	public static void agentmain(String agentArgs, Instrumentation inst) {
		init(agentArgs, inst);
	}

	private static void init(String agentArgs, Instrumentation inst) {
		Args args = new Args(agentArgs);
		if (!args.isValid()) {
			throw new RuntimeException("args is invalid");
		}
		new Reloader(inst, args);
	}

	public static void stop() {
		// reloader.stop();
		// executor.shutdown();
	}

	public Reloader(Instrumentation inst, Args args) {
		this.instrumentation = inst;
		this.classPath = args.getClassFolder();
		this.jarPath = args.getJarFolder();
		int scanInterval = DEFAULT_SCAN_INTERVAL;
		if (args.getInterval() > scanInterval) {
			scanInterval = args.getInterval();
		}
		log.setUseParentHandlers(false);
		log.setLevel(args.getLogLevel());
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(args.getLogLevel());
		log.addHandler(consoleHandler);

		executor = Executors.newScheduledThreadPool(2);

		FileAlterationMonitor fileMonitor = new FileAlterationMonitor(classPath, "class");
		fileMonitor.addListener(this);
		executor.scheduleWithFixedDelay(fileMonitor, 0, scanInterval, TimeUnit.MILLISECONDS);

		if (jarPath != null) {
			JarFileMonitor jarMonitor = new JarFileMonitor(jarPath);
			jarMonitor.addListener(this);
			executor.scheduleWithFixedDelay(jarMonitor, 0, scanInterval, TimeUnit.MILLISECONDS);
		}

		log.info("class path: " + classPath);
		log.info("jars path: " + jarPath);
		log.info("scan interval (ms): " + scanInterval);
		log.info("log level: " + log.getLevel());
	}

	private void initLoadedClasses() {
		Class<?>[] classes = instrumentation.getAllLoadedClasses();
		for (Class<?> clazz : classes) {
			loadedClasses.put(clazz.getName(), clazz);
		}
	}

	@Override
	public void onAlteration(ClassFileEvent event) {
		reloadClass(getClassName(event.getSource()), event);
	}

	@Override
	public void onJarAlteration(JarFileEvent event) {
		reloadClass(getClassName(event.getEntryName()), event);
	}

	/**
	 * 重新加载class文件
	 * 
	 * @param className
	 * @param event
	 */
	private void reloadClass(String className, EventObject event) {
		if (!loaded) {
			initLoadedClasses();
			loaded = true;
		}
		Class<?> loadedClass = loadedClasses.get(className);
		if (loadedClass == null) {
			return;
		}
		try {
			ClassDefinition definition = new ClassDefinition(loadedClass, getByteArray(event));
			instrumentation.redefineClasses(new ClassDefinition[] { definition });
			if (log.isLoggable(Level.FINE)) {
				// log.log(Level.FINE, "Reload class: " +
				// clazz.getName());
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "error", e);
			e.printStackTrace();
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
		if (event instanceof ClassFileEvent) {
			return getByteArray(new FileInputStream(new File(classPath + event.getSource())));
		} else if (event instanceof JarFileEvent) {
			JarFileEvent jarEvent = (JarFileEvent) event;
			JarFile jar = jarEvent.getSource();
			return getByteArray(jar.getInputStream(getJarEntry(jar, jarEvent.getEntryName())));
		}
		throw new RuntimeException("No such event:" + event.getClass().getName());
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
		for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();) {
			JarEntry entry = entries.nextElement();
			if (entry.getName().equals(entryName)) {
				return entry;
			}
		}
		throw new IllegalArgumentException("No such jar entry:" + entryName);
	}

	private byte[] getByteArray(InputStream in) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int bytesRead = 0;
		while ((bytesRead = in.read(buffer)) != -1) {
			byte[] tmp = new byte[bytesRead];
			System.arraycopy(buffer, 0, tmp, 0, bytesRead);
			baos.write(tmp);
		}
		byte[] result = baos.toByteArray();
		baos.close();
		in.close();
		return result;
	}
}
