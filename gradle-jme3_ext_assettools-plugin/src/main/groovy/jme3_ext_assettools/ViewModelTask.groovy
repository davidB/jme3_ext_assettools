package jme3_ext_assettools

import java.nio.file.Files;

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*

class ViewModelTask extends DefaultTask {
	def URL assetCfg

	def assetDirs
	FileCollection getAssetDirs() {
		project.files(assetDirs)
	}
	def rpath
	String getRpath() {
		return rpath.toString()
	}
	def file
	File getFile() {
		(file == null) ? null : project.file(file)
	}
	def assetClassLoader

	@TaskAction
	def action() {
		ModelViewer viewer = new ModelViewer(assetCfg)
		if (assetClassLoader != null) viewer.addClassLoader(assetClassLoader);
		viewer.addAssetDirs(assetDirs)
		println("inRPath : ${rpath} / ${getRpath()}")
		println("inFile : ${file} / ${getFile()}")
		if (getRpath() != null) {
			viewer.showModel("fake", getRpath())
		} else {
			viewer.showModel("fake", getFile())
		}
		viewer.running.await()
	}
}
