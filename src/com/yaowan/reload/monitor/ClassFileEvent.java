package com.yaowan.reload.monitor;

import java.io.File;
import java.util.EventObject;

/**
 * class文件事件
 * 
 * @author zhuyuanbiao
 *
 * @date 2017年5月27日 下午3:13:04
 */
public class ClassFileEvent extends EventObject {

	private static final long serialVersionUID = 4696923746078504205L;

	public ClassFileEvent(String path, String basePath) {
		super(path.replace(basePath + File.separator, ""));
	}

	@Override
	public String getSource() {
		return (String) super.getSource();
	}

}
