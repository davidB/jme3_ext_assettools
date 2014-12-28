package jme3_ext_remote_editor;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import pgex.Cmds.Cmd;
import pgex.Cmds.SetEye;
import pgex.Datas.Data;

import com.jme3.app.SimpleApplication;
import com.jme3.light.LightList;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;


/**
 * Handles a server-side channel.
 */
//TODO manage env per connection : remove data from the connection when connection close.
@RequiredArgsConstructor
public class RemoteHandler {
	//Metrics metrics = new Metrics();
	final Executor executor = Executors.newSingleThreadExecutor();
	final RemoteCtx remoteCtx = new RemoteCtx();

	public final SimpleApplication app;
	public final Pgex pgex;

	public void enable() throws Exception {
		ViewPort vp = app.getRenderManager().createPostView("remoteHandler_" + System.currentTimeMillis(), app.getCamera());
		vp.addProcessor(remoteCtx.view);
		app.getRootNode().attachChild(remoteCtx.root);
		System.out.println("enable");
	}

	public void disable() throws Exception {
		//TODO only clean root when no remote client
		remoteCtx.root.detachAllChildren();
		LightList ll = remoteCtx.root.getLocalLightList();
		for(int i = ll.size() - 1; i > -1; i--) ll.remove(i);
		app.getRootNode().detachChild(remoteCtx.root);
		remoteCtx.view.getViewPort().removeProcessor(remoteCtx.view);
		System.out.println("disable");
	}

	public void channelRead(ChannelHandlerContext ctx, Object msg0) {
		ByteBuf msg = (ByteBuf)msg0;
		byte k = msg.readByte();
		try {
			switch(k) {
				case Protocol.Kind.askScreenshot : askScreenshot(ctx, msg); break;
				case Protocol.Kind.pgexCmd : pgexCmd(ctx, msg); break;
				default : System.out.println("Unsupported kind of message : " + k);
			}
		} catch(Exception exc) {
			exc.printStackTrace();
		}
	}

	void askScreenshot(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		int w = msg.readInt();
		int h = msg.readInt();
		msg.release();
		enqueue((rc)->{
			rc.view.askReshape.set(new SceneProcessorCopyToBGRA.ReshapeInfo(w, h, true));
			//TODO run notify in async (in an executor)
			rc.view.askNotify.set((bytes) -> {
				if (bytes.limit() != (w * h * 4)) {
					System.out.printf("bad size : %d != %d \n", bytes.limit(), w*h*4 );
					return false;
				}
				executor.execute(() -> {
					ByteBuf out = null;
					synchronized (bytes) {
						out = wrappedBuffer(bytes);  // performance
						//out = copiedBuffer(bytes);  //secure
					}
					ByteBuf header = ctx.alloc().buffer(4+1);
					header.writeInt(out.readableBytes());
					header.writeByte(Protocol.Kind.rawScreenshot);
					ctx.write(header);
					ctx.writeAndFlush(out);
				});
				return true;
			});
		});
	}

	void pgexCmd(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		try {
			byte[] b = new byte[msg.readableBytes()];
			msg.readBytes(b);
			Cmd cmd0 = Cmd.parseFrom(b);
			switch(cmd0.getCmdCase()) {
				case SETEYE: setEye(ctx, cmd0.getSetEye()); break;
				case SETDATA: setData(ctx, cmd0.getSetData()); break;
				//case : setCamera(ctx, cmd0); break;
				default:
					System.out.println("unsupported cmd : " + cmd0.getCmdCase().name());
			}
		} catch(Exception exc) {
			exc.printStackTrace();
		}
	}

	void setData(ChannelHandlerContext ctx, Data data) {
		enqueue((rc)-> {
			pgex.merge(data, rc.root);
			System.out.println("setData : dump rc.root");
			dump(rc.root, "");
		});
	}

	void setEye(ChannelHandlerContext ctx, SetEye cmd) {
		enqueue((rc)-> {
			CameraNode cam = rc.cam;
			Quaternion rot = pgex.cnv(cmd.getRotation(), cam.getLocalRotation());
			cam.setLocalRotation(rot.clone());
			cam.setLocalTranslation(pgex.cnv(cmd.getLocation(), cam.getLocalTranslation()));
			cam.setCamera(rc.view.getViewPort().getCamera());
			if (cmd.hasNear()) cam.getCamera().setFrustumNear(cmd.getNear());
			if (cmd.hasFar()) cam.getCamera().setFrustumFar(cmd.getFar());
			if (cmd.hasProjection()) cam.getCamera().setProjectionMatrix(pgex.cnv(cmd.getProjection(), new Matrix4f()));
			cam.getCamera().update();
			cam.setEnabled(true);
		});
	}

	public void enqueue(Consumer<RemoteCtx> f) {
		app.enqueue(() -> { f.accept(remoteCtx); return null;});
	}

	void dump(Node node, String prefix) {
		List<Spatial> children = node.getChildren();
		System.out.printf("%s %s (%d)\n", prefix, node.getName(), children.size());
		prefix = (prefix.length() == 0)? " +--":  ("\t"+ prefix);
		for (Spatial sp : children) {
			if (sp instanceof Node) dump((Node) sp, prefix);
			else System.out.printf("%s %s [%s]\n", prefix, sp.getName(), sp.getClass().getName());
		}
	}
}