package jme3_ext_assettools

import java.nio.file.Files;

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*

class ExtractModelTask extends DefaultTask {
	def URL assetCfg
	def assetClassLoader

	def file
	File getFile() {
		return project.file(file)
	}

	def rpath
	String getRpath() {
		(rpath == null) ? null : rpath.toString()
	}

	def boolean prefixTexture = true

	//@OutputDirectory
	def outDir
	File getOutDir(){
		if (outDir == null) {
			outDir = "${project.buildDir}/assets"
		}
		project.file(outDir)
	}

	def outBaseName
	@OutputFiles
	def outFiles
	FileCollection getOutFiles(){
		(outFiles == null)? project.files() : project.files(outFiles)
	}

	@TaskAction
	def action() {
		if (outBaseName == null) {
			String p = (getRpath() != null)? getRpath() : getFile().getName()
			p = p.replace('\\', '/')
			int i0 = Math.max(0, p.lastIndexOf('/'))
			outBaseName = p.subSequence(i0, p.indexOf('.', i0))
		}
		ModelExtractor extractor = new ModelExtractor(assetCfg)
		if (assetClassLoader) {
			extractor.assetManager.addClassLoader(assetClassLoader)
		}
		outFiles = project.files((getRpath() != null)
			? extractor.extract(outBaseName, getRpath(), prefixTexture, getOutDir())
			: extractor.extract(outBaseName, getFile(), prefixTexture, getOutDir())
		)
	}
}
