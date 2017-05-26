package com.yaowan.reload.monitor;

/**
 * jar文件修改监听器
 * 
 * @author Alias
 *
 */
public interface JarModifiedListener {

	void jarModified(JarEvent event);

}
