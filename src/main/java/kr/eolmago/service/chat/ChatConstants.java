package kr.eolmago.service.chat;

public final class ChatConstants {
	private ChatConstants() {}

	// paging
	public static final int MESSAGE_PAGE_SIZE = 30;

	// redis stream
	public static final String STREAM_KEY = "chat:messages";
	public static final String GROUP = "chat-group";

	public static final String FIELD_ROOM_ID = "roomId";
	public static final String FIELD_SENDER_ID = "senderId";
	public static final String FIELD_CONTENT = "content";

	// redis stream group init (XGROUP CREATE ... $ MKSTREAM)
	public static final String CMD_XGROUP = "XGROUP";
	public static final String XGROUP_CREATE = "CREATE";
	public static final String XGROUP_START_ID = "$";
	public static final String XGROUP_MKSTREAM = "MKSTREAM";

	// ws topic
	public static final String TOPIC_ROOM_PREFIX = "/topic/chat.rooms.";

	// consumer tuning
	public static final long POLL_FIXED_DELAY_MS = 200L;
	public static final int POLL_COUNT = 50;
	public static final long POLL_BLOCK_SECONDS = 1L;
	public static final String DEFAULT_CONSUMER_NAME = "c1";
}
