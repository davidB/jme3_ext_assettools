package jme3_ext_assettools;

import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.tools.ant.util.ReaderInputStream;
import org.gradle.api.file.FileCollection;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetManager;
import com.jme3.asset.MaterialKey;
import com.jme3.asset.TextureKey;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
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
		try {
			return extract(name, assetManager.loadModel(rpath), prefixTexture, froot, transform);
		} catch(Exception exc) {
			String msg = String.format("failed to extract '%s' from '%s' into '%s'", name, rpath, froot);
			throw new IllegalStateException(msg , exc);
		}
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
		String tmpAssetDir = null;
		if (rpath == null) {
			tmpAssetDir = f.getParent();
			rpath = f.getName();
		}
		try {
			if (tmpAssetDir != null) assetManager.registerLocator(tmpAssetDir, FileLocator.class);
			b = extract(name, assetManager.loadModel(f.getName()), prefixTexture, froot, transform);
			if (tmpAssetDir != null) assetManager.unregisterLocator(tmpAssetDir, FileLocator.class);
			return b;
		} catch(Exception exc) {
			String msg = String.format("failed to extract '%s' from '%s' into '%s': file(%s), tmpAssetDir(%s)", name, rpath, froot, f, tmpAssetDir);
			throw new IllegalStateException(msg , exc);
		}
	}

	public Collection<File> extract(String name, Spatial root, boolean prefixTexture, File froot, Transform transform) throws Exception {
		if (transform != null) {
			root.setLocalTransform(transform.combineWithParent(root.getLocalTransform()));
		}
		HashSet<File> b = new HashSet<File>();
		froot = froot.getAbsoluteFile();
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
						Files.copy(in, f.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
					//Files.copy(assetManager.locateAsset(k).openStream(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
					// rewrite the j3m, because path of texture is different
					try (InputStream in = new ReaderInputStream(new StringReader(materialToJ3M(t)), "UTF-8")) {
						Files.copy(in, f.toPath(), StandardCopyOption.REPLACE_EXISTING);
					}
				}
				b.add(f);
			}
		}
		File f0 = new File(froot, "Models/" + name +".j3o");
		exporter.save(root, f0);
		b.add(f0);
		return b;
	}

	static String materialToJ3M(Material m) {
		String out = String.format("Material %s : %s {\n", m.getName(), m.getMaterialDef().getAssetName());
		out += "\tMaterialParameters {\n";
		for(MatParam p : m.getParams()) {
			out += String.format("\t\t%s : %s\n", p.getName(), p.getValueAsString());
		}
		out += "\t}\n";
		RenderState s = m.getAdditionalRenderState();
		if (s != null) {
			out += "\tAdditionalRenderState {\n";
			if (s.isApplyAlphaFallOff()) out += String.format("\t\tAlphaTestFalloff %.3d\n", s.getAlphaFallOff());
			if (s.isApplyBlendMode()) out += String.format("\t\tBlend %s\n", s.getBlendMode().name());
			if (s.isApplyColorWrite()) out += String.format("\t\tColorWrite %s\n", s.isColorWrite());
			if (s.isApplyCullMode()) out += String.format("\t\tFaceCull %s\n", s.getFaceCullMode().name());
			if (s.isApplyDepthTest()) out += String.format("\t\tDepthTest %s\n", s.isDepthTest());
			if (s.isApplyDepthWrite()) out += String.format("\t\tDepthWrite %s\n", s.isDepthWrite());
			if (s.isApplyPointSprite()) out += String.format("\t\tPointSprite %s\n", s.isPointSprite());
			if (s.isApplyPolyOffset()) out += String.format("\t\tPolyOffset %.3d %.3d\n", s.getPolyOffsetFactor(), s.getPolyOffsetUnits());
			if (s.isApplyWireFrame()) out += String.format("\t\tWireframe %s\n", s.isWireframe());
			out += "\t}\n";
		}
		out += "}\n";
		return out;
	}
}

