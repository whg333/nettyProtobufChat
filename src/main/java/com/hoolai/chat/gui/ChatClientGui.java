package com.hoolai.chat.gui;

import static org.jboss.netty.channel.Channels.pipeline;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import com.hoolai.chat.bo.ChatMsg;
import com.hoolai.chat.bo.ChatMsg.MsgType;
import com.hoolai.chat.codec.ChatCommand;
import com.hoolai.chat.codec.ChatProtoBufDecoder;
import com.hoolai.chat.codec.ChatProtoBufEncoder;
import com.hoolai.chat.codec.LengthFieldBasedFrameDecoder;
import com.hoolai.chat.proto.ChatProto.ChatMsgProto;

public class ChatClientGui extends JFrame{
	
	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger("chatServer");

	private JLabel ipLabel, portLabel;
	private JTextField ipFiled, portFiled;
	private JTextArea sendArea, receiveArea;
	private JButton connectButton, sendButton, pingButton, closeButton;
	
	private ClientBootstrap bootstrap;
	private volatile Channel channel;
	private final BlockingQueue<ChatMsg> response = new LinkedBlockingQueue<ChatMsg>();
	private ScheduledExecutorService scheduleExecutor;
	
	public ChatClientGui(){
		super("消息服务器GUI测试");
		setLayout(new FlowLayout()); 
		
		ipLabel = new JLabel("消息服务器IP：");
		portLabel = new JLabel("消息服务器port端口：");
		ipFiled = new JTextField("localhost", 8);
		portFiled = new JTextField("8007", 5);
		connectButton = new JButton("连接");
		sendArea = new JTextArea(10, 50);
		receiveArea = new JTextArea(10, 50);
		sendButton = new JButton("发送");
		pingButton = new JButton("Ping");
		closeButton = new JButton("关闭");
		
		add(connectButton);
		add(pingButton);
		add(ipLabel);
		add(ipFiled);
		add(portLabel);
		add(portFiled);
		add(closeButton);
		add(sendArea);
		add(sendButton);
		add(receiveArea);
		
		connectButton.setEnabled(true);
		connectButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable(){
					@Override
					public void run(){
						if(connect()){
							startHeartbeat();
							connectButton.setEnabled(false);
							pingButton.setEnabled(true);
							closeButton.setEnabled(true);
							sendButton.setEnabled(true);
							sendArea.setEditable(true);
						}
					}
				});
			}
		});
		
		pingButton.setEnabled(false);
		pingButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						if (ping()) {
							receiveArea.setText("ping received pong successful~!");
						}
					}
				});
			}
		});
		
		closeButton.setEnabled(false);
		closeButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable(){
					@Override
					public void run(){
						disconnect();
						stopHeartbeat();
						connectButton.setEnabled(true);
						pingButton.setEnabled(false);
						closeButton.setEnabled(false);
						sendButton.setEnabled(false);
						sendArea.setEditable(false);
					}
				});
			}
		});
		
		sendButton.setEnabled(false);
		sendButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						sedMsg(sendArea.getText());
					}
				});
			}
		});
		
		sendArea.setEditable(false);
		receiveArea.setEditable(false);
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(150, 130, 600, 450);
		setVisible(true);
		setResizable(false);
	}
	
//	  private Socket socket;
//    private BufferedReader reader;
//    private BufferedWriter writer;
	
	private boolean connect(){
//     socket = new Socket("192.168.20.12", 8085);
//     System.out.println("socket connect success!");
//     reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//     writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            
        bootstrap = new ClientBootstrap(
                new NioClientSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool()));

        // Set up the pipeline factory.
        bootstrap.setPipelineFactory(new ChannelPipelineFactory(){
        	public ChannelPipeline getPipeline(){
        		return pipeline(
        		        new LengthFieldBasedFrameDecoder(81920, 0, 4, 0, 4, true, true),
        		        new ChatProtoBufDecoder(),
        		        new ChatProtoBufEncoder(),
        		        new ChatClientGuiHandler()
        		);
        	}
        });

        // Start the connection attempt.
        SocketAddress address = new InetSocketAddress(ipFiled.getText(), Integer.parseInt(portFiled.getText()));
        ChannelFuture future = bootstrap.connect(address);
        channel = future.awaitUninterruptibly().getChannel();
        if (!future.isSuccess()) {
            future.getCause().printStackTrace();
            bootstrap.releaseExternalResources();
			return false;
        }
        
        return true;
	}
	
	/**
	 * 启动心跳调度任务，防止被Chat Server踢掉
	 */
	private void startHeartbeat(){
		if(scheduleExecutor == null){
			scheduleExecutor = Executors.newScheduledThreadPool(1);
		}
		
		scheduleExecutor.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				heartbeat();
			}
		}, TimeUnit.SECONDS.toMillis(1), TimeUnit.SECONDS.toMillis(3), TimeUnit.MILLISECONDS);
		
		logger.info("ChatClientGui Schedule threads start...");
	}
	
	private void stopHeartbeat(){
		scheduleExecutor.shutdown();
		scheduleExecutor = null;
	}
	
	private void heartbeat(){
		channel.write(ChatMsg.heartbeatProto().toBuilder());
	}
	
	private void disconnect(){
		// Close the connection. Make sure the close operation ends because
		// all I/O operations are asynchronous in Netty.
		channel.close().awaitUninterruptibly();
		
		// Shut down all thread pools to exit.
		bootstrap.releaseExternalResources();
	}
	
	private void sedMsg(String msgStr){
		ChatMsgProto msg = ChatMsg.testProto(msgStr);
		channel.write(msg.toBuilder()).addListener(new ChannelFutureListener(){
			@Override
			public void operationComplete(ChannelFuture future){
				if(future.getChannel().isReadable()){
					//System.out.println("sedMsg success");
				}
			}
		});
	}
	
	private boolean ping(){
		ChatMsgProto msg = ChatMsg.pingProto();
		channel.write(msg.toBuilder());
		
		ChatMsg responseMsg;
        boolean interrupted = false;
        for (;;) {
            try {
            	responseMsg = response.take();
                break;
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        
        if(responseMsg.getMsg().equals("pong")){
        	return true;
        }
        return false;
	}
	
	private final class ChatClientGuiHandler extends SimpleChannelUpstreamHandler{
		
		@Override
	    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
	    	logger.info("client channel connected:" + ctx.getChannel());
	    	
//	    	ChatMsgProto msg = ChatMsg.testProto("whg");
//	        e.getChannel().write(msg.toBuilder());
	        
	        receiveArea.setText("client channel connected:" + ctx.getChannel());
	    }

	    @Override
	    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
//	    	logger.info("client messageReceived...");
	        if(e.getMessage() instanceof ChatCommand) {
	        	ChatCommand cmd = (ChatCommand) e.getMessage();
	        	ChatMsgProto msgProto = (ChatMsgProto) cmd.getProto();
	        	ChatMsg msg = ChatMsg.wrap(msgProto);
	        	if(msgProto.getMsg().equals("pong")){
	        		response.offer(msg);
	        	}else if(msg.getType() != MsgType.heartMsg){
	        		receiveArea.setText(msgProto.toString());
	        	}
	        } else {
	        	logger.error("client received Undefined msg");
	        }
	    }

	    @Override
	    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
			logger.error("Unexpected exception from downstream.", e.getCause());
			receiveArea.setText("Unexpected exception from downstream.\n" + e.getCause());
	        e.getChannel().close();
	    }
	    
	    @Override
		public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			logger.info("client channel disconnected:" + ctx.getChannel());
			receiveArea.setText("client channel disconnected:" + ctx.getChannel());
			e.getChannel().close();
	    }
	}
	
	public static void main(String[] arg) {
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run(){
				new ChatClientGui();
			}
		});
	}

}