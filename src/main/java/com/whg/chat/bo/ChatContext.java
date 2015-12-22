package com.whg.chat.bo;

import static com.whg.chat.bo.ChatMsg.addGameFriendMsg;
import static com.whg.chat.bo.ChatMsg.applyGameFriendMsg;
import static com.whg.chat.bo.ChatMsg.leaveCommentMsg;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.whg.chat.bo.ChatMsg.MsgType;
import com.whg.chat.bo.task.TimeOutPlayerExistTask;
import com.whg.chat.bo.task.WorldMsgBroadcastTask;
import com.whg.chat.util.ConcurrentHashMap;

public class ChatContext{
	
	/** 表示一个帧的最大长度，此处是8kb */
	public static final int max_frame_length_8kb = 81920;
	
	/** 表示一个帧长度所占的字节，此处是一个int长度 ，即4个字节*/
	public static final int frame_length_4b = 4;
	
	/** 表示protobuf命令所占的长度，此处是一个short长度，即2个字节 */
	public static final int protobuf_cmd_length_2b = 2;
	
	private static Logger logger = Logger.getLogger("chatServer");

	private final ConcurrentHashMap<Integer, ChatPlayer> connectedPlayers = new ConcurrentHashMap<Integer, ChatPlayer>(1000);
	private final ConcurrentHashMap<Long, ChatPlayer> loggedPlayers = new ConcurrentHashMap<Long, ChatPlayer>(1000);
	private final ConcurrentHashMap<Integer, Set<Long>> unions = new ConcurrentHashMap<Integer, Set<Long>>(500);
	
	private final BlockingQueue<ChatMsg> worldMsgQueue = new LinkedBlockingQueue<ChatMsg>();
	private final ExecutorService  executor = Executors.newCachedThreadPool();
	
	private static final ChatContext instance = new ChatContext();
	
	private ChatContext(){
//		for(int i=0;i<3;i++){
//			ChatMsgProto msgProto = ChatMsg.testProto();
//			worldMsgQueue.offer(ChatMsg.wrap(msgProto));
//		}
	}
	
	public static void start(){
		instance.startSchedule();
	}
	
	public static ChatContext getInstance(){
		return instance;
	}
	
	public boolean addConnectedPlayer(ChatPlayer player){
		return connectedPlayers.putIfAbsent(player.getChannelId(), player) == null;
	}
	
	public boolean removeConnectedPlayer(ChatPlayer player) {
        return this.connectedPlayers.remove(player.getChannelId()) != null;
    }
	
	public int connectedPlayerSize(){
		return connectedPlayers.size();
	}
	
	public Collection<ChatPlayer> connectedPlayers(){
		return connectedPlayers.values();
	}
	
	public boolean addLoggedPlayer(ChatPlayer player){
		return loggedPlayers.putIfAbsent(player.getUserId(), player) == null;
	}
	
	public boolean removeLoggedPlayer(ChatPlayer player) {
        return this.loggedPlayers.remove(player.getUserId()) != null;
    }
	
	public ChatPlayer findLoggedPlayer(long userId){
		return loggedPlayers.get(userId);
	}
	
	public int loggedPlayerSize(){
		return loggedPlayers.size();
	}
	
	public Collection<ChatPlayer> loggedPlayers(){
		return loggedPlayers.values();
	}
	
	public boolean addUnionPlayers(ChatPlayer player){
		int unionId = player.getUnionId();
		if(unionId <= 0){
			return false;
		}
		long userId = player.getUserId();
		if(unions.containsKey(unionId)){
			Set<Long> unionPlayers = unions.get(unionId);
			return unionPlayers.add(userId);
		}else{
			Set<Long> unionPlayer = new HashSet<Long>();
			unionPlayer.add(userId);
			return unions.putIfAbsent(unionId, unionPlayer) == null;
		}
	}
	
	public boolean removeUnionPlayer(ChatPlayer player) {
		int unionId = player.getUnionId();
		if(!unions.containsKey(unionId)){
			return true;
		}
		Set<Long> unionPlayers = unions.get(unionId);
		if(unionPlayers == null){
			return true;
		}
		return unionPlayers.remove(player.getUserId());
		
    }
	
	public int unionPlayerSize(){
		return unions.size();
	}
	
	public void handleChatMsg(final ChatPlayer player, final ChatMsg msg){
		Runnable chatMsgTask = chatMsgTask(player, msg);
		if(chatMsgTask == null){
			return;
		}
		execute(chatMsgTask);
	}
	
	private Runnable chatMsgTask(final ChatPlayer player, final ChatMsg msg){
		player.refreshLastOperateTime();
		
		switch(msg.getType()){
		case ping:
			return new Runnable(){
				@Override
				public void run(){
					responsePong(player, msg);
				}
			};
		case heartMsg:
			return new Runnable(){
				@Override
				public void run(){
					refreshHeartTime(player, msg);
				}
			};
		case sysMsg:
		case worldMsg:
			return new Runnable(){
				@Override
				public void run(){
					addWorldMsgQueue(msg);
				}
			};
		case unionMsg:
			return new Runnable(){
				@Override
				public void run(){
					sendMsgToUnion(msg);
				}
			};
		case privateMsg:
			return new Runnable(){
				@Override
				public void run(){
					sendMsgToLoggedPlayer(msg.getTargetUserId(), msg);
					sendMsgToLoggedPlayer(msg.getUserId(), msg);
				}
			};
		default:
			logger.info("不支持的消息类型："+msg.getType());
			return null;
		}
	}
	
	public void execute(Runnable task){
		executor.execute(task);
	}
	
	public boolean addSysMsg(long userId, String userName, String msg){
		ChatMsg sysMsg = new ChatMsg();
		sysMsg.setType(MsgType.sysMsg);
		sysMsg.setUserId(userId);
		sysMsg.setUserName(userName);
		sysMsg.setMsg(msg);
		return addWorldMsgQueue(sysMsg);
	}
	
	
	public boolean addRobMineSysMsg(){
		ChatMsg sysMsg = new ChatMsg();
		sysMsg.setType(MsgType.robMineSysMsg);
		sysMsg.setUserId(-1);
		sysMsg.setUserName("");
		sysMsg.setMsg("");
		return addWorldMsgQueue(sysMsg);
	}
	
	public boolean addWorldMsgQueue(ChatMsg msg){
		return worldMsgQueue.offer(msg);
	}
	
	private void sendMsgToUnion(ChatMsg msg){
		Set<Long> unionPlayers = unions.get(msg.getUnionId());
		if(unionPlayers == null){
			return;
		}
		for(Long playerUserId:unionPlayers){
			sendMsgToLoggedPlayer(playerUserId, msg);
		}
	}
	
	public void sendMsgToLoggedPlayer(long userId, ChatMsg msg){
		ChatPlayer player = findLoggedPlayer(userId);
		if(player == null){
			return;
		}
		player.sendMsg(msg.copyTo().toBuilder());
	}
	
	public void applyGameFriend(long userId, final String userName, final long targetUserId) {
		execute(
			new Runnable(){
				@Override
				public void run(){
					ChatPlayer friend = findLoggedPlayer(targetUserId);
					if(friend != null){
						friend.sendMsg(applyGameFriendMsg(userName).toBuilder());
					}
				}
			}
		);
	}
	
	public void addGameFriend(long userId, final String userName, final long targetUserId) {
		execute(
			new Runnable(){
				@Override
				public void run(){
					ChatPlayer friend = findLoggedPlayer(targetUserId);
					if(friend != null){
						friend.sendMsg(addGameFriendMsg(userName).toBuilder());
					}
				}
			}
		);
	}
	
	public void leaveComment(long userId, final String userName, final long targetUserId){
		execute(
			new Runnable(){
				@Override
				public void run(){
					ChatPlayer friend = findLoggedPlayer(targetUserId);
					if(friend != null){
						friend.sendMsg(leaveCommentMsg(userName).toBuilder());
					}
				}
			}
		);
	}
	
	private void refreshHeartTime(ChatPlayer player, ChatMsg msg){
		player.refreshHeartTime();
		player.sendMsg(msg.copyTo().toBuilder());
	}
	
	private void responsePong(ChatPlayer player, ChatMsg msg){
		msg.setMsg("pong");
		player.sendMsg(msg.copyTo().toBuilder());
	}
	
	public boolean worldMsgQueueIsEmpty(){
		return worldMsgQueue.isEmpty();
	}
	
	public ChatMsg takeFromWorldMsgQueue(){
		try {
			return worldMsgQueue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void startSchedule(){
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
		// 每500毫秒（即半秒）从系统消息队列里面拿消息广播
		executor.scheduleAtFixedRate(new WorldMsgBroadcastTask(this), TimeUnit.SECONDS.toMillis(5), TimeUnit.MILLISECONDS.toMillis(500), TimeUnit.MILLISECONDS);
		// 加入心跳
		executor.scheduleAtFixedRate(new TimeOutPlayerExistTask(this), TimeUnit.SECONDS.toMillis(5), TimeUnit.SECONDS.toMillis(5), TimeUnit.MILLISECONDS);
		// 生成系统测试信息
		//executor.scheduleAtFixedRate(new WorldMsgTestGeneratorTask(this), TimeUnit.SECONDS.toMillis(5), TimeUnit.SECONDS.toMillis(5), TimeUnit.MILLISECONDS);
		logger.info("ChatServerHandler Schedule threads start...");
	}
	
}
