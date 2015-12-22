package com.whg.chat.bo;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

import com.google.protobuf.Message.Builder;
import com.whg.chat.util.TimeUtil;

public class ChatPlayer {
	
	private static Logger logger = Logger.getLogger("chatServer");
	
    private Channel channel;
    
	private long userId;
	private String userName;
	private int unionId;
	
	private AtomicLong heartTime = new AtomicLong(TimeUtil.currentTimeMillis());
	private AtomicLong lastOperateTime = new AtomicLong(TimeUtil.currentTimeMillis());
    
	public ChatPlayer(Channel channel) {
		this.channel = channel;
	}
	
	public void exit(ChatContext chatContext) {
		chatContext.removeConnectedPlayer(this);
		if(chatContext.removeLoggedPlayer(this)){
			chatContext.removeUnionPlayer(this);
		}
		if(channel != null){
			if(channel.isOpen()){
				channel.close();
			}
			channel = null;
		}
    }

	public Integer getChannelId() {
		return channel.getId();
	}
	
	public ChannelFuture sendMsg(Builder msg) {
        return channel.write(msg);
    }
	
	public long getUserId() {
		return userId;
	}
	
	public int getUnionId() {
		return unionId;
	}

	public boolean synUserInfo(ChatMsg msg) {
		if(userId <= 0){
			userId = msg.getUserId();
			if(userName == null && msg.hasUserName()){
				userName = msg.getUserName();
			}
			if(unionId <= 0 && msg.hasUnion()){
				unionId = msg.getUnionId();
			}
			logger.debug("同步玩家信息成功,channelId:"+channel.getId()+",userId:"+userId+",unionId:"+unionId);
			return true;
		}
		return false;
	}
	
	public boolean isHeartTimeOut(){
		return TimeUtil.currentTimeMillis() - heartTime.get() > getMaxHeartTimeOut();
	}
	
	public boolean isIdleTimeOut(){
		return TimeUtil.currentTimeMillis() - lastOperateTime.get() > getMaxIdleTimeOut();
	}
	
    /**
     * 如果超过最大心跳时间，则自动踢出，目前是2分钟
     * @return
     */
    public static long getMaxHeartTimeOut(){
    	 return TimeUnit.MINUTES.toMillis(2);
    }
    
    /**
     * 如果超过最大空闲时间，则自动踢出，目前是2小时
     * @return
     */
    public static long getMaxIdleTimeOut(){
    	return TimeUnit.HOURS.toMillis(2);
    }
    
	public void refreshHeartTime() {
		heartTime.set(TimeUtil.currentTimeMillis());
	}
	
	public void refreshLastOperateTime(){
		lastOperateTime.set(TimeUtil.currentTimeMillis());
	}

	@Override
	public String toString() {
		return "ChatPlayer [userId=" + userId + ", userName=" + userName + ", unionId=" + unionId + "]";
	}

}
