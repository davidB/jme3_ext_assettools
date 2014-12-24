package jme3_ext_remote_editor;

import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public class Pgex {

	static Vector3f cnv(pgex.Math.Vec3 src, Vector3f dst) {
		dst.set(src.getX(), src.getY(), src.getZ());
		return dst;
	}

	static Quaternion cnv(pgex.Math.Quaternion src, Quaternion dst) {
		System.out.printf("quat before: %s : w:%s x:%s y:%s z:%s\n", src.toString(), src.getW(),  src.getX(),  src.getY(),  src.getZ());
		dst.set(src.getX(), src.getY(), src.getZ(), src.getW());
		System.out.printf("quat after: %s : w:%s x:%s y:%s z:%s\n", dst.toString(), dst.getW(),  src.getX(),  src.getY(),  src.getZ());
		return dst;
	}

	static Matrix4f cnv(pgex.Math.Mat4 src, Matrix4f dst) {
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
}
