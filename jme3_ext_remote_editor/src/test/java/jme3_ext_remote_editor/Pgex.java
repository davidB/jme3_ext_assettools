package jme3_ext_remote_editor;

import java.util.List;

import lombok.RequiredArgsConstructor;

import com.jme3.material.Material;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;

// TODO use a Validation object (like in scala/scalaz) with option to log/dump stacktrace
@RequiredArgsConstructor
public class Pgex {

	final Material defaultMaterial;

	public Vector3f cnv(pgex.Datas.Vec3 src, Vector3f dst) {
		dst.set(src.getX(), src.getY(), src.getZ());
		return dst;
	}

	public Quaternion cnv(pgex.Datas.Quaternion src, Quaternion dst) {
		dst.set(src.getX(), src.getY(), src.getZ(), src.getW());
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
	public void merge(pgex.Datas.Data src, Node root) {
		for(pgex.Datas.Node n : src.getNodesList()) {
			//TODO manage parent hierarchy
			String id = n.getId();
			Spatial child = root.getChild(id);
			if (child == null) {
				child = new Node(id);
				root.attachChild(child);
			}
			if (n.getTransformsCount() > 0) {
				if (n.getTransformsCount() > 1) {
					throw new IllegalArgumentException("doesn't support more than 1 transform");
				}
				merge(n.getTransforms(0), child);
			}
		}
		for(pgex.Datas.GeometryObject g : src.getGeometriesList()) {
			//TODO manage parent hierarchy
			String id = g.getId();
			Geometry child = (Geometry)root.getChild(id);
			if (child == null) {
				child = new Geometry();
				child.setMaterial(defaultMaterial);
				root.attachChild(child);
			}
			child = cnv(g, child);
		}
		for(pgex.Datas.Relation r : src.getRelationsList()) {
			Spatial op1 = root.getChild(r.getSrc());
			Spatial op2 = root.getChild(r.getDest());
			if (op1 == null) {
				System.out.println("can't link op1 not found :" + r.getSrc());
			}
			if (op2 == null) {
				System.out.println("can't link op2 not found :" + r.getDest());
			}
			if (op1 != null && op2 != null) {
				 if (op1 instanceof Node) {
					((Node) op1).attachChild(op2);
				}
			}
		}
	}

	public void merge(pgex.Datas.Transform src, Spatial dst) {
		dst.setLocalRotation(cnv(src.getRotation(), dst.getLocalRotation()));
		dst.setLocalTranslation(cnv(src.getTranslation(), dst.getLocalTranslation()));
		dst.setLocalScale(cnv(src.getScale(), dst.getLocalScale()));
	}
}
