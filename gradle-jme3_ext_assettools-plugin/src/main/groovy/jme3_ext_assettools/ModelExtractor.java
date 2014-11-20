package jme3_ext_assettools;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.gradle.api.file.FileCollection;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetManager;
import com.jme3.asset.MaterialKey;
import com.jme3.asset.TextureKey;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.material.Material;
import com.jme3.math.Transform;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.OBJLoader;
import com.jme3.scene.plugins.blender.BlenderLoader;
import com.jme3.system.JmeSystem;
import com.jme3.texture.Texture;

//TODO generate filename with checksum (md5)
public class ModelExtractor {
	public final AssetManager assetManager;
	public final List<File> assetDirs = new LinkedList<>();

	public ModelExtractor(URL assetCfg) throws Exception {
		if (assetCfg == null){
			assetCfg = Thread.currentThread().getContextClassLoader().getResource("com/jme3/asset/Desktop.cfg");
		}
		assetManager = JmeSystem.newAssetManager(assetCfg);
		assetManager.registerLoader(OBJLoader.class, "obj");
		assetManager.registerLoader(MTLoaderExt.class, "mtl");
		assetManager.registerLoader(BlenderLoader.class, "blend");
	}

	public void addAssetDirs(final FileCollection dirs) {
		if (dirs == null || dirs.isEmpty()) return;
		for( File f : dirs.getFiles()) {
			final File f0 = f;
			if (f.isDirectory()) {
				this.assetDirs.add(f);
				assetManager.registerLocator(f0.getAbsolutePath(), FileLocator.class);
			}
		}
	}

	public Collection<File> extract(String name, String rpath, boolean prefixTexture, File froot, Transform transform) {
		return extract(name, assetManager.loadModel(rpath), prefixTexture, froot, transform);
	}

	public Collection<File> extract(String name, File f, boolean prefixTexture, File froot, Transform transform) {
		String apath = f.getAbsolutePath();
		String rpath = null;
		for(File d : assetDirs) {
			if (apath.startsWith(d.getAbsolutePath())) {
				rpath = apath.substring(d.getAbsolutePath().length()+1);
			}
		}
		Collection<File> b;
		if (rpath == null) {
			assetManager.registerLocator(f.getParent(), FileLocator.class);
			b = extract(name, assetManager.loadModel(f.getName()), prefixTexture, froot, transform);
			assetManager.unregisterLocator(f.getParent(), FileLocator.class);
		} else {
			b = extract(name, assetManager.loadModel(rpath), prefixTexture, froot, transform);
		}
		return b;
	}

	public Collection<File> extract(String name, Spatial root, boolean prefixTexture, File froot, Transform transform) {
		if (transform != null) {
			root.setLocalTransform(transform.combineWithParent(root.getLocalTransform()));
		}
		HashSet<File> b = new HashSet<File>();
		froot = froot.getAbsoluteFile();
		try {
			BinaryExporter exporter = new BinaryExporter();
			MaterialCollector mc = new MaterialCollector();
			mc.collect(root, true);
			for(Texture t : mc.textures){
				TextureKey ksrc = (TextureKey)t.getKey();
				File f = new File(froot, ksrc.getName());
				if (!f.exists()) {
					String folder = "Textures/" + (prefixTexture?name + "/" : "");
					String kdestName = (ksrc.getFolder() == null || ksrc.getFolder().length() == 0)?  (folder + ksrc.getName()) : ksrc.getName().replace(ksrc.getFolder(), folder);
					TextureKey kdest = new TextureKey(kdestName);
					kdest.setAnisotropy(ksrc.getAnisotropy());
					kdest.setAsCube(ksrc.isAsCube());
					kdest.setAsTexture3D(ksrc.isAsTexture3D());
					kdest.setFlipY(ksrc.isFlipY());
					kdest.setGenerateMips(ksrc.isGenerateMips());
					kdest.setTextureTypeHint(ksrc.getTextureTypeHint());
					t.setKey(kdest);
					f = new File(froot, kdest.getName());
					f.getParentFile().mkdirs();
					AssetInfo ai = assetManager.locateAsset(ksrc);
					if (ai == null) {
						//System.err.println("not found : " + ksrc);
					} else {
						try (InputStream in = ai.openStream()) {
							Files.copy(ai.openStream(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
						}
					}
				}
				b.add(f);
			}
			//			int count = 0;
			for(Material t : mc.materials) {
				MaterialKey k = (MaterialKey)t.getKey();
				if (k == null) {
					//					//TODO create name from checksum
					//					count++;
					//					k = new AssetKey<Material>("Materials/" + count + ".j3m");
					//					t.setKey(k);
					//					File f = new File(froot, k.getName());
					//					if (!f.exists()) {
					//						f.getParentFile().mkdirs();
					//						exporter.save(t, f);
					//					}
					//					b.add(f);
				} else {
					File f = new File(froot, k.getName());
					if (!f.exists()) {
						f.getParentFile().mkdirs();
						Files.copy(assetManager.locateAsset(k).openStream(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
					}
					b.add(f);
				}
			}
			File f0 = new File(froot, "Models/" + name +".j3o");
			exporter.save(root, f0);
			b.add(f0);
			return b;
		} catch(RuntimeException exc) {
			throw exc;
		} catch(Exception exc) {
			throw new RuntimeException("wrap:" + exc, exc);
		}
	}
}

