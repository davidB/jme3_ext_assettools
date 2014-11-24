package jme3_ext_assettools;

import groovy.lang.Closure;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import lombok.Setter;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.file.RelativePath;
import org.gradle.api.internal.file.AbstractFileTree;
import org.gradle.api.internal.file.DefaultFileVisitDetails;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.nativeintegration.filesystem.services.FileSystemServices;
import org.gradle.internal.nativeplatform.filesystem.FileSystem;
import org.gradle.internal.os.OperatingSystem;

import com.jme3.asset.plugins.FileLocator;
import com.jme3.asset.plugins.UrlLocator;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.math.Transform;

public class ModelExtractorTask extends DefaultTask {
	public URL assetCfg;
	public ClassLoader assetClassLoader;
	@Setter private Object assetClassPath;
	public FileCollection getAssetClassPath() {
		if (assetClassPath == null) return null;
		Object v = (assetClassPath instanceof Closure) ? ((Closure<?>)assetClassPath).call() : assetClassPath;
		return getProject().files(v);
	}

	@Setter private Object assetDirs;
	public FileCollection getAssetDirs() {
		return (assetDirs == null) ? null : getProject().files(assetDirs);
	}

	@Setter private Object file;
	public File getFile() {
		return getProject().file(file);
	}

	@Setter private Object rpath;
	public String getRpath() {
		if (rpath == null) return null;
		Object v = (rpath instanceof Closure) ? ((Closure<?>)rpath).call() : rpath;
		return v.toString();
	}

	@Setter Object prefixTexture = true;
	public boolean getPrefixTexture() {
		if (prefixTexture == null) return true;
		Object v = (prefixTexture instanceof Closure) ? ((Closure<?>)prefixTexture).call() : prefixTexture;
		return Boolean.valueOf(v.toString());
	}

	@Setter Object scale = 1.0f;
	public float getScale() {
		if (scale == null) return 1.0f;
		Object v = (scale instanceof Closure) ? ((Closure<?>)scale).call() : scale;
		return Float.valueOf(v.toString());
	}

	//@OutputDirectory
	@Setter private Object outDir;
	public File getOutDir(){
		if (outDir == null) {
			outDir = getProject().getBuildDir() + "/assets";
		}
		return getProject().file(outDir);
	}

	@Setter private Object outBaseName;
	public String getOutBaseName() {
		if (outBaseName == null) return null;
		Object v = (outBaseName instanceof Closure) ? ((Closure<?>)outBaseName).call() : outBaseName;
		return v.toString();
	}

	@OutputFiles
	@Setter private Object outFiles;
	public FileCollection getOutFiles(){
		return (outFiles == null)
			? getProject().files()
			: (outFiles instanceof FileCollection)
			? (FileCollection) outFiles
			:getProject().files(outFiles)
			;
	}

	@TaskAction
	public void action() throws Exception{
		Logger.getLogger("com.jme3").setLevel(Level.INFO);
		if (getOutBaseName() == null) {
			String p = (getRpath() != null)? getRpath() : getFile().getName();
			p = p.replace('\\', '/');
			int begin = Math.max(0, p.lastIndexOf('/'));
			int end = p.indexOf('.', begin);
			end = (end<0) ? p.length() : end;
			outBaseName = p.subSequence(begin, end);
		}
		ModelExtractor extractor = new ModelExtractor(assetCfg);
		if (assetClassLoader != null) {
			extractor.assetManager.addClassLoader(assetClassLoader);
		}
		FileCollection acp = getAssetClassPath();
		if (acp != null && !acp.isEmpty()) {

			Set<File> files = acp.getFiles();
			for(File f: files) {
				System.out.println("add file :" + f);
				if (f.isDirectory()) {
					extractor.assetManager.registerLocator(f.getAbsolutePath(), FileLocator.class);
				} else if (f.getName().endsWith(".jar") || f.getName().endsWith(".zip")) {
					extractor.assetManager.registerLocator(f.getAbsolutePath(), ZipLocator.class);
				} else {
					extractor.assetManager.registerLocator(f.toURI().toURL().toExternalForm(), UrlLocator.class);
				}
			}

//			URL[] urls = new URL[files.size()];
//			int i = 0;
//			for(File f: files) {
//				urls[i] = f.toURI().toURL();
//				System.out.println("add url :" + i + " .. " + urls[i]);
//				i++;
//			}
//			extractor.assetManager.addClassLoader(new URLClassLoader(urls));
		}
		extractor.addAssetDirs(getAssetDirs());
		Transform t = new Transform();
		t.setScale(getScale());
		final Collection<File> files = (getRpath() != null)
			? extractor.extract(getOutBaseName(), getRpath(), getPrefixTexture(), getOutDir(), t)
			: extractor.extract(getOutBaseName(), getFile(), getPrefixTexture(), getOutDir(), t)
		;
//		outFiles = getProject().files(files);

		//TODO find a way to return FileTree with correct path
//		PatternSet p = new PatternSet();
//		p.include(new Spec<FileTreeElement>(){
//			//HashSet<String> check = HashSet<String>();
//
//			@Override
//			public boolean isSatisfiedBy(FileTreeElement arg0) {
//				return files.contains(arg0.getFile());
//			}
//		});
//		outFiles = new DirectoryFileTree(getOutDir(), p);

		final FileSystem fileSystem = new FileSystemServices().createFileSystem(OperatingSystem.current());
		outFiles = new AbstractFileTree() {
			@Override
			public FileTree visit(FileVisitor visitor) {

				AtomicBoolean stopFlag = new AtomicBoolean();
				int parentPathLg = getOutDir().getAbsolutePath().length() + 1;
				for(File file : files) {
					RelativePath path = new RelativePath(true, file.getAbsolutePath().substring(parentPathLg).split(File.separator));
					FileVisitDetails details = new DefaultFileVisitDetails(file, path, stopFlag, fileSystem, fileSystem);
					visitor.visitFile(details);
					if (stopFlag.get()) break;
				}
				return this;
			}

			@Override
			public String getDisplayName() {
				return "ModelExtractor";
			}
		};
	}

}
