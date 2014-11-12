package jme3_ext_assettools

import java.net.URL;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*

class ExtractModelTask extends DefaultTask {
	def URL assetCfg
	def assetClassLoader
	def assetDirs
	FileCollection getAssetDirs() {
		(assetDirs == null) ? null : project.files(assetDirs)
	}

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
	String getOutBaseName() {
		if (outBaseName == null) return null
		def v = (outBaseName instanceof Closure) ? outBaseName() : outBaseName
		v.toString()
	}

	@OutputFiles
	def outFiles
	FileCollection getOutFiles(){
		(outFiles == null)? project.files() : project.files(outFiles)
	}

	@TaskAction
	def action() {
		Logger.getLogger("com.jme3").setLevel(Level.INFO)
		if (getOutBaseName() == null) {
			String p = (getRpath() != null)? getRpath() : getFile().getName()
			p = p.replace('\\', '/')
			int begin = Math.max(0, p.lastIndexOf('/'))
			int end = p.indexOf('.', begin)
			end = (end<0) ? p.length() : end
			outBaseName = p.subSequence(begin, end)
		}
		ModelExtractor extractor = new ModelExtractor(getAssetCfg())
		if (getAssetClassLoader() != null) {
			extractor.assetManager.addClassLoader(getAssetClassLoader())
		}
		extractor.addAssetDirs(getAssetDirs())
		outFiles = project.files((getRpath() != null)
			? extractor.extract(getOutBaseName(), getRpath(), getPrefixTexture(), getOutDir())
			: extractor.extract(getOutBaseName(), getFile(), getPrefixTexture(), getOutDir())
		)
	}
}
