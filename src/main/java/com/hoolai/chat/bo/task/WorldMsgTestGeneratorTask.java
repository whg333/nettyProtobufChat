package com.hoolai.chat.bo.task;

import java.util.Random;

import org.apache.log4j.Logger;

import com.hoolai.chat.bo.ChatContext;
import com.hoolai.chat.bo.ChatMsg;
import com.hoolai.chat.bo.ChatMsg.MsgType;

public class WorldMsgTestGeneratorTask implements Runnable{
	
	private static final Logger logger = Logger.getLogger("chatServer");
	private static final Random rnd = new Random();
	
	private final ChatContext chatContext;
	
	public WorldMsgTestGeneratorTask(ChatContext chatContext){
		this.chatContext = chatContext;
	}

	@Override
	public void run(){
		addSysTestMsg();
	}
	
	public void addSysTestMsg(){
		logger.debug("server sendMsgToAllPlayers...AllPlayersNum size:"+chatContext.connectedPlayerSize());
		chatContext.addWorldMsgQueue(generateTestSystemMsg());
		logger.debug("server sendMsgToAllPlayers...end");
	}
	
	private ChatMsg generateTestSystemMsg(){
		ChatMsg sysMsg = new ChatMsg();
		sysMsg.setType(MsgType.worldMsg);
		sysMsg.setUserId(+rnd.nextInt());
		sysMsg.setUserName("测试者：AsRoad"+rnd.nextInt());
		sysMsg.setMsg("测试一下"+rnd.nextInt());
		return sysMsg;
	}
	
}
