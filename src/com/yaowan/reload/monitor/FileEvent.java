package com.yaowan.reload.monitor;

import java.io.File;
import java.util.EventObject;

/**
 * 事件
 * 
 * @author Alias
 *
 */
public class FileEvent extends EventObject {

	private static final long serialVersionUID = 4696923746078504205L;

	public FileEvent(String path, String basePath) {
		super(path.replace(basePath + File.separator, ""));
	}

	@Override
	public String getSource() {
		return (String) super.getSource();
	}

}
