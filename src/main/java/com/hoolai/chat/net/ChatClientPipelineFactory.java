package com.hoolai.chat.net;

import static org.jboss.netty.channel.Channels.pipeline;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;

import com.hoolai.chat.codec.ChatProtoBufDecoder;
import com.hoolai.chat.codec.ChatProtoBufEncoder;
import com.hoolai.chat.codec.LengthFieldBasedFrameDecoder;

public class ChatClientPipelineFactory implements ChannelPipelineFactory {

    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline p = pipeline();
        
//        p.addLast("frameDecoder", new ProtobufVarint32FrameDecoder());
//        p.addLast("protobufDecoder", new ProtobufDecoder(ChatProto.ChatMsgProto.getDefaultInstance()));
//
//        p.addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender());
//        p.addLast("protobufEncoder", new ProtobufEncoder());
        
        p.addLast("framer",  new LengthFieldBasedFrameDecoder(81920, 0, 4, 0, 4, true, true));
        p.addLast("decoder", new ChatProtoBufDecoder());
        p.addLast("encoder", new ChatProtoBufEncoder());

        p.addLast("handler", new ChatClientHandler());
        return p;
    }
}
