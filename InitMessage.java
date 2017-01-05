package connect4_main;

import java.io.Serializable;

public class InitMessage implements Serializable
{
	int playerId;
	
	public InitMessage(int playerId)
	{
		this.playerId = playerId;
	}
	
}
