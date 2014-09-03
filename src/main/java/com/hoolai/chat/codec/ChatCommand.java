package com.hoolai.chat.codec;

import com.google.protobuf.GeneratedMessage;

public class ChatCommand {
	
	private final GeneratedMessage proto;

	public ChatCommand(GeneratedMessage proto) {
		this.proto = proto;
	}

	public GeneratedMessage getProto() {
		return proto;
	}
	
}
