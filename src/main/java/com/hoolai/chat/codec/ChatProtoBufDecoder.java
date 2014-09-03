package com.hoolai.chat.codec;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;

import com.google.protobuf.GeneratedMessage;
import com.hoolai.chat.proto.ChatProto.ChatMsgProto;

/**
 * 先将ChannelBuffer解码成ProtoBuff，再构造一个基于业务的Command
 */
public class ChatProtoBufDecoder extends OneToOneDecoder {

    private static Logger logger = Logger.getLogger("chatServer");

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
    	logger.debug("ChatProtoBufDecoder decode start...");
    	if (!(msg instanceof ChannelBuffer)) {
    		logger.debug("ChatProtoBufDecoder decode end...receive:"+msg);
            return msg;
        }
    	
        ChannelBuffer buf = (ChannelBuffer) msg;
        if (buf.hasArray()) {
        	//解析cmdType，用0xffff强制转换成short类型，但聊天系统没用到cmdType，但同步房间机制用到了
            int cmdType = buf.readShort() & 0xffff;
            
            GeneratedMessage proto = (GeneratedMessage) ChatMsgProto.getDefaultInstance().newBuilderForType()
                    .mergeFrom(buf.array(), buf.arrayOffset() + buf.readerIndex(), buf.readableBytes()).build();
            ChatCommand command = new ChatCommand(proto);

        	logger.debug("------------------------------receive msg start--------------------------------");
            logger.debug(command.getProto().toString());
            logger.debug("------------------------------receive msg end----------------------------------");
            
            logger.debug("ChatProtoBufDecoder decode end...");
            return command;
        } else {
            logger.warn("receive msg meet a null buf");
        }
        
        logger.debug("ChatProtoBufDecoder decode end...return null");
        return null;
    }

}
