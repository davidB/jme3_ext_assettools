package jme3_ext_assettools

import java.nio.file.Files;

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*

class ViewModelTask extends DefaultTask {
	def URL assetCfg
	def assetClassLoader
	def assetDirs
	FileCollection getAssetDirs() {
		(assetDirs == null) ? null : project.files(assetDirs)
	}

	def rpath
	String getRpath() {
		if (rpath == null) return null
		def v = (rpath instanceof Closure) ? rpath() : rpath
		v.toString()
	}
	def file
	File getFile() {
		(file == null) ? null : project.file(file)
	}

	@TaskAction
	def action() {
		println(">> inRPath : ${rpath} / ${getRpath()}")
		println(">> inFile : ${file} / ${getFile()}")
		if (getRpath() == null && getFile() == null) return;

		ModelViewer viewer = new ModelViewer(assetCfg)
		if (assetClassLoader != null) viewer.addClassLoader(assetClassLoader);
		viewer.addAssetDirs(getAssetDirs())
		if (getRpath() != null) {
			viewer.showModel("fake", getRpath())
		} else if (getFile() != null) {
			viewer.showModel("fake", getFile())
		}
		viewer.running.await()
	}
}
