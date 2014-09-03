package com.hoolai.chat.codec;

import static com.hoolai.chat.net.ChatServerHandler.REQUEST_STRING;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.frame.CorruptedFrameException;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;

/**
 * <b>功能描述：</b>跟netty的该类功能类似，不过会对是特定的String还是真正的报文包做一个区分<br>
 * <b>项目名称：</b>synroom<br>
 * <b>修改记录：</b><br>
 * @author yizhuan
 * @date 2012-2-1下午06:28:40<br>
 * TODO
 */
public class LengthFieldBasedFrameDecoder extends FrameDecoder {

    private final int maxFrameLength;

    private final int lengthFieldOffset;

    private final int lengthFieldLength;

    private final int lengthFieldEndOffset;

    private final int lengthAdjustment;

    private final int initialBytesToStrip;

    private boolean discardingTooLongFrame;

    private long tooLongFrameLength;

    private long bytesToDiscard;

    private boolean skipPolicy = false;

    private boolean skipTencentGTWHead = false;

    private static final byte[] POLICY_STR_BYTES = REQUEST_STRING.getBytes();

    private static final byte[] CRLF_BYTES = "\r\n\r\n".getBytes();

    private static final byte[] GET_BYTES = "GET".getBytes();

    /**
     * Creates a new instance.
     *
     * @param maxFrameLength
     *        the maximum length of the frame.  If the length of the frame is
     *        greater than this value, {@link TooLongFrameException} will be
     *        thrown.
     * @param lengthFieldOffset
     *        the offset of the length field
     * @param lengthFieldLength
     *        the length of the length field
     *
     */
    public LengthFieldBasedFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength) {
        this(maxFrameLength, lengthFieldOffset, lengthFieldLength, 0, 0, false, false);
    }

    /**
     * Creates a new instance.
     *
     * @param maxFrameLength
     *        the maximum length of the frame.  If the length of the frame is
     *        greater than this value, {@link TooLongFrameException} will be
     *        thrown.
     * @param lengthFieldOffset
     *        the offset of the length field
     * @param lengthFieldLength
     *        the length of the length field
     * @param lengthAdjustment
     *        the compensation value to add to the value of the length field
     * @param initialBytesToStrip
     *        the number of first bytes to strip out from the decoded frame
     */
    public LengthFieldBasedFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
            int lengthAdjustment, int initialBytesToStrip, boolean skipPolicy, boolean skipTencentGTWHead) {
        this.skipPolicy = skipPolicy;
        this.skipTencentGTWHead = skipTencentGTWHead;
        if (maxFrameLength <= 0) {
            throw new IllegalArgumentException("maxFrameLength must be a positive integer: " + maxFrameLength);
        }

        if (lengthFieldOffset < 0) {
            throw new IllegalArgumentException("lengthFieldOffset must be a non-negative integer: " + lengthFieldOffset);
        }

        if (initialBytesToStrip < 0) {
            throw new IllegalArgumentException("initialBytesToStrip must be a non-negative integer: "
                    + initialBytesToStrip);
        }

        if (lengthFieldLength != 1 && lengthFieldLength != 2 && lengthFieldLength != 3 && lengthFieldLength != 4
                && lengthFieldLength != 8) {
            throw new IllegalArgumentException("lengthFieldLength must be either 1, 2, 3, 4, or 8: "
                    + lengthFieldLength);
        }

        if (lengthFieldOffset > maxFrameLength - lengthFieldLength) {
            throw new IllegalArgumentException("maxFrameLength (" + maxFrameLength + ") "
                    + "must be equal to or greater than " + "lengthFieldOffset (" + lengthFieldOffset + ") + "
                    + "lengthFieldLength (" + lengthFieldLength + ").");
        }

        this.maxFrameLength = maxFrameLength;
        this.lengthFieldOffset = lengthFieldOffset;
        this.lengthFieldLength = lengthFieldLength;
        this.lengthAdjustment = lengthAdjustment;
        lengthFieldEndOffset = lengthFieldOffset + lengthFieldLength;
        this.initialBytesToStrip = initialBytesToStrip;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
        Integer hadGetPackage = (Integer) ctx.getAttachment();
        //只判断前100个包，前100个包存在前端Flash建立Socket连接过程中对Socket安全策略的跨域请求，截取<policy-file-request/>返回即可
        //且由于搭建在滕旭服务器上，前100个包也存在腾讯的GTW头验证机制（以GET开头且以\r\n\r\n结束），GTW这个不需要进行截取，跳过即可
        if (hadGetPackage == null || hadGetPackage <= 100) {
            if (hadGetPackage == null) {
                ctx.setAttachment(new Integer(1));
            } else {
                ctx.setAttachment(hadGetPackage + 1);
            }
            if (skipPolicy || skipTencentGTWHead) {
                if (skipPolicy) {
                    int readableBytes = buffer.readableBytes();
                    if (readableBytes >= lengthFieldEndOffset) {
                        if (readableBytes >= POLICY_STR_BYTES.length) {
                            byte[] bytes = new byte[POLICY_STR_BYTES.length];
                            buffer.getBytes(buffer.readerIndex(), bytes);
                            boolean isEqual = checkIsEqual(bytes, POLICY_STR_BYTES);
                            if (isEqual) {
                                buffer.skipBytes(POLICY_STR_BYTES.length);
                                return new String(bytes);
                            }
                        } else {
                            byte[] bytes = new byte[readableBytes];
                            buffer.getBytes(buffer.readerIndex(), bytes);
                            boolean isEqual = checkIsEqual(bytes, POLICY_STR_BYTES);
                            if (isEqual) {
                                return null;
                            }
                        }
                    }
                } 
                if (skipTencentGTWHead) {
                    int readableBytes = buffer.readableBytes();
                    if (readableBytes >= lengthFieldEndOffset) {
                        if (readableBytes >= GET_BYTES.length) {
                            byte[] bytes = new byte[readableBytes];
                            buffer.getBytes(buffer.readerIndex(), bytes);
                            boolean isBeginWith = isBeginWith(bytes, GET_BYTES);
                            if (isBeginWith) {
                                int endIndex = findEndIndex(bytes, CRLF_BYTES);
                                if (endIndex == -1) {
                                    return null;
                                } else {
                                    buffer.skipBytes(endIndex);
                                }
                            } else {
                                //do nothing
                            }
                        } else {
                            byte[] bytes = new byte[readableBytes];
                            buffer.getBytes(buffer.readerIndex(), bytes);
                            boolean isEqual = checkIsEqual(bytes, GET_BYTES);
                            if (isEqual) {
                                return null;
                            }
                        }
                    }
                }
            }
        }

        if (discardingTooLongFrame) {
            long bytesToDiscard = this.bytesToDiscard;
            int localBytesToDiscard = (int) Math.min(bytesToDiscard, buffer.readableBytes());
            buffer.skipBytes(localBytesToDiscard);
            bytesToDiscard -= localBytesToDiscard;
            this.bytesToDiscard = bytesToDiscard;
            if (bytesToDiscard == 0) {
                // Reset to the initial state and tell the handlers that
                // the frame was too large.
                // TODO Let user choose when the exception should be raised - early or late?
                //      If early, fail() should be called when discardingTooLongFrame is set to true.
                long tooLongFrameLength = this.tooLongFrameLength;
                this.tooLongFrameLength = 0;
                fail(ctx, tooLongFrameLength);
            } else {
                // Keep discarding.
            }
            return null;
        }

        if (buffer.readableBytes() < lengthFieldEndOffset) {
            return null;
        }

        int actualLengthFieldOffset = buffer.readerIndex() + lengthFieldOffset;
        long frameLength;
        switch (lengthFieldLength) {
        case 1:
            frameLength = buffer.getUnsignedByte(actualLengthFieldOffset);
            break;
        case 2:
            frameLength = buffer.getUnsignedShort(actualLengthFieldOffset);
            break;
        case 3:
            frameLength = buffer.getUnsignedMedium(actualLengthFieldOffset);
            break;
        case 4:
            frameLength = buffer.getUnsignedInt(actualLengthFieldOffset);
            break;
        case 8:
            frameLength = buffer.getLong(actualLengthFieldOffset);
            break;
        default:
            throw new Error("should not reach here");
        }

        if (frameLength < 0) {
            buffer.skipBytes(lengthFieldEndOffset);
            throw new CorruptedFrameException("negative pre-adjustment length field: " + frameLength);
        }

        frameLength += lengthAdjustment + lengthFieldEndOffset;
        if (frameLength < lengthFieldEndOffset) {
            buffer.skipBytes(lengthFieldEndOffset);
            throw new CorruptedFrameException("Adjusted frame length (" + frameLength + ") is less "
                    + "than lengthFieldEndOffset: " + lengthFieldEndOffset);
        }

        if (frameLength > maxFrameLength) {
            // Enter the discard mode and discard everything received so far.
            discardingTooLongFrame = true;
            tooLongFrameLength = frameLength;
            bytesToDiscard = frameLength - buffer.readableBytes();
            buffer.skipBytes(buffer.readableBytes());
            return null;
        }

        // never overflows because it's less than maxFrameLength
        int frameLengthInt = (int) frameLength;
        if (buffer.readableBytes() < frameLengthInt) {
            return null;
        }

        if (initialBytesToStrip > frameLengthInt) {
            buffer.skipBytes(frameLengthInt);
            throw new CorruptedFrameException("Adjusted frame length (" + frameLength + ") is less "
                    + "than initialBytesToStrip: " + initialBytesToStrip);
        }
        buffer.skipBytes(initialBytesToStrip);

        // extract frame
        int readerIndex = buffer.readerIndex();
        int actualFrameLength = frameLengthInt - initialBytesToStrip;
        ChannelBuffer frame = extractFrame(buffer, readerIndex, actualFrameLength);
        buffer.readerIndex(readerIndex + actualFrameLength);

        return frame;
    }

    private int findEndIndex(byte[] bytes, byte[] crlfBytes) {
        int countEnd = bytes.length - crlfBytes.length;
        for (int i = 0; i <= countEnd; i++) {
            if (checkInclude(bytes, crlfBytes, i)) {
                return i + crlfBytes.length;
            }
        }
        return -1;
    }

    private boolean checkInclude(byte[] bytes, byte[] crlfBytes, int i) {
        for (int j = 0; j < crlfBytes.length; j++) {
            if (crlfBytes[j] != bytes[i + j]) {
                return false;
            }
        }
        return true;
    }

    private boolean isBeginWith(byte[] bytes1, byte[] bytes2) {
        if (bytes1.length < bytes2.length) {
            return false;
        }
        for (int i = 0; i < bytes2.length; i++) {
            if (bytes1[i] != bytes2[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean checkIsEqual(byte[] bytes, byte[] bytes2) {
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] != bytes2[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Extract the sub-region of the specified buffer. This method is called by
     * {@link #decode(ChannelHandlerContext, Channel, ChannelBuffer)} for each
     * frame.  The default implementation returns a copy of the sub-region.
     * For example, you could override this method to use an alternative
     * {@link ChannelBufferFactory}.
     * <p>
     * If you are sure that the frame and its content are not accessed after
     * the current {@link #decode(ChannelHandlerContext, Channel, ChannelBuffer)}
     * call returns, you can even avoid memory copy by returning the sliced
     * sub-region (i.e. <tt>return buffer.slice(index, length)</tt>).
     * It's often useful when you convert the extracted frame into an object.
     * Refer to the source code of {@link ObjectDecoder} to see how this method
     * is overridden to avoid memory copy.
     */
    protected ChannelBuffer extractFrame(ChannelBuffer buffer, int index, int length) {
        ChannelBuffer frame = buffer.factory().getBuffer(length);
        frame.writeBytes(buffer, index, length);
        return frame;
    }

    private void fail(ChannelHandlerContext ctx, long frameLength) {
        if (frameLength > 0) {
            Channels.fireExceptionCaught(ctx.getChannel(), new TooLongFrameException("Adjusted frame length exceeds "
                    + maxFrameLength + ": " + frameLength + " - discarded"));
        } else {
            Channels.fireExceptionCaught(ctx.getChannel(), new TooLongFrameException("Adjusted frame length exceeds "
                    + maxFrameLength + " - discarding"));
        }
    }
}
