package jme3_ext_remote_editor;

import static io.netty.buffer.Unpooled.wrappedBuffer;

import com.jme3.app.SimpleApplication;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import pgex.Cmds.Cmd;
import pgex.Cmds.SetCamera;

/**
 * Handles a server-side channel.
 */
public class ServerHandler4Capture extends ChannelInboundHandlerAdapter {
	SceneProcessorCopyToBGRA capturer;
	SimpleApplication jme;

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
			System.out.println(">>>" +  w + " x " + h);
			capturer.askReshape.set(new SceneProcessorCopyToBGRA.ReshapeInfo(w, h, true));
			//TODO run notify in async (in an executor)
			capturer.askNotify.set((bytes) -> {
				ByteBuf out = null;
				synchronized (bytes) {
					if (bytes.limit() != (w * h * 4)) {
						System.out.printf("bad size : %d != %d \n", bytes.limit(), w*h*4 );
						return false;
					}
					out = wrappedBuffer(bytes);  // performance
					//out = copiedBuffer(bytes);  //secure
				}
				ByteBuf header = ctx.alloc().buffer(4+1);
				header.writeInt(out.readableBytes());
				header.writeByte(Protocol.Kind.rawScreenshot);
				ctx.write(header);
				ctx.writeAndFlush(out);
				System.out.println(">>> send : " + Protocol.Kind.rawScreenshot);
				return true;
			});
		} else if (k == Protocol.Kind.pgex_cmd) {
			System.out.println(">>> pgex_cmd");
			try {
				byte[] b = new byte[msg.readableBytes()];
				msg.readBytes(b);
				Cmd cmd0 = Cmd.parseFrom(b);
				switch(cmd0.getCmdCase()) {
					case SETCAMERA:
						jme.enqueue(()-> {
							SetCamera cmd = cmd0.getSetCamera();
							Camera cam = jme.getCamera();
							cam.setLocation(Pgex.cnv(cmd.getLocation(), cam.getLocation()));
							cam.setRotation(Pgex.cnv(cmd.getRotation(), cam.getRotation()));
							cam.setProjectionMatrix(Pgex.cnv(cmd.getProjection(), cam.getProjectionMatrix()));
							return null;
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