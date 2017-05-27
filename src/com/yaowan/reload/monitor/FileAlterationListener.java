package com.yaowan.reload.monitor;

/**
 * 文件修改监听器
 * 
 * @author Alias
 *
 * @date 2017年5月27日 下午2:45:55
 */
public interface FileAlterationListener {

	/**
	 * 增加文件
	 * 
	 * @param event
	 */
	void onAdd(ClassFileEvent event);

	/**
	 * 删除文件
	 * 
	 * @param event
	 */
	void onDelete(ClassFileEvent event);

	/**
	 * 修改文件
	 * 
	 * @param event
	 */
	void onAlteration(ClassFileEvent event);

	/**
	 * 修改jar文件
	 * 
	 * @param event
	 */
	void onJarAlteration(JarFileEvent event);

}
