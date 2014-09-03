package com.hoolai.chat.bo.task;

import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.hoolai.chat.bo.ChatContext;
import com.hoolai.chat.bo.ChatPlayer;

public class TimeOutPlayerExistTask implements Runnable {

	private static final Logger logger = Logger.getLogger("chatServer");
	
	private final ChatContext chatContext;
	
	public TimeOutPlayerExistTask(ChatContext chatContext){
		this.chatContext = chatContext;
	}

	@Override
	public void run() {
		checkPlayersExist(chatContext.connectedPlayers());
		checkPlayersExist(chatContext.loggedPlayers());
	}
	
	private void checkPlayersExist(Collection<ChatPlayer> players){
		try {
			Iterator<ChatPlayer> it = players.iterator();
			while (it.hasNext()) {
				ChatPlayer player = it.next();
				if (player == null) {
					it.remove();
					continue;
				}
				checkPlayerExist(player);
			}
		} catch (Exception e) {
			logger.error("check Player Exist Heart task meet an exception", e);
		}
	}

	private void checkPlayerExist(ChatPlayer player) {
		if(!player.isHeartTimeOut() && !player.isIdleTimeOut()){
			return;
		}
		logger.debug("player heart timeout ! userId:" + player.getUserId());
		System.out.println("player heart timeout ! userId:" + player.getUserId()+",channelId:"+player.getChannelId());
		player.exit(chatContext);
	}

}
