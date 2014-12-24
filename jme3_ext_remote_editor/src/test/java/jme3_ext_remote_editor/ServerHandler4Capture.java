package jme3_ext_remote_editor;

import static io.netty.buffer.Unpooled.wrappedBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import pgex.Cmds.Cmd;
import pgex.Cmds.SetCamera;

import com.jme3.scene.CameraNode;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;


/**
 * Handles a server-side channel.
 */
public class ServerHandler4Capture extends ChannelInboundHandlerAdapter {
	/** delegate the enqueue processing */
	public Consumer<Consumer<RemoteCtx>> enqueue;
	//Metrics metrics = new Metrics();
	final Executor executor = Executors.newSingleThreadExecutor();

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg0) {
		System.out.println(">>> channelRead");
		ByteBuf msg = (ByteBuf)msg0;
		byte k = msg.readByte();
		System.out.println(">>> channelRead : " + k);
		if (k == Protocol.Kind.askScreenshot) {
			int w = msg.readInt();
			int h = msg.readInt();
			msg.release();
			enqueue.accept((rc)->{
				System.out.println(">>>" +  w + " x " + h);
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
						System.out.println(">>> send : " + Protocol.Kind.rawScreenshot);
					});
					return true;
				});
			});
		} else if (k == Protocol.Kind.pgex_cmd) {
			System.out.println(">>> pgex_cmd");
			try {
				byte[] b = new byte[msg.readableBytes()];
				msg.readBytes(b);
				Cmd cmd0 = Cmd.parseFrom(b);
				switch(cmd0.getCmdCase()) {
					case SETCAMERA:
						enqueue.accept((rc)-> {
							SetCamera cmd = cmd0.getSetCamera();
							CameraNode cam = rc.cam;
							Quaternion rot = Pgex.cnv(cmd.getRotation(), cam.getLocalRotation());
							System.out.printf("rotation received: %s \n", rot);
							cam.setLocalRotation(rot.clone());
							System.out.printf("rot %s\n", cam.getLocalRotation());
							cam.setLocalTranslation(Pgex.cnv(cmd.getLocation(), cam.getLocalTranslation()));
							cam.setCamera(rc.view.getViewPort().getCamera());
//							cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
							cam.getCamera().setProjectionMatrix(Pgex.cnv(cmd.getProjection(), cam.getCamera().getProjectionMatrix()));
							cam.setEnabled(true);
							System.out.printf("location: %s / %s \n", cam.getLocalTranslation(), cam.getWorldTranslation());
							System.out.printf("rotation: %s / %s / %s \n", rot, cam.getLocalRotation(), cam.getWorldRotation());
							System.out.println(cam.getParent());
//							//cam.setRotation(transform.getRotation().mult(Pgex.cnv(cmd.getRotation(), cam.getRotation()), new Quaternion()));
//							System.out.printf("camera in : %s %s %s %s %s\n", cmd.getRotation(), cmd.getRotation().getX(), cmd.getRotation().getY(), cmd.getRotation().getZ(), cmd.getRotation().getW());
//							//cam.setRotation(Pgex.cnv(cmd.getRotation(), new Quaternion()));
//							//cam.setLocation(transform.transformVector(Pgex.cnv(cmd.getLocation(), cam.getLocation()), new Vector3f()));
//							//cam.setLocation(Pgex.cnv(cmd.getLocation(), cam.getLocation()));
//							cam.setFrame(
//								transform.transformVector(Pgex.cnv(cmd.getLocation(), cam.getLocation()), new Vector3f())
////								Pgex.cnv(cmd.getLocation(), cam.getLocation())
//								, transform.getRotation().mult(Pgex.cnv(cmd.getRotation(), cam.getRotation()), new Quaternion())
////								, Pgex.cnv(cmd.getRotation(), cam.getRotation()).mult(transform.getRotation(), new Quaternion())
////								, Pgex.cnv(cmd.getRotation(), cam.getRotation())
//							);
//							//cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
//							//cam.setProjectionMatrix(Pgex.cnv(cmd.getProjection(), cam.getProjectionMatrix()));
//							System.out.printf("camera in %s :: %s :: %s\n", cam.getHeight(), cam.getFrustumNear(), cam.getFrustumFar());
//							System.out.printf("camera %s :: %s :: %s\n", cam.getRotation(), cam.getLocation(), cam.getProjectionMatrix());
						});
						break;
					default:
						System.out.println("unsupported cmd : " + cmd0.getCmdCase().name());
				}
				System.out.println("cmd : " + cmd0);
			} catch(Exception exc) {
				exc.printStackTrace();
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
		// Close the connection when an exception is raised.
		cause.printStackTrace();
		ctx.close();
	}
}