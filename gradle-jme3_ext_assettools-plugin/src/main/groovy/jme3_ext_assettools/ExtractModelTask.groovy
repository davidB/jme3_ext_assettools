package jme3_ext_assettools

import java.nio.file.Files;

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.*

class ExtractModelTask extends DefaultTask {
	def URL assetCfg

	def File inFile

	def assetClassLoader
	def String inRPath

	def boolean prefixTexture = true

	@OutputDirectory
	def File outDir = project.file("${project.buildDir}/assets")
	def outBaseName
	def Collection<File> files

	@TaskAction
	def action() {
		if (outBaseName == null) {
			String p = (inRPath!=null)? inRPath : inFile.getName()
			p = p.replace('\\', '/')
			int i0 = Math.max(0, p.lastIndexOf('/'))
			outBaseName = p.subSequence(i0, p.indexOf('.', i0))
		}
		ModelExtractor extractor = new ModelExtractor(assetCfg)
		if (assetClassLoader) {
			extractor.assetManager.addClassLoader(assetClassLoader)
		}
		files = (inRPath != null) ? extractor.extract(outBaseName, inRPath, prefixTexture, outDir) : extractor.extract(outBaseName, inFile, prefixTexture, outDir)
	}
}
