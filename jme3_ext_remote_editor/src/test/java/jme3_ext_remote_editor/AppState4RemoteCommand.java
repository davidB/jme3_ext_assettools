package jme3_ext_remote_editor;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.util.function.Consumer;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;

public class AppState4RemoteCommand extends AbstractAppState {

	public int port = 4242;
	private ChannelFuture f;
	EventLoopGroup bossGroup;
	EventLoopGroup workerGroup;
	private SimpleApplication app;

	public final RemoteCtx remoteCtx = new RemoteCtx();

	void start() throws Exception {
		bossGroup = new NioEventLoopGroup();
		workerGroup = new NioEventLoopGroup();

		ServerBootstrap b = new ServerBootstrap();
		b.group(bossGroup, workerGroup)
		.channel(NioServerSocketChannel.class)
		.childHandler(new ChannelInitializer<SocketChannel>() { // (4)
			@Override
			public void initChannel(SocketChannel ch) throws Exception {
				ServerHandler4Capture c = new ServerHandler4Capture();
				c.enqueue = AppState4RemoteCommand.this::enqueue;
				ch.pipeline().addLast(
					new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 1, 4)
					,c
				);
			}
		})
		.option(ChannelOption.SO_BACKLOG, 128)
		.childOption(ChannelOption.SO_KEEPALIVE, true);

		// Bind and start to accept incoming connections.
		f = b.bind(port).sync();

	}

	void stop() throws Exception {
		if (workerGroup != null) workerGroup.shutdownGracefully();
		if (bossGroup != null) bossGroup.shutdownGracefully();
		if (f != null) f.channel().close().sync();
	}

	public void enqueue(Consumer<RemoteCtx> f) {
		app.enqueue(() -> { f.accept(remoteCtx); return null;});
	}

	public void initialize(com.jme3.app.state.AppStateManager stateManager0, com.jme3.app.Application app0) {
		try {
			app = (SimpleApplication)app0;
			start();
			app.getViewPort().addProcessor(remoteCtx.view);
			app.getRootNode().attachChild(remoteCtx.root);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void cleanup() {
		try {
			app.getRootNode().detachChild(remoteCtx.root);
			app.getViewPort().removeProcessor(remoteCtx.view);
			stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	};
}
