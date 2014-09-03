package com.hoolai.chat.codec;

import static com.hoolai.chat.bo.ChatContext.*;
import static com.hoolai.chat.net.ChatServerHandler.RESPOND_STRING;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;

/**
 * 将ProtoBuff的Builder build后转换成Byte数组，然后写入ChannelBuffer里面，格式如下
 * <br/>+------------+---------------+-----------------+       
 * <br/>| length 4b  |  cmd type 2b  | protobuff bytes |  
 * <br/>+------------+---------------+-----------------+ 
 */
public class ChatProtoBufEncoder extends OneToOneEncoder {
    
    private static Logger logger = Logger.getLogger("chatServer");
    
    private final ChannelBuffer buf;
    
    public ChatProtoBufEncoder() {
        buf = ChannelBuffers.buffer(RESPOND_STRING.getBytes().length);
        buf.writeBytes(RESPOND_STRING.getBytes());
    }
    
    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
        logger.debug("ChatProtoBufEncoder encode start...");
    	if(!(msg instanceof Builder)) {
            if(msg instanceof String) {
                if(msg.equals(RESPOND_STRING)) {
                	logger.debug("ChatProtoBufEncoder encode end...send:"+RESPOND_STRING);
                    return buf;
                }
            }
            logger.error("someone want to send an unsupport msg type:" + msg.getClass().getName()+"\n"+msg);
            return ChannelBuffers.buffer(0);
        }
        
        Builder builder = (Builder)msg;
        Message proto = builder.build();
        
        //根据配置同步命令xml来查找对应的cmdType，但聊天系统没用到cmdType，同步房间机制用到了
        //int cmdId = CMDS_CONFIG.getRespCmdId(proto.getClass().getName());
        int cmdType = 30000;
        byte[] bytes = proto.toByteArray();
        
        //1个整型长度（4个字节），代表后面整个data所占字节长度，而具体的data包括 1个short（2个字节）的长度的cmd type和具体protobuf Byte数组
        ChannelBuffer buf = ChannelBuffers.buffer(frame_length_4b + protobuf_cmd_length_2b + bytes.length);
        buf.writeInt(protobuf_cmd_length_2b + bytes.length);
        buf.writeShort(cmdType);
        buf.writeBytes(bytes);
        
        logger.debug("------------------------------send msg start--------------------------------");
        logger.debug(proto.toString());
        logger.debug("------------------------------send msg end----------------------------------");
        
        logger.debug("ChatProtoBufEncoder encode end...");
        return buf;
    }

}
