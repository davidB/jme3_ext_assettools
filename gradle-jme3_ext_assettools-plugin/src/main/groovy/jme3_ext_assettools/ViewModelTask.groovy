package jme3_ext_assettools

import java.nio.file.Files;

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.*

class ViewModelTask extends DefaultTask {
	def URL assetCfg

	def String inRPath

	def assetClassLoader

	@TaskAction
	def action() {
		ModelViewer viewer = new ModelViewer(assetCfg)
		if (assetClassLoader != null) viewer.addClassLoader(assetClassLoader);
		viewer.showModel("fake", inRPath)
		viewer.running.await()
	}
}
