package com.yaowan.reload.monitor;

/**
 * 方便子类重写自己感兴趣的方法
 * 
 * @author Alias
 *
 * @date 2017年5月27日 下午2:50:21
 */
public class FileAlterationListenerAdaptor implements FileAlterationListener {

	@Override
	public void onAdd(ClassFileEvent event) {

	}

	@Override
	public void onDelete(ClassFileEvent event) {

	}

	@Override
	public void onAlteration(ClassFileEvent event) {

	}

	@Override
	public void onJarAlteration(JarFileEvent event) {

	}

}
