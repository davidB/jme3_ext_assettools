package samples;

import java.util.logging.Level;
import java.util.logging.Logger;

import jme3_ext_remote_editor.AppState4RemoteCommand;

import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.debug.Grid;
import com.jme3.system.AppSettings;

public class DemoRendererForBlender {
	public static void main(String[] args) {
		Logger.getLogger("").setLevel(Level.WARNING);

		AppSettings settings = new AppSettings(true);
		settings.setResolution(1280, 720);
		settings.setVSync(true);
		settings.setFullscreen(false);

		SimpleApplication app = new SimpleApplication(){
			@Override
			public void simpleInitApp() {
			}
		};

		app.setSettings(settings);
		app.setShowSettings(false);
		app.setDisplayStatView(true);
		app.setDisplayFps(true);
		// !!!! without .setPauseOnLostFocus(false) server will only send screenshot to blender,... when jme main screen have focus
		app.setPauseOnLostFocus(false);
		app.start();

		//Setup Camera
		app.enqueue(() -> {
			app.getFlyByCamera().setEnabled(false);
			app.getStateManager().detach(app.getStateManager().getState(FlyCamAppState.class));
			app.getInputManager().setCursorVisible(true);
			return null;
		});
		app.enqueue(() -> {
			app.getRootNode().attachChild(makeScene(app));
			return null;
		});
		app.enqueue(() -> {
			app.getStateManager().attach(new AppState4RemoteCommand());
			return null;
		});
	}

	static Node makeScene(SimpleApplication app) {
		Node scene = new Node("demo");
		AssetManager assetManager = app.getAssetManager();
		scene.attachChild(makeGrid(Vector3f.ZERO, 10, ColorRGBA.Blue, assetManager));
		scene.attachChild(makeCoordinateAxes(Vector3f.ZERO, assetManager));
		return scene;
	}
	static Spatial makeGrid(Vector3f pos, int size, ColorRGBA color, AssetManager assetManager){
		Geometry g = putShape("wireframe grid", new Grid(size, size, 1.0f), color, assetManager);
		g.center().move(pos);
		return g;
	}

	static Spatial makeCoordinateAxes(Vector3f pos, AssetManager assetManager){
		Node b = new Node("axis");
		b.setLocalTranslation(pos);

		Arrow arrow = new Arrow(Vector3f.UNIT_X);
		arrow.setLineWidth(4); // make arrow thicker
		b.attachChild(putShape("x", arrow, ColorRGBA.Red, assetManager));

		arrow = new Arrow(Vector3f.UNIT_Y);
		arrow.setLineWidth(4); // make arrow thicker
		b.attachChild(putShape("y", arrow, ColorRGBA.Green, assetManager));

		arrow = new Arrow(Vector3f.UNIT_Z);
		arrow.setLineWidth(4); // make arrow thicker
		b.attachChild(putShape("z", arrow, ColorRGBA.Blue, assetManager));
		return b;
	}

	static Geometry putShape(String name, Mesh shape, ColorRGBA color, AssetManager assetManager){
		Geometry g = new Geometry(name, shape);
		Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		mat.getAdditionalRenderState().setWireframe(true);
		mat.setColor("Color", color);
		g.setMaterial(mat);
		return g;
	}
}
