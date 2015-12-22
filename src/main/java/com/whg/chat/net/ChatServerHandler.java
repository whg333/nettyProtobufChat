package com.whg.chat.net;

import static com.whg.chat.bo.ChatMsg.repeatLogin;
import static com.whg.chat.bo.ChatMsg.wrap;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import com.whg.chat.bo.ChatContext;
import com.whg.chat.bo.ChatMsg;
import com.whg.chat.bo.ChatPlayer;
import com.whg.chat.codec.ChatCommand;
import com.whg.chat.proto.ChatProto.ChatMsgProto;

public class ChatServerHandler extends SimpleChannelUpstreamHandler{
	
	private static Logger logger = Logger.getLogger("chatServer");
	
    public static final String REQUEST_STRING = "<policy-file-request/>" + '\0';
    public static final String RESPOND_STRING = "<cross-domain-policy><allow-access-from domain=\"*\" to-ports=\"*\"/></cross-domain-policy>" + '\0';
	
    private ChatContext context = ChatContext.getInstance();

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		logger.debug("server channel connected:" + ctx.getChannel());
		
		ChatPlayer player = new ChatPlayer(ctx.getChannel());
        ctx.setAttachment(player);
        if(!context.addConnectedPlayer(player)) {
            logger.error("someone connected,but have a player with the same playerId(channelId)");
            ctx.setAttachment(null);
            ctx.getChannel().close();
        }
        
        logger.debug("connected size:" + context.connectedPlayerSize());
    }
	
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        logger.debug("server messageReceived...");
        
        if(e.getMessage() instanceof ChatCommand) {
        	ChatPlayer player = (ChatPlayer) ctx.getAttachment();
            if (player == null) {
                logger.error("get synRoomPlayer fail but we an msg, do not process");
                ctx.getChannel().close();
            } else {
            	ChatCommand cmd = (ChatCommand) e.getMessage();
            	ChatMsgProto msgProto = (ChatMsgProto)cmd.getProto();
                if(msgProto == null){
                	logger.error("receive msg is null!");
                }else{
                	ChatMsg msg = wrap(msgProto);
                	if(player.synUserInfo(msg)){
                		transformConnectedToLogged(player);
                	}
	                context.handleChatMsg(player, msg);
	                logger.debug(msg);
	                //e.getChannel().write(msg.toBuilder());
                }
            }
        } else {
            String msg = (String) e.getMessage();
            if (REQUEST_STRING.equals(msg)) {
                ctx.getChannel().write(RESPOND_STRING).addListener(ChannelFutureListener.CLOSE);
            } else {
                ctx.getChannel().close();
            }
            System.out.println(msg);
        }
        
//        ChatMsgProto msg = (ChatMsgProto) e.getMessage();
//        if(msg == null){
//        	logger.error("receive msg is null!");
//        }
//        boolean offered = msgQueue.offer(msg);
//        Assert.isTrue(offered);
//        
//        System.out.println(msgQueue.size());
//        
//        System.out.println(msg);
//        e.getChannel().write(msg);
    }
	
	private void transformConnectedToLogged(ChatPlayer player){
		if(context.removeConnectedPlayer(player)){
    		if(context.addLoggedPlayer(player)){
    			if(context.addUnionPlayers(player)){
            		logger.debug("添加联盟成员成功:"+player);
            	}
    		}else{
    			processRepeatLogged(player);
    		}
    	}
	}
	
	private void processRepeatLogged(ChatPlayer player){
		logger.debug("发现重复登录:"+player);
		player.sendMsg(repeatLogin().toBuilder());
		
		String errMsg = null;
		long userId = player.getUserId();
		ChatPlayer loggedPlayer = context.findLoggedPlayer(userId);
		if(loggedPlayer == null){
			errMsg = "can't found logged player:"+userId;
			logger.error(errMsg);
			throw new IllegalArgumentException(errMsg);
		}
		
		loggedPlayer.exit(context);
		if(!context.addLoggedPlayer(player)){
			errMsg = "can't add connect player:"+userId;
			logger.error(errMsg);
			throw new IllegalArgumentException(errMsg);
		}
	}
	
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        logger.error("upstream handler catch an exception", e.getCause());
        disconnectedPlayer(ctx);
    }
	
	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		logger.debug("server channel disconnected:" + ctx.getChannel());
		disconnectedPlayer(ctx);
    }
	
	private void disconnectedPlayer(ChannelHandlerContext ctx){
		ChatPlayer player = (ChatPlayer) ctx.getAttachment();
        if(player != null) {
        	ctx.setAttachment(null);
            player.exit(context);
        }
	}
	
}
