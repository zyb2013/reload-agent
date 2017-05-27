package com.yaowan.reload.monitor;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * jar文件监视器
 * 
 * @author Alias
 *
 */
public class JarFileMonitor extends FileAlterationListenerAdaptor implements Runnable {

	private final static Logger log = Logger.getLogger(JarFileMonitor.class.getName());

	private final FileAlterationMonitor fileMonitor;
	private final String absoluteFolderPath;
	private final Map<String, Map<String, Long>> jarsMap;
	private final List<FileAlterationListener> jarModifiedListeners;

	public JarFileMonitor(String absoluteFolderPath) {
		this.absoluteFolderPath = absoluteFolderPath;
		this.jarsMap = new HashMap<String, Map<String, Long>>();
		this.jarModifiedListeners = new LinkedList<>();

		fileMonitor = new FileAlterationMonitor(absoluteFolderPath, "jar");
		fileMonitor.addListener(this);
	}

	@Override
	public void run() {
		fileMonitor.run();
	}

	@Override
	public void onAlteration(ClassFileEvent event) {
		JarFile file = getJarFile(event);
		if (file != null) {
			Map<String, Long> jarEntries = jarsMap.get(event.getSource());
			for (Enumeration<JarEntry> entries = file.entries(); entries.hasMoreElements();) {
				JarEntry entry = entries.nextElement();
				if (entry.getName().indexOf(".class") == -1) {
					continue;
				}
				if (!jarEntries.containsKey(entry.getName())) {
					jarEntries.put(entry.getName(), 0L);
				}
				if (entry.getTime() != jarEntries.get(entry.getName()).longValue()) {
					jarEntries.put(entry.getName(), Long.valueOf(entry.getTime()));
					doAlteration(new JarFileEvent(file, entry.getName()));
				}
			}
		}
	}

	@Override
	public void onAdd(ClassFileEvent event) {
		JarFile file = getJarFile(event);

		if (file != null) {
			Map<String, Long> jarEntries = new HashMap<String, Long>();
			jarsMap.put(event.getSource(), jarEntries);

			for (Enumeration<JarEntry> entries = file.entries(); entries.hasMoreElements();) {
				JarEntry entry = entries.nextElement();
				if (entry.getName().endsWith("jar")) {
					jarEntries.put(entry.getName(), Long.valueOf(entry.getTime()));
					doAlteration(new JarFileEvent(file, entry.getName()));
				}
			}
		}
	}

	@Override
	public void onDelete(ClassFileEvent event) {
		jarsMap.remove(event.getSource());
	}

	public void addListener(FileAlterationListener listener) {
		jarModifiedListeners.add(listener);
	}

	private void doAlteration(JarFileEvent event) {
		for (FileAlterationListener listener : jarModifiedListeners) {
			listener.onJarAlteration(event);
		}
	}

	private JarFile getJarFile(ClassFileEvent event) {
		try {
			return new JarFile(absoluteFolderPath + event.getSource());
		} catch (IOException e) {
			log.log(Level.SEVERE, "error", e);
			return null;
		}
	}

}
