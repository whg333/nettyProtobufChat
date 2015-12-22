package com.whg.chat.net;

import static com.whg.chat.bo.ChatContext.frame_length_4b;
import static com.whg.chat.bo.ChatContext.max_frame_length_8kb;
import static org.jboss.netty.channel.Channels.pipeline;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;

import com.whg.chat.codec.ChatProtoBufDecoder;
import com.whg.chat.codec.ChatProtoBufEncoder;
import com.whg.chat.codec.LengthFieldBasedFrameDecoder;

public class ChatClientPipelineFactory implements ChannelPipelineFactory {

    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline p = pipeline();
        
//        p.addLast("frameDecoder", new ProtobufVarint32FrameDecoder());
//        p.addLast("protobufDecoder", new ProtobufDecoder(ChatProto.ChatMsgProto.getDefaultInstance()));
//
//        p.addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender());
//        p.addLast("protobufEncoder", new ProtobufEncoder());
        
        p.addLast("framer",  new LengthFieldBasedFrameDecoder(max_frame_length_8kb, 0, frame_length_4b, 0, frame_length_4b, true, true));
        p.addLast("decoder", new ChatProtoBufDecoder());
        p.addLast("encoder", new ChatProtoBufEncoder());

        p.addLast("handler", new ChatClientHandler());
        return p;
    }
}
