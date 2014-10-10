package com.hoolai.chat.codec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hoolai.chat.bo.ChatMsg;
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
        	//解析cmdType，用0xffff强制转换成short类型，聊天系统没用到cmdType，但同步房间机制用到了
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
    
    public static void main(String[] args) throws InvalidProtocolBufferException {
    	//String byteStr = "00000024753008FFFFFFFFFFFFFFFFFF0110FFFFFFFFFFFFFFFFFF0122042D3230304A0470696E67";
    	//String byteStr = "00000023753008FFFFFFFFFFFFFFFFFF01100022042D3230304A0C6865617274626561742E2E2E";
    	String byteStr = "00000023753008ffffffffffffffffff01100022042d3230304a0c6865617274626561742e2e2e";
    	char[] byteStrArray = byteStr.toCharArray();
    	List<Byte> l = new ArrayList<Byte>();
    	System.out.println(byteStrArray.length);
        for(int i=0;i<byteStrArray.length;i+=2){
        	int by = Integer.parseInt(byteStr.substring(i, i+2), 16) & 0xff; //两两一组十六进制的字节表示
        	System.out.print(by);
        	l.add((byte)by);
        }
        System.out.println();
        Byte[] bb = l.toArray(new Byte[0]);
        System.out.println(Arrays.toString(bb));
        System.out.println(bb.length);
    	ChannelBuffer buf = ChannelBuffers.buffer(bb.length);
        
//    	byte[] bytes = ChatMsg.pingProto().toByteArray();
//    	ChannelBuffer buf = ChannelBuffers.buffer(frame_length_4b + protobuf_cmd_length_2b + bytes.length);
    	
//    	int len = protobuf_cmd_length_2b + bytes.length;
//    	short cmdType = 30000;
//    	buf.writeInt(len);
//      buf.writeShort(cmdType);
//      buf.writeBytes(bytes);
    	buf.writeBytes(toByte(bb));	//直接写入捕获的protobuf字节码消息(包括了4字节长度和2字节命令号)，和上面注释掉的效果是一样的
        
        System.out.println(Arrays.toString(buf.array()));
        System.out.println(buf.array().length);
        for(byte b:buf.array()){
//        	System.out.print(Integer.toHexString(b & 0xff));
        	System.out.print(b & 0xff);
        }
        System.out.println();
        
        buf.readInt();
        buf.readShort();
    	GeneratedMessage proto = (GeneratedMessage) ChatMsgProto.getDefaultInstance().newBuilderForType()
        	.mergeFrom(buf.array(), buf.arrayOffset() + buf.readerIndex(), buf.readableBytes()).build();
    	
    	ChatCommand cmd = new ChatCommand(proto);
    	ChatMsgProto msgProto = (ChatMsgProto) cmd.getProto();
    	ChatMsg msg = ChatMsg.wrap(msgProto);
    	System.out.println(msg);
	}
    
    public static byte[] toByte(Byte[] bytes){
    	byte[] result = new byte[bytes.length];
    	for(int i=0;i<bytes.length;i++){
    		result[i] = bytes[i];
    	}
    	return result;
    }

}
