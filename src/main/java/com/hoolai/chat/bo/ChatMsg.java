package com.hoolai.chat.bo;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hoolai.chat.proto.ChatProto.ChatMsgProto;
import com.hoolai.chat.util.StringUtil;

public class ChatMsg {
	
	public enum MsgType{
		ping(-1,-1),
		heartMsg(-1, 0),
		sysMsg(0, 0),
		robMineSysMsg(0, 1),
		worldMsg(1, 0), 
		unionMsg(2,0), 
		privateMsg(3,0),
		friendApply(3,1),	//申请好友时发送此类型
		addFriend(3,2),	//同意好友申请是发送此类型
		currentChangeMsg(4,0);	//当前信息改变推前端时发送此类型
		
		private int mainType;
		private int subType;
		private MsgType(int mainType, int subType){
			this.mainType = mainType;
			this.subType = subType;
		}
		public static MsgType valueOf(int mainType, int subType){
			for(MsgType type:values()){
				if(type.mainType == mainType 
						&& type.subType == subType){
					return type;
				}
			}
			throw new IllegalArgumentException("没有查找到对应的消息类型");
		}
	}

	private MsgType type;
	private int channelId;
	private long userId;
	private String userName;
	private long targetUserId;
	private String targetUserName;
	private int unionId;
	private String msg;
	
	public static ChatMsg wrap(ChatMsgProto chatMsgProto){
		return new ChatMsg(chatMsgProto);
	}
	
	public static ChatMsgProto testProto(){
		return testProto("whgTest");
	}
	
	private static final String javaClientUserIdStr = "-200";
	
	public static ChatMsgProto testProto(String msg){
		return ChatMsgProto.newBuilder()
		.setMainType(0)
		.setSubType(0)
		.setChannelId(23)
		.setUserId(javaClientUserIdStr)
		.setUnionId(2)
		.setMsg(msg)
		.build();
	}
	
	public static ChatMsgProto pingProto(){
		return ChatMsgProto.newBuilder()
		.setMainType(-1)
		.setSubType(-1)
		.setUserId(javaClientUserIdStr)
		.setMsg("ping")
		.build();
	}
	
	public static ChatMsgProto heartbeatProto(){
		return ChatMsgProto.newBuilder()
		.setMainType(-1)
		.setSubType(0)
		.setUserId(javaClientUserIdStr)
		.setMsg("heartbeat...")
		.build();
	}
	
	public static ChatMsgProto repeatLogin(){
		return ChatMsgProto.newBuilder()
		.setMainType(-100)
		.setSubType(0)
		.setMsg("重复登录")
		.build();
	}
	
	public static ChatMsgProto applyGameFriendMsg(String userName){
		return ChatMsgProto.newBuilder()
		.setMainType(3)
		.setSubType(1)
		.setMsg("玩家"+userName+"申请加您为好友")
		.build();
	}
	
	public static ChatMsgProto addGameFriendMsg(String userName){
		return ChatMsgProto.newBuilder()
		.setMainType(3)
		.setSubType(2)
		.setMsg("玩家"+userName+"同意加您为好友")
		.build();
	}
	
	public static ChatMsgProto leaveCommentMsg(String userName){
		return ChatMsgProto.newBuilder()
		.setMainType(3)
		.setSubType(3)
		.setMsg("玩家"+userName+"给您留言了")
		.build();
	}
	
	public ChatMsg(){
		
	}

	private ChatMsg(ChatMsgProto chatMsgProto) {
		copyFrom(chatMsgProto);
	}
	
	public void parseFrom(byte[] bytes) {
        try {
        	ChatMsgProto proto = ChatMsgProto.parseFrom(bytes);
            copyFrom(proto);
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    public void copyFrom(GeneratedMessage proto) {
    	ChatMsgProto chatMsgProto = (ChatMsgProto) proto;
    	type = MsgType.valueOf(chatMsgProto.getMainType(), chatMsgProto.getSubType());
    	channelId = chatMsgProto.getChannelId();
    	if(chatMsgProto.hasUserId() && !StringUtil.isEmpty(chatMsgProto.getUserId())){
    		userId = Long.parseLong(chatMsgProto.getUserId());
    	}
    	if(chatMsgProto.hasUserName()){
    		userName = chatMsgProto.getUserName();
    	}
    	if(chatMsgProto.hasTargetUserId() && !StringUtil.isEmpty(chatMsgProto.getTargetUserId())){
    		targetUserId = Long.parseLong(chatMsgProto.getTargetUserId());
    	}
    	if(chatMsgProto.hasTargetUserName()){
    		targetUserName = chatMsgProto.getTargetUserName();
    	}
    	unionId = chatMsgProto.getUnionId();
    	msg = chatMsgProto.getMsg();
    }

	public byte[] toByteArray() {
    	ChatMsgProto chatMsgProto = (ChatMsgProto) copyTo();
        return chatMsgProto.toByteArray();
    }
    
    public GeneratedMessage copyTo() {
    	ChatMsgProto.Builder builder = ChatMsgProto.newBuilder();
    	builder.setMainType(type.mainType);
    	builder.setSubType(type.subType);
    	builder.setChannelId(channelId);
    	builder.setTargetUserId(targetUserId+"");
    	if(userName != null){
    		builder.setUserName(userName);
    	}
    	if(targetUserName != null){
    		builder.setTargetUserName(targetUserName);
    	}
    	builder.setUserId(userId+"");
    	builder.setUnionId(unionId);
    	builder.setMsg(msg);
        return builder.build();
    }
    
    public long getTargetUserId() {
		return targetUserId;
	}
    
	public long getUserId() {
		return userId;
	}

	public int getUnionId() {
		return unionId;
	}

	public boolean hasUserName() {
		return userName != null;
	}

	public String getUserName() {
		return userName;
	}
	
	public boolean hasUnion(){
		return unionId > 0;
	}
	
	public String getTargetUserName() {
		return targetUserName;
	}
	
	public void setType(MsgType type) {
		this.type = type;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}
	
	public Object getMsg() {
		return msg;
	}
	
    public int getChannelId() {
		return channelId;
	}
    
    public MsgType getType() {
		return type;
	}

	@Override
	public String toString() {
		return "ChatMsg [channelId=" + channelId + ", msg=" + msg + ", targetUserId=" + targetUserId + ", targetUserName=" + targetUserName
				+ ", type=" + type + ", unionId=" + unionId + ", userId=" + userId + ", userName=" + userName + "]";
	}

}
