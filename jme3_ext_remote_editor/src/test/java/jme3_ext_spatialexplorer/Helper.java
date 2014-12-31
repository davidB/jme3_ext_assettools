package jme3_ext_spatialexplorer;

import java.util.List;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class Helper {

	public static void dump(Node node, String prefix) {
		List<Spatial> children = node.getChildren();
		System.out.printf("%s %s (%d)\n", prefix, node.getName(), children.size());
		prefix = (prefix.length() == 0)? " +--":  ("\t"+ prefix);
		for (Spatial sp : children) {
			if (sp instanceof Node) dump((Node) sp, prefix);
			else System.out.printf("%s %s [%s]\n", prefix, sp.getName(), sp.getClass().getName());
		}
	}
}
