package connect4_main;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import processing.core.PApplet;


public class TestClient extends PApplet
{	
	public static final String SERVER_HOSTNAME = "localhost";
    public static final int SERVER_PORT = 12010;
    
	public static Handler handler;
	public static StateMessage stateMsg = null;
	
	public void settings()
	{
		size(500,500);
	}
	public void draw()
	{
		background(0);
		stateMsg = handler.getLatestStateMsg();
		
		if(stateMsg!=null)
		{
			int x=100,y=130;
			int tx= 100 + stateMsg.slotWidth/2,ty = 120;
			
			fill(255,255,255);
			text("Left and Right arrow keys to move",160,440);
			text("Enter to drop coin",200,460);
			triangle(tx+stateMsg.pointer*45,ty,tx-10+stateMsg.pointer*45,ty-10,tx+10 + stateMsg.pointer*45,ty-10);
			
			for(int i=0;i<stateMsg.dimensionX;i++)
			{
				for(int j=0;j<stateMsg.dimensionY;j++)
				{
					if(stateMsg.matrix[i][j]==0)
					{
						fill(255,255,255);
						rect(x,y,stateMsg.slotWidth,stateMsg.slotHeight);
					}
					else if(stateMsg.matrix[i][j]==1)
					{
						fill(255,0,0);
						rect(x,y,stateMsg.slotWidth,stateMsg.slotHeight);
					}
					else if(stateMsg.matrix[i][j]==2)
					{
						fill(0,0,255);
						rect(x,y,stateMsg.slotWidth,stateMsg.slotHeight);
					}
					
					x= x + 10 + stateMsg.slotWidth;
				}
				x=100;
				y=y+45;
			}
			if(stateMsg.winCondition)
			{
				fill(255,255,255);
				textSize(35);
				text("Player "+stateMsg.winner+" wins!", 130,70);
				textSize(10);
				text("Press N to start a new game",180,90);
			}
			else
			{
				fill(255,255,255);
				text("Player "+stateMsg.turn+" 's turn",200,20);
			}
		
		}
		else
		{
			System.out.println("null");
		}
		
	}

	
	
	public static void main(String args[])
	{
			ObjectInputStream in = null;
	        ObjectOutputStream out = null;
	        try 
	        {
	        	Socket socket = new Socket(SERVER_HOSTNAME, SERVER_PORT);
	        	out = new ObjectOutputStream(socket.getOutputStream());
	        	in = new ObjectInputStream(socket.getInputStream());
	        	System.out.println("Connected to server "+ SERVER_HOSTNAME + ":" + SERVER_PORT);
	        } 
	        catch (IOException ioe)
	        {
	           System.err.println("Can not establish connection to " +
	               SERVER_HOSTNAME + ":" + SERVER_PORT);
	           ioe.printStackTrace();
	           System.exit(-1);
	        }
	 
	        Sender sender = new Sender(out);
	           
	        
	        handler = new Handler(sender);
	        handler.start();
	
	        sender.setDaemon(true);
	        sender.start();
	 
	        
	        Receiver receiver = new Receiver(in,handler);
	        receiver.setDaemon(true);
	        receiver.start();
			
			PApplet.main("connect4_main.TestClient");
	}
	@Override
	public void keyPressed()
	{
		if(key==CODED)
		{
			switch(keyCode)
			{
				case LEFT:
						handler.sendOutboundMessage("left");
					break;
				case RIGHT:
						handler.sendOutboundMessage("right");
					break;
			}
		}
		else if(key==ENTER)
		{
			handler.sendOutboundMessage("enter");
		}
		else if(key=='n')
		{
			handler.sendOutboundMessage("n");
		}
	}
}

class Handler extends Thread
{
	int player;
	StateMessage stateMsg;
	ClientToServerMessage clientMessage = new ClientToServerMessage();
	
	Sender senderThread;
	
	ConcurrentHashMap<Integer,Object> hashmap = new ConcurrentHashMap<Integer, Object>();//incoming StateMessages
	
	public Handler(Sender senderThread)
	{
		this.senderThread=senderThread;
	}
	

	public synchronized StateMessage getLatestStateMsg()
	{	
		//Get Local State
		if (stateMsg!=null)
		{
			return stateMsg;
		}
		else
		{
			return null;
		}
	}

	
	//GameLoop updates
	public synchronized void sendOutboundMessage(String key)
	{
			clientMessage.keyPressInfo(player,key);
			senderThread.addObjectToQueue(clientMessage);		
	}
	
	
	public synchronized void newObjectReceived(Object anObject)
	{
		//Update Local State
		if(anObject instanceof StateMessage)
		{
			stateMsg = (StateMessage) anObject; 	
		}
		//Update Character if InitMessage
		else if(anObject instanceof InitMessage)
		{
			InitMessage temp = (InitMessage) anObject;
			player = temp.playerId;
			System.out.println("Player"+player);
		}
	}

}

class Sender extends Thread
{
	private ObjectOutputStream oos;
	Vector<Object> objectsToBeSent=new Vector<Object>();
	
	public Sender(ObjectOutputStream oos)
	{
	        this.oos=oos;
	}
 
	public void sendObjectToServer(Object object)
	{
		try
		{
			oos.writeObject(object);
			oos.reset();	
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	 public synchronized void addObjectToQueue(Object anObject) 
	    {
		 	if(anObject!=null)
		 	{
		 		objectsToBeSent.add(anObject);
		        notify();  
		 	}
	    } 
	 	
	private synchronized Object getNextObjectFromQueue() throws InterruptedException
	{
		while(objectsToBeSent.size()==0)
			wait();
		Object anObject = objectsToBeSent.get(0);
        objectsToBeSent.removeElementAt(0); 
        return anObject; 
	}
	
    public void run()
	{
    	 try 
         { 
             while (!isInterrupted()) 
             { 
                 Object anObject = getNextObjectFromQueue(); 
                 sendObjectToServer(anObject); 
             } 
         } 
         catch (Exception e) 
         { 
         } 
  	 }
}

class Receiver extends Thread
{
	public ObjectInputStream ois;
	Handler handler;
	Receiver(ObjectInputStream ois, Handler handler)
	{ 
		this.ois=ois;
		this.handler=handler;
	}
	@Override
	 public void run() 
    { 
        try 
        {
            while (!isInterrupted()) 
            { 
                try 
                {  
                	Object anObject = ois.readObject();
                	if(anObject == null)
                		break;
                	handler.newObjectReceived(anObject);
                } 
                catch (SocketTimeoutException | ClassNotFoundException ste)
                { 
                } 
            } 
        } 
        catch (IOException ioex) 
        { 
        } 
    }
}