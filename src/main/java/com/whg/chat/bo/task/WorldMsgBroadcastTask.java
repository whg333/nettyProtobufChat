package com.whg.chat.bo.task;

import org.apache.log4j.Logger;

import com.whg.chat.bo.ChatContext;
import com.whg.chat.bo.ChatMsg;
import com.whg.chat.bo.ChatPlayer;

public class WorldMsgBroadcastTask implements Runnable{
	
	private static final Logger logger = Logger.getLogger("chatServer");
	
	private final ChatContext chatContext;
	
	public WorldMsgBroadcastTask(ChatContext chatContext){
		this.chatContext = chatContext;
	}

	@Override
	public void run(){
		try {
			sendMsgToAllPlayers();
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("send msg to all player occur error!", e.getCause());
		}
	}
	
	public void sendMsgToAllPlayers() throws InterruptedException{
		if(chatContext.worldMsgQueueIsEmpty()){
			return;
		}
		logger.debug("server sendMsgToAllPlayers...AllPlayersNum size:"+chatContext.connectedPlayerSize());
		ChatMsg msg = chatContext.takeFromWorldMsgQueue();
		if(msg == null){
			return;
		}
		for(ChatPlayer player:chatContext.loggedPlayers()){
        	player.sendMsg(msg.copyTo().toBuilder());
        }
		logger.debug("server sendMsgToAllPlayers...end");
	}
	
}
