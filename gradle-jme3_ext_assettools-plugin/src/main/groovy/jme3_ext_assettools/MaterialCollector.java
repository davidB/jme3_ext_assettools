package jme3_ext_assettools;

import java.util.HashSet;

import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;

public class MaterialCollector implements SceneGraphVisitor {
	final HashSet<Material> materials = new HashSet<Material>();
	final HashSet<Texture> textures = new HashSet<Texture>();

	public void collect(Spatial v, boolean reset) {
		if (reset) {
			materials.clear();
			textures.clear();
		}
		v.breadthFirstTraversal(this);
	}

	@Override
	public void visit(Spatial v) {
		if (v instanceof Geometry) {
			Geometry g = (Geometry)v;
			Material m = g.getMaterial();
			for (MatParam mp : m.getParams()) {
				if (mp.getVarType() == VarType.Texture2D || mp.getVarType() == VarType.Texture3D || mp.getVarType() == VarType.TextureCubeMap) {
					textures.add(((Texture)mp.getValue()));
				}
			}
			materials.add(m);
		}
	}
}