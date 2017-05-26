package com.yaowan.reload.monitor;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 文件监控器
 * 
 * @author Alias
 *
 */
public class FileMonitor implements Runnable {

	private final File folder;
	
	private final ExtFilenameFilter filenameFilter;
	
	private final String fileExtension;
	
	private final Map<String, Long> fileMap;
	
	private final List<FileAddedListener> fileAddedListeners;
	
	private final List<FileDeletedListener> fileDeletedListeners;
	
	private final List<FileModifiedListener> fileModifiedListeners;

	class ExtFilenameFilter implements FilenameFilter {

		@Override
		public boolean accept(File folder, String name) {
			return name.endsWith(fileExtension) || new File(folder.getAbsolutePath() + File.separator + name).isDirectory();
		}

	}

	public FileMonitor(String absoluteFolderPath, String fileExtension) {
		this.fileExtension = fileExtension;
		this.filenameFilter = new ExtFilenameFilter();
		this.fileMap = new HashMap<String, Long>();
		this.fileAddedListeners = new LinkedList<FileAddedListener>();
		this.fileDeletedListeners = new LinkedList<FileDeletedListener>();
		this.fileModifiedListeners = new LinkedList<FileModifiedListener>();
		this.folder = new File(absoluteFolderPath);

		if (!folder.isAbsolute() || !folder.isDirectory()) {
			throw new IllegalArgumentException("The parameter with value " + absoluteFolderPath + " MUST be a folder");
		}
	}

	@Override
	public void run() {
		checkDeletion();
		checkAddAndModify(folder);
	}

	protected void checkDeletion() {
		List<String> pathsToDelete = new LinkedList<String>();
		for (String path : fileMap.keySet()) {
			if (!new File(path).exists()) {
				pathsToDelete.add(path);
				notifyDeletedListeners(new FileEvent(path, folder.getAbsolutePath()));
			}
		}
		for (String path : pathsToDelete) {
			fileMap.remove(path);
		}
	}

	protected void checkAddAndModify(File currentFolder) {
		for (File file : getFiles(currentFolder)) {
			if (file.isDirectory()) {
				checkAddAndModify(file);
			} else {
				if (fileMap.containsKey(file.getAbsolutePath())) {
					if (fileMap.get(file.getAbsolutePath()).longValue() != file.lastModified()) {
						fileMap.put(file.getAbsolutePath(), Long.valueOf(file.lastModified()));
						System.out.println(file.getAbsolutePath() + " changed...");
						notifyModifiedListeners(new FileEvent(file.getAbsolutePath(), folder.getAbsolutePath()));
					}
				} else {
					fileMap.put(file.getAbsolutePath(), Long.valueOf(file.lastModified()));
					notifyAddedListeners(new FileEvent(file.getAbsolutePath(), folder.getAbsolutePath()));
				}
			}
		}
	}

	public File[] getFiles(File folder) {
		return folder.listFiles(filenameFilter);
	}

	public void addModifiedListener(FileModifiedListener listener) {
		fileModifiedListeners.add(listener);
	}

	public void addDeletedListener(FileDeletedListener listener) {
		fileDeletedListeners.add(listener);
	}

	public void addAddedListener(FileAddedListener listener) {
		fileAddedListeners.add(listener);
	}

	private void notifyModifiedListeners(FileEvent event) {
		for (FileModifiedListener listener : fileModifiedListeners) {
			listener.fileModified(event);
		}
	}

	private void notifyAddedListeners(FileEvent event) {
		for (FileAddedListener listener : fileAddedListeners) {
			listener.fileAdded(event);
		}
	}

	private void notifyDeletedListeners(FileEvent event) {
		for (FileDeletedListener listener : fileDeletedListeners) {
			listener.fileDeleted(event);
		}
	}

}
