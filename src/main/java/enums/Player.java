package enums;

public class Player {
	public enum State {
		AWAITING_CLIENT_PACKET,
		AWAITING_SERVER_PACKET,
		PLAYING,
		DISCONNECTED
	}
}
