package com.whg.chat.net;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import com.whg.chat.bo.ChatContext;

public class ChatServer {
	
	private static Logger logger = Logger.getLogger("chatServer");
	public static final int PORT = 8007;

	public static void main(String[] args) throws Exception {
		start();
    }
	
	public static void start(){
		// Configure the server.
        ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool()));

        // Set up the event pipeline factory.
        bootstrap.setPipelineFactory(new ChatServerPipelineFactory());

        // Bind and start to accept incoming connections.
        SocketAddress address = new InetSocketAddress(PORT);
        bootstrap.bind(address);
        logger.info("ChatServer start on ... "+address);
        ChatContext.start();
	}
	
}
