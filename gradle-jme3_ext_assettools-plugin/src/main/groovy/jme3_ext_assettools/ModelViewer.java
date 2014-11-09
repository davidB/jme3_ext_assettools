package jme3_ext_assettools;

import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;


public class ModelViewer extends SimpleApplication{

	public static void main(String[] args){
		ModelViewer app = new ModelViewer(null);
		if (args.length > 0) {
			String[] model = args[0].split("@");
			app.showModel(model[0], model[1]);
		}
	}

	public final CountDownLatch running = new CountDownLatch(1);

	public ModelViewer(URL assetCfg) {
		AppSettings settings = new AppSettings(true);
		if (assetCfg != null) {
			settings.putString("AssetConfigURL", assetCfg.toExternalForm());
		}
		this.setSettings(settings);
		this.start();
	}

	@Override
	public void simpleInitApp() {
		makeLigths(rootNode);
		setupCamera();
	}

	@Override
	public void destroy() {
		super.destroy();
		running.countDown();
	}

	public void addClassLoader(final ClassLoader cl) {
		final SimpleApplication app = this;
		this.enqueue(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				app.getAssetManager().addClassLoader(cl);
				return null;
			}
		});
	}

	public void showModel(final String name, final String path) {
		final SimpleApplication app = this;
		this.enqueue(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				Spatial v = app.getAssetManager().loadModel(path);
				v.setName(name);
				app.getRootNode().detachChildNamed(name);
				app.getRootNode().attachChild(v);
				return null;
			}
		});
	}

	void makeLigths(Node anchor) {
		DirectionalLight dl = new DirectionalLight();
		dl.setColor(ColorRGBA.White);
		dl.setDirection(Vector3f.UNIT_XYZ.negate());
		anchor.addLight(dl);
	}

	void setupCamera() {
		flyCam.setEnabled(true);
//		inputManager.setCursorVisible(true);
//		ChaseCamera chaseCam = new ChaseCamera(cam, target, inputManager);
//		chaseCam.setDefaultDistance(6.0f);
//		chaseCam.setMaxDistance(1000f);
//		//chaseCam.setDragToRotate(false);
//		chaseCam.setMinVerticalRotation((float)Math.PI / -2f + 0.001f);
//		chaseCam.setInvertVerticalAxis(true);
//		cam.setFrustumFar(1000.0f);
	}

}

