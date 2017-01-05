package connect4_main;

import java.io.Serializable;

public class ClientToServerMessage implements Serializable
{
	int player;
	String keyPress;
	
	ClientToServerMessage()
	{
		keyPress = null;
	}
	
	public void keyPressInfo(int player,String key)
	{
		this.player = player;
		keyPress = key;
	}
}
