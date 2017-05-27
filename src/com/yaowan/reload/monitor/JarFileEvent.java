package com.yaowan.reload.monitor;

import java.util.EventObject;
import java.util.jar.JarFile;

/**
 * jar文件事件
 * 
 * @author Alias
 *
 * @date 2017年5月27日 下午3:14:15
 */
public class JarFileEvent extends EventObject {

	private static final long serialVersionUID = -7809367345460212417L;

	private final String entryName;

	public JarFileEvent(JarFile file, String entryName) {
		super(file);
		this.entryName = entryName;
	}

	@Override
	public JarFile getSource() {
		return (JarFile) super.getSource();
	}

	public String getEntryName() {
		return entryName;
	}

}
