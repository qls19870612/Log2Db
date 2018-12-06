package utils;

import org.jboss.netty.buffer.ChannelBuffer;

import java.util.HashMap;

import game.collection.IntPair;
import game.collection.LeftIntPair;
import game.sink.util.ZkConfig.StrArrayValue;

public final class Empty {

    private Empty() {
    }

    public static final byte[] BYTE_ARRAY = new byte[0];

    public static final byte[][] BYTES_ARRAY = new byte[0][];

    public static final Object DUMB_OBJECT = new Object();

    public static final Object[] OBJECT_ARRAY = new Object[0];

    public static final int[] INT_ARRAY = new int[0];

    public static final boolean[] BOOL_ARRAY = new boolean[0];

    public static final String[] STRING_ARRAY = new String[0];

    public static final String STRING = "";

    public static final long[] LONG_ARRAY = new long[0];

    public static final Integer[] INTEGER_ARRAY = new Integer[0];

    public static final StrArrayValue STR_ARRAY_VALUE = new StrArrayValue(STRING_ARRAY);


    public static final IntPair[] INT_PAIR_ARRAY = new IntPair[0];

    public static final LeftIntPair<Object>[] LEFT_INT_PAIR_ARRAY = LeftIntPair.newArray(0);

    public static final ChannelBuffer[] BUFFER_ARRAY = new ChannelBuffer[0];

    public static final HashMap<String, String> KV_MAP = new HashMap<>();
}
