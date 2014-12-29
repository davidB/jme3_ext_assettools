package jme3_ext_remote_editor;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import pgex.Datas.Data;

import com.jme3.asset.AssetManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.scene.Geometry;
import com.jme3.scene.LightNode;
import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.shader.VarType;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

// TODO use a Validation object (like in scala/scalaz) with option to log/dump stacktrace
@RequiredArgsConstructor
public class Pgex {
	final AssetManager assetManager;
	final Material defaultMaterial;

	public Vector2f cnv(pgex.Datas.Vec2 src, Vector2f dst) {
		dst.set(src.getX(), src.getY());
		return dst;
	}

	public Vector3f cnv(pgex.Datas.Vec3 src, Vector3f dst) {
		dst.set(src.getX(), src.getY(), src.getZ());
		return dst;
	}

	public Vector4f cnv(pgex.Datas.Vec4 src, Vector4f dst) {
		dst.set(src.getX(), src.getY(), src.getZ(), src.getW());
		return dst;
	}

	public Quaternion cnv(pgex.Datas.Quaternion src, Quaternion dst) {
		dst.set(src.getX(), src.getY(), src.getZ(), src.getW());
		return dst;
	}

	public Vector4f cnv(pgex.Datas.Quaternion src, Vector4f dst) {
		dst.set(src.getX(), src.getY(), src.getZ(), src.getW());
		return dst;
	}

	public ColorRGBA cnv(pgex.Datas.Color src, ColorRGBA dst) {
		dst.set(src.getR(), src.getG(), src.getB(), src.getA());
		return dst;
	}

	public Matrix4f cnv(pgex.Datas.Mat4 src, Matrix4f dst) {
		dst.m00 = src.getC00();
		dst.m10 = src.getC10();
		dst.m20 = src.getC20();
		dst.m30 = src.getC30();
		dst.m01 = src.getC01();
		dst.m11 = src.getC11();
		dst.m21 = src.getC21();
		dst.m31 = src.getC31();
		dst.m02 = src.getC02();
		dst.m12 = src.getC12();
		dst.m22 = src.getC22();
		dst.m32 = src.getC32();
		dst.m03 = src.getC03();
		dst.m13 = src.getC13();
		dst.m23 = src.getC23();
		dst.m33 = src.getC33();
		return dst;
	}

	public Mesh.Mode cnv(pgex.Datas.Mesh.Primitive v) {
		switch(v) {
		case line_strip: return Mode.LineStrip;
		case lines: return Mode.Lines;
		case points: return Mode.Points;
		case triangle_strip: return Mode.TriangleStrip;
		case triangles: return Mode.Triangles;
		default: throw new IllegalArgumentException(String.format("doesn't support %s : %s", v.getClass(), v));
		}
	}

	public VertexBuffer.Type cnv(pgex.Datas.VertexArray.Attrib v) {
		switch(v) {
		case position: return Type.Position;
		case normal: return Type.Normal;
		case bitangent: return Type.Binormal;
		case tangent: return Type.Tangent;
		case color: return Type.Color;
		case texcoord: return Type.TexCoord;
		case texcoord2: return Type.TexCoord2;
		case texcoord3: return Type.TexCoord3;
		case texcoord4: return Type.TexCoord4;
		case texcoord5: return Type.TexCoord5;
		case texcoord6: return Type.TexCoord6;
		case texcoord7: return Type.TexCoord7;
		case texcoord8: return Type.TexCoord8;
		default: throw new IllegalArgumentException(String.format("doesn't support %s : %s", v.getClass(), v));
		}
	}

	//TODO use an optim version: including a patch for no autoboxing : https://code.google.com/p/protobuf/issues/detail?id=464
	public float[] hack_cnv(pgex.Datas.FloatBuffer src) {
		float[] b = new float[src.getValuesCount()];
		List<Float> l = src.getValuesList();
		for(int i = 0; i < b.length; i++) b[i] = l.get(i);
		return b;
	}

	//TODO use an optim version: including a patch for no autoboxing : https://code.google.com/p/protobuf/issues/detail?id=464
	public int[] hack_cnv(pgex.Datas.UintBuffer src) {
		int[] b = new int[src.getValuesCount()];
		List<Integer> l = src.getValuesList();
		for(int i = 0; i < b.length; i++) b[i] = l.get(i);
		return b;
	}

	public Mesh cnv(pgex.Datas.Mesh src, Mesh dst) {
		if (src.getIndexArraysCount() > 1) {
			throw new IllegalArgumentException("doesn't support more than 1 index array");
		}
		if (src.getLod() > 1) {
			throw new IllegalArgumentException("doesn't support lod > 1 : "+ src.getLod());
		}

		dst.setMode(cnv(src.getPrimitive()));
		for(pgex.Datas.VertexArray va : src.getVertexArraysList()) {
			VertexBuffer.Type type = cnv(va.getAttrib());
			dst.setBuffer(type, va.getFloats().getStep(), hack_cnv(va.getFloats()));
		}
		for(pgex.Datas.IndexArray va : src.getIndexArraysList()) {
			dst.setBuffer(VertexBuffer.Type.Index, va.getInts().getStep(), hack_cnv(va.getInts()));
		}
		dst.updateCounts();
		dst.updateBound();
		return dst;
	}

	public Geometry cnv(pgex.Datas.GeometryObject src, Geometry dst) {
		if (src.getMeshesCount() > 1) {
			throw new IllegalArgumentException("doesn't support more than 1 mesh");
		}
		dst.setName(src.getId());
		dst.setMesh(cnv(src.getMeshes(0), new Mesh()));
		return dst;
	}

	//TODO optimize to create less intermediate node
	public void merge(pgex.Datas.Data src, Node root, Map<String, Object> components) {
		mergeNodes(src, root, components);
		mergeGeometries(src, root, components);
		mergeMaterials(src, components);
		mergeLights(src, root, components);
		//mergeCustomParams(src, components);
		// relations should be the last because it reuse data provide by other (put in components)
		mergeRelations(src, root, components);
	}

	public void mergeLights(Data src, Node root, Map<String, Object> components) {
		for(pgex.Datas.Light srcl : src.getLightsList()) {
			//TODO manage parent hierarchy
			String id = srcl.getId();
			LightNode dst = (LightNode) components.get(id);
			if (dst == null) {
				Light l0 = null;
				switch(srcl.getKind()) {
				case ambient:
					l0 = new AmbientLight();
					break;
				case directional: {
					DirectionalLight l = new DirectionalLight();
					l.setDirection(Vector3f.UNIT_Z.clone());
					l0 = l;
					break;
				}
				case spot: {
					SpotLight l = new SpotLight();
					l.setDirection(Vector3f.UNIT_Z.clone());
					l0 = l;
					break;
				}
				case point:
					l0 = new PointLight();
					break;
				}
				dst = new LightNode(id, l0);
				dst.getLight().setName(id);
				root.addLight(dst.getLight());
				root.attachChild(dst);
				components.put(id, dst);
			}
			if (srcl.hasColor()) {
				dst.getLight().setColor(cnv(srcl.getColor(), new ColorRGBA()).mult(srcl.getIntensity()));
			}
			//TODO manage attenuation
			switch(srcl.getKind()) {
			case directional: {
				DirectionalLight l = (DirectionalLight)dst.getLight();
				if (srcl.hasDirection()) {
					l.setDirection(cnv(srcl.getDirection(), l.getDirection()));
				}
			}
			case spot: {
				SpotLight l = (SpotLight)dst.getLight();
				if (srcl.hasAttenuation() && srcl.getAttenuation().hasAngle()) {
					l.setSpotOuterAngle(srcl.getAttenuation().getAngle());
				}
				if (srcl.hasDirection()) {
					l.setDirection(cnv(srcl.getDirection(), l.getDirection()));
				}
				break;
			}
			case point: {
				PointLight l = (PointLight)dst.getLight();
				if (srcl.hasAttenuation() && srcl.getAttenuation().hasDistance() && srcl.getAttenuation().getDistance()) {
					if (srcl.getAttenuation().hasLinear()) {
						l.setRadius(srcl.getAttenuation().getLinear().getEnd());
					} else if (srcl.getAttenuation().hasSmooth()) {
						l.setRadius(srcl.getAttenuation().getSmooth().getEnd());
					}
				}
				break;
			}
			default:
				//nothing
				break;
			}
		}
	}

	public void mergeNodes(pgex.Datas.Data src, Node root, Map<String, Object> components) {
		for(pgex.Datas.Node n : src.getNodesList()) {
			//TODO manage parent hierarchy
			String id = n.getId();
			Node child = (Node) components.get(id);
			if (child == null) {
				child = new Node(id);
				root.attachChild(child);
				components.put(id, child);
			}
			if (n.getTransformsCount() > 0) {
				if (n.getTransformsCount() > 1) {
					throw new IllegalArgumentException("doesn't support more than 1 transform");
				}
				merge(n.getTransforms(0), child);
			}
		}
	}

	public void mergeGeometries(pgex.Datas.Data src, Node root, Map<String, Object> components) {
		for(pgex.Datas.GeometryObject g : src.getGeometriesList()) {
			//TODO manage parent hierarchy
			String id = g.getId();
			Geometry child = (Geometry)components.get(id);
			if (child == null) {
				child = new Geometry();
				child.setMaterial(defaultMaterial);
				root.attachChild(child);
				components.put(id, child);
			}
			child = cnv(g, child);
		}
	}

	public void mergeMaterials(pgex.Datas.Data src, Map<String, Object> components) {
		for(pgex.Datas.Material m : src.getMaterialsList()) {
			//TODO manage parent hierarchy
			String id = m.getId();
			Material mat = (Material)components.get(id);
			if (mat == null) {
				//TODO choose material via family or MatParam
				mat = newMaterial(m);
				components.put(id, mat);
			}
			for(pgex.Datas.MaterialParam p : m.getParamsList()) {
				mergeToMaterial(p, mat);
			}
		}
	}

	//	public void mergeCustomParams(pgex.Datas.Data src, Map<String, Object> components) {
	//		for(pgex.Datas.CustomParams g : src.getCustomParamsList()) {
	//			//TODO merge (instead of replace)
	//			components.put(g.getId(), g);
	//		}
	//	}

	public void mergeRelations(pgex.Datas.Data src, Node root, Map<String, Object> components) {
		for(pgex.Datas.Relation r : src.getRelationsList()) {
			Object op1 = components.get(r.getRef1());
			Object op2 = components.get(r.getRef2());
			if (op1 == null) {
				System.out.println("can't link op1 not found :" + r.getRef1());
			}
			if (op2 == null) {
				System.out.println("can't link op2 not found :" + r.getRef2());
			}
			if (op1 == null || op2 == null) return;
			boolean done = false;
			if (op1 instanceof Geometry) {
				Geometry g1 = (Geometry) op1;
				if (op2 instanceof LightNode) {
					((LightNode)op2).attachChild(g1);
					// TODO raise an alert, strange to link LightNode and Geometry
					done = true;
				} else if (op2 instanceof Material) {
					g1.setMaterial((Material)op2);
					done = true;
				} else if (op2 instanceof Node) {
					((Node) op2).attachChild(g1);
					done = true;
					//				}else if (op2 instanceof pgex.Datas.CustomParams) {
					//					for(pgex.Datas.CustomParam p : ((pgex.Datas.CustomParams)op2).getParamsList()) {
					//						g1.setUserData(p.getName(), getValue(p));
					//					}
				}
			} else if (op1 instanceof LightNode) {
				LightNode l1 = (LightNode)op1;
				if (op2 instanceof Node) {
					((Node) op2).attachChild(l1);
					done = true;
				}
			} else if (op1 instanceof Material) {
				Material m1 = (Material)op1;
				if (op2 instanceof Node) {
					((Node) op2).setMaterial(m1);
					done = true;
					//				}else if (op2 instanceof pgex.Datas.CustomParams) {
					//					for(pgex.Datas.CustomParam p : ((pgex.Datas.CustomParams)op2).getParamsList()) {
					//						mergeToMaterial(p, m1);
					//					}
				}
			} else if (op1 instanceof Node) {
				// Node n1 = (Node) op1;
				//				if (op2 instanceof pgex.Datas.CustomParams) {
				//					for(pgex.Datas.CustomParam p : ((pgex.Datas.CustomParams)op2).getParamsList()) {
				//						n1.setUserData(p.getName(), getValue(p));
				//					}
				//				}
			}
			if (!done) {
				System.out.printf("doesn't know how to make relation %s(%s) -- %s(%s)\n", r.getRef1(), op1.getClass(), r.getRef2(), op2.getClass());
			}
		}
	}

	//	Object getValue(pgex.Datas.CustomParam p) {
	//		if (p.hasColor()) return cnv(p.getColor(), new ColorRGBA());
	//		if (p.hasFloat()) return p.getFloat();
	//		if (p.hasInt()) return p.getInt();
	//		if (p.hasMat4()) return cnv(p.getMat4(), new Matrix4f());
	//		if (p.hasQuat()) return cnv(p.getQuat(), new Quaternion());
	//		if (p.hasString()) return p.getString();
	//		if (p.hasVec2()) return cnv(p.getVec2(), new Vector2f());
	//		if (p.hasVec3()) return cnv(p.getVec3(), new Vector3f());
	//		if (p.hasVec4()) return cnv(p.getVec4(), new Vector4f());
	//		return null;
	//	}

	public Image.Format getValue(pgex.Datas.Texture2DInline.Format f) {
		switch(f){
		//case bgra8: return Image.Format.BGR8;
		case rgb8: return Image.Format.RGB8;
		case rgba8: return Image.Format.RGBA8;
		default: throw new UnsupportedOperationException("image format :" + f);
		}
	}

	public Texture getValue(pgex.Datas.Texture t) {
		switch(t.getDataCase()){
		case DATA_NOT_SET: return null;
		case RPATH: return assetManager.loadTexture(t.getRpath());
		case TEX2D: {
			pgex.Datas.Texture2DInline t2di = t.getTex2D();
			Image img = new Image(getValue(t2di.getFormat()), t2di.getWidth(), t2di.getHeight(), t2di.getData().asReadOnlyByteBuffer());
			return new Texture2D(img);
		}
		default:
			throw new IllegalArgumentException("doesn't support more than texture format:" + t.getDataCase());
		}
	}

	public Material newMaterial(pgex.Datas.Material m) {
		boolean lightFamily = false;
		for (pgex.Datas.MaterialParam p : m.getParamsList()) {
			lightFamily = lightFamily || (p.getAttrib() == pgex.Datas.MaterialParam.Attrib.specular);
		}
		String def = lightFamily ? "Common/MatDefs/Light/Lighting.j3md" : "Common/MatDefs/Misc/Unshaded.j3md";
		Material mat = new Material(assetManager, def);
		if (lightFamily) {
			mat.setBoolean("UseMaterialColors", true);
			mat.setBoolean("UseVertexColor", true);
		}
		return mat;
	}

	//	Material mergeToMaterial(pgex.Datas.CustomParam p, Material dst) {
	//		String name = p.getName();
	//		switch(p.) {
	//		if (p.hasColor()){
	//			dst.setColor(name, cnv(p.getColor(), new ColorRGBA()));
	//		} else if (p.hasFloat()) {
	//			dst.setFloat(name, p.getFloat());
	//		} else if (p.hasInt()) {
	//			dst.setInt(name, p.getInt());
	//		} else if (p.hasMat4()) {
	//			dst.setMatrix4(name, cnv(p.getMat4(), new Matrix4f()));
	//		} else if (p.hasQuat()) {
	//			dst.setVector4(name, cnv(p.getQuat(), new Vector4f()));
	//		} else if (p.hasString()){
	//			System.out.println("Material doesn't support string parameter :" + name + " --> " + p.getString());
	//		} else if (p.hasVec2()) {
	//			dst.setVector2(name, cnv(p.getVec2(), new Vector2f()));
	//		} else if (p.hasVec3()) {
	//			dst.setVector3(name, cnv(p.getVec3(), new Vector3f()));
	//		} else if (p.hasVec4()) {
	//			dst.setVector4(name, cnv(p.getVec4(), new Vector4f()));
	//		} else if (p.hasTexture()) {
	//			dst.setTexture(name, getValue(p.getTexture()));
	//		} else {
	//			dst.clearParam(name);
	//		}
	//		return dst;
	//	}

	public Material mergeToMaterial(pgex.Datas.MaterialParam p, Material dst) {
		String name = findMaterialParamName(p.getAttrib().name(), toVarType(p.getValueCase()), dst);
		if (name == null){
			System.out.println("can't find a matching name for :" + p.getAttrib().name() + " (" + p.getValueCase().name() + ")");
			return dst;
		}
		switch(p.getValueCase()) {
		case VALUE_NOT_SET:
			dst.clearParam(name);
			break;
		case VBOOL:
			dst.setBoolean(name, p.getVbool());
			break;
		case VCOLOR:
			dst.setColor(name, cnv(p.getVcolor(), new ColorRGBA()));
			break;
		case VFLOAT:
			dst.setFloat(name, p.getVfloat());
			break;
		case VINT:
			dst.setInt(name, p.getVint());
			break;
		case VMAT4:
			dst.setMatrix4(name, cnv(p.getVmat4(), new Matrix4f()));
			break;
		case VQUAT:
			dst.setVector4(name, cnv(p.getVquat(), new Vector4f()));
			break;
		case VSTRING:
			System.out.println("Material doesn't support string parameter :" + name + " --> " + p.getVstring());
			break;
		case VTEXTURE:
			dst.setTexture(name, getValue(p.getVtexture()));
			break;
		case VVEC2:
			dst.setVector2(name, cnv(p.getVvec2(), new Vector2f()));
			break;
		case VVEC3:
			dst.setVector3(name, cnv(p.getVvec3(), new Vector3f()));
			break;
		case VVEC4:
			dst.setVector4(name, cnv(p.getVvec4(), new Vector4f()));
			break;
		default:
			System.out.println("Material doesn't support parameter :" + name + " of type " + p.getValueCase().name());
			break;
		}
		return dst;
	}

	public String findMaterialParamName(String name, VarType type, Material dst) {
		if (type == null) return null;
		MaterialDef md = dst.getMaterialDef();
		String name2 = findMaterialParamName(new String[]{name, name + "Map"}, type, md);
		if (name2 == null) {
			if (pgex.Datas.MaterialParam.Attrib.color.name().equals(name)) {
				name2 = findMaterialParamName(new String[]{"Color", "Diffuse", "ColorMap", "DiffuseMap"}, type, md);
			} else if (pgex.Datas.MaterialParam.Attrib.specular.name().equals(name)) {
				name2 = findMaterialParamName(new String[]{"Specular", "SpecularMap"}, type, md);
			} else if (pgex.Datas.MaterialParam.Attrib.specular_power.name().equals(name)) {
				name2 = findMaterialParamName(new String[]{"Shininess", "ShininessMap"}, type, md);
			} else if (pgex.Datas.MaterialParam.Attrib.emission.name().equals(name)) {
				name2 = findMaterialParamName(new String[]{"GlowColor", "GlowMap"}, type, md);
			} else if (pgex.Datas.MaterialParam.Attrib.normal.name().equals(name)) {
				name2 = findMaterialParamName(new String[]{"NormalMap"}, type, md);
			} else if (pgex.Datas.MaterialParam.Attrib.opacity.name().equals(name)) {
				name2 = findMaterialParamName(new String[]{"AlphaMap"}, type, md);
			}
		}
		return name2;
	}

	public String findMaterialParamName(String[] names, VarType type, MaterialDef scope) {
		for(String name2 : names){
			for(MatParam mp : scope.getMaterialParams()) {
				if (mp.getName().equalsIgnoreCase(name2) && mp.getVarType() == type) {
					return mp.getName();
				}
			}
		}
		return null;
	}

	public VarType toVarType(pgex.Datas.MaterialParam.ValueCase src) {
		switch(src) {
		case VBOOL : return VarType.Boolean;
		case VCOLOR : return VarType.Vector4;
		case VFLOAT : return VarType.Float;
		case VINT : return VarType.Int;
		case VMAT4 : return VarType.Matrix4;
		case VQUAT : return VarType.Vector4;
		case VTEXTURE : return VarType.Texture2D;
		case VVEC2 : return VarType.Vector2;
		case VVEC3 : return VarType.Vector3;
		case VVEC4 : return VarType.Vector4;
		default: return null;
		}
	}

	public void merge(pgex.Datas.Transform src, Spatial dst) {
		dst.setLocalRotation(cnv(src.getRotation(), dst.getLocalRotation()));
		dst.setLocalTranslation(cnv(src.getTranslation(), dst.getLocalTranslation()));
		dst.setLocalScale(cnv(src.getScale(), dst.getLocalScale()));
	}
}

