package com.yaowan.reload.monitor;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 监视文件的修改
 * 
 * @author Alias
 *
 * @date 2017年5月27日 下午2:42:38
 */
public class FileAlterationMonitor implements Runnable {

	private final File folder;

	private final ExtFilenameFilter filenameFilter;

	/** 文件后缀名称 */
	private final String postfixName;

	private final Map<String, Long> fileMap;

	private final List<FileAlterationListener> listeners;

	private final static Logger log = Logger.getLogger(FileAlterationMonitor.class.getName());

	class ExtFilenameFilter implements FilenameFilter {

		@Override
		public boolean accept(File folder, String name) {
			return name.endsWith(postfixName) || new File(folder.getAbsolutePath() + File.separator + name).isDirectory();
		}

	}

	public FileAlterationMonitor(String path, String postfixName) {
		this.postfixName = postfixName;
		this.filenameFilter = new ExtFilenameFilter();
		this.fileMap = new HashMap<>();
		this.listeners = new LinkedList<>();
		this.folder = new File(path);
		if (!folder.isAbsolute() || !folder.isDirectory()) {
			throw new IllegalArgumentException(path + " is not a folder.");
		}
	}

	@Override
	public void run() {
		scanDeletion();
		scanAlteration(folder);
	}

	protected void scanDeletion() {
		List<String> paths = new LinkedList<>();
		for (String path : fileMap.keySet()) {
			if (!new File(path).exists()) {
				paths.add(path);
				doDelete(new ClassFileEvent(path, folder.getAbsolutePath()));
			}
		}
		for (String path : paths) {
			fileMap.remove(path);
		}
	}

	protected void scanAlteration(File currentFolder) {
		for (File file : getFiles(currentFolder)) {
			if (file.isDirectory()) {
				scanAlteration(file);
			} else {
				if (fileMap.containsKey(file.getAbsolutePath())) {
					if (fileMap.get(file.getAbsolutePath()).longValue() != file.lastModified()) {
						fileMap.put(file.getAbsolutePath(), Long.valueOf(file.lastModified()));
						log.log(Level.FINE, file.getAbsolutePath() + " changed...");
						doModified(new ClassFileEvent(file.getAbsolutePath(), folder.getAbsolutePath()));
					}
				} else {
					fileMap.put(file.getAbsolutePath(), Long.valueOf(file.lastModified()));
					doAdd(new ClassFileEvent(file.getAbsolutePath(), folder.getAbsolutePath()));
				}
			}
		}
	}

	public File[] getFiles(File folder) {
		return folder.listFiles(filenameFilter);
	}

	public void addListener(FileAlterationListener listener) {
		listeners.add(listener);
	}

	private void doModified(ClassFileEvent event) {
		long start = System.currentTimeMillis();
		for (FileAlterationListener listener : listeners) {
			listener.onAlteration(event);
		}
		log.info("Reload finish, cost time:" + (System.currentTimeMillis() - start) + "ms...");
	}

	private void doAdd(ClassFileEvent event) {
		for (FileAlterationListener listener : listeners) {
			listener.onAdd(event);
		}
	}

	private void doDelete(ClassFileEvent event) {
		for (FileAlterationListener listener : listeners) {
			listener.onDelete(event);
		}
	}

}
