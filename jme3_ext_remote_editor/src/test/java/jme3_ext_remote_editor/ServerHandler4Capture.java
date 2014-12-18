package jme3_ext_remote_editor;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import static io.netty.buffer.Unpooled.*;

/**
 * Handles a server-side channel.
 */
public class ServerHandler4Capture extends ChannelInboundHandlerAdapter {
	SceneProcessorCopyToBGRA capturer;

	final static int K_REQ_IMAGE = 1;
	final static int K_SEND_IMAGE = 2;

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg0) {
		System.out.println(">>> channelRead");
		ByteBuf msg = (ByteBuf)msg0;
		byte k = msg.readByte();
		if (k == K_REQ_IMAGE) {
			int w = msg.readInt();
			int h = msg.readInt();
			msg.release();
			System.out.println(">>>" +  w + " x " + h);
			capturer.askReshape.set(new SceneProcessorCopyToBGRA.ReshapeInfo(w, h, true));
			//TODO run notify in async (in an executor)
			capturer.askNotify.set((ByteBuffer bytes) -> {
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
				header.writeByte(K_SEND_IMAGE);
				ctx.write(header);
				ctx.writeAndFlush(out);
				System.out.println(">>> send : " + K_SEND_IMAGE);
				return true;
			});
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
		// Close the connection when an exception is raised.
		cause.printStackTrace();
		ctx.close();
	}
}