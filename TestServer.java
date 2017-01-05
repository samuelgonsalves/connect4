package connect4_main;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;


public class TestServer 
{
	public static final int LISTENING_PORT = 12010; 
    
    private static ServerSocket mServerSocket; 
 
    private static ClientHandler mClientHandler; 
    
    
    public static void main(String[] args)
	{
			bindServerSocket(); 
			
	        mClientHandler = new ClientHandler(); 
	        
	        mClientHandler.start();
	        
	        
	        
	        new Thread(new AcceptConnections(mServerSocket, mClientHandler)).start();
		
	}
	private static void bindServerSocket() 
	{ 
	        try 
	        { 
	            mServerSocket = new ServerSocket(LISTENING_PORT); 
	            System.out.println("Server started on " + "port " + LISTENING_PORT); 
	        }
	        catch (IOException ioe) 
	        { 
	            System.err.println("Can not start listening on " + "port " + LISTENING_PORT); 
	            ioe.printStackTrace(); 
	            System.exit(-1); 
	        } 
    } 
}

class AcceptConnections extends Thread
{
	private static ServerSocket mServerSocket; 
    private static ClientHandler mClientHandler;
  
    AcceptConnections(ServerSocket ss,ClientHandler sd)
    {
    	mServerSocket=ss;
    	mClientHandler=sd;
    }
    
    
    
	@Override
	public void run()
	{
		Initializer init = new Initializer(mClientHandler);
		init.start();
		while (true) 
	        { 
	            try 
	            { 
	            	//Accept connections, start ClientSender and ClientListener threads
	                if(Client.clientCount!=2)
	                {
	                	Socket socket = mServerSocket.accept(); 
		                Client client = new Client(); 
		                client.mSocket = socket; 
		                ClientListener clientListener = new ClientListener(client, mClientHandler); 
		                ClientSender clientSender = new ClientSender(client, mClientHandler); 
		                client.mClientListener = clientListener; 
		                clientListener.start(); 
		                client.mClientSender = clientSender; 
		                clientSender.start(); 
		                mClientHandler.addClient(client);
		                
		                System.out.println(Client.clientCount);
		                InitMessage initMsg = new InitMessage(Client.clientCount);
		                client.mClientSender.sendObject(initMsg);
		                init.addClientToQueue(client); 
	
	                }
	            		               
	               
	            } 
	            catch (IOException ioe)
	            { 
	                ioe.printStackTrace(); 
	            } 
	        } 
	}    
}

class Initializer extends Thread
{
	private static ClientHandler mClientHandler;
  
	Vector <Client> clientQueue= new Vector<Client>(); 
	
	
	public Initializer(ClientHandler clientHandler)
	{
		mClientHandler=clientHandler;
	}
	
	public synchronized void addClientToQueue(Client client)
	{
		clientQueue.addElement(client);
		notify();
	}
	
	private synchronized Client getNextClientFromQueue() throws InterruptedException
	{ 
 		while (clientQueue.size()==0) 
 			wait(); 
	    Client clientFromQueue = clientQueue.get(0);
	    clientQueue.removeElementAt(0); 
	    return clientFromQueue; 
	} 
	 
	
	public void run()
	{
		StateMessage state = new StateMessage();
		mClientHandler.createStateForClientHandler(state);
		
	    while(true)
	    {
	    	state.initMatrix();
			try
			{
				Client client = getNextClientFromQueue();
		    	mClientHandler.broadCastMessageToAllClients(client, state);			
			
			}
			catch (InterruptedException e)
			{
			}
	    }
    }
}

class ClientHandler extends Thread
{ 
	
    private static Vector<Client> mClients = new Vector<Client>(); 
    
    private static ConcurrentHashMap<Client,Character> clientToCharacterHashMap = new ConcurrentHashMap<Client,Character>();
    
    private static ConcurrentHashMap<Client,Object> hashmap = new ConcurrentHashMap<Client,Object>();//Incoming Objects
    
    
    static StateMessage stateMsg;
       
    
    public synchronized void addClient(Client aClient) 
    { 	
            mClients.add(aClient);	
    } 
 
    public synchronized void deleteClient(Client aClient)
    { 
        int clientIndex = mClients.indexOf(aClient); 
        if (clientIndex != -1) 
            mClients.removeElementAt(clientIndex); 
    } 
 
   
    //Add newly connected Client character to the hashmap
    
	public void addCharacterToHashmap(Client client, Character character)
	{
		clientToCharacterHashMap.put(client, character);
	}
	
	
	//Pass statemessage from acceptconnections to clienthandler
	public void createStateForClientHandler(StateMessage state)
	{
		ClientHandler.stateMsg = state;
	}
	
	
	
	//Key press logic, event raised
	public void handleClientMessage(Client client,ClientToServerMessage clientMessage)
	{
		if(clientMessage.player==stateMsg.currentTurn())
		{
			switch(clientMessage.keyPress)
			{
				case "left":
					{
						if(stateMsg.pointer>0)
						{
							stateMsg.pointer--;
						}
					}
				break;
				
				case "right":
					{
						if(stateMsg.pointer < stateMsg.dimensionY - 1)
						{
							stateMsg.pointer++;
						}
					}
				break;
				
				case "enter":
					{
						if(!stateMsg.checkMatrixFullCondition()&&!stateMsg.columnFullCondition(stateMsg.pointer))
						{
							stateMsg.addCoin(stateMsg.pointer,stateMsg.currentTurn());
							stateMsg.switchTurn();
						}
					}
				break;
		
				case "n":
					{
						if(stateMsg.winCondition)
							stateMsg.newGame();
					}
				break;
			}
	
		}
	}
	
    public synchronized void dispatchObject(Client aClient, Object anObject) 
    { 
        hashmap.put(aClient, anObject);
        notify(); 
    } 
 
    private synchronized Object getNextObjectFromQueue() throws InterruptedException 
    {
    	Object temp=null;
    	while(hashmap.isEmpty())
    		wait();
    	
    	for(int i = 0;i < mClients.size();i++)
    	{
    		Client client= (Client) mClients.get(i);
    		if(client!=null)
    		{
    			temp = (Object)hashmap.get(client);
    			if(temp!=null)
    			{
        			hashmap.remove(client);
        			return temp;
    			}
    		}
    	}
    	return null;
    } 
 
    private void sendObjectToAClient(Client clientSender,Object anObject)
    {
    	clientSender.mClientSender.sendObject(anObject);
    }
    
    public void broadCastMessageToAllClients(Client clientSender, Object anObject)
    {   	
    	for(Client client:mClients)
    	{
    			sendObjectToAClient(client,anObject);
    	}
    }
    
    public void run() 
    { 
        try 
        { 
            while(true) 
            { 
            	for (int i=0; i < mClients.size(); i++) 
            	{
            	     Client client = (Client) mClients.get(i);
            	     if(hashmap.get(client)!=null)
            	     {
            	    	 Object anObject = getNextObjectFromQueue();
            	    	 
            	    	 if(anObject!=null)
            	    	 {
            	    		 if(anObject instanceof ClientToServerMessage)
            	    		 {
            	    			 handleClientMessage(client,(ClientToServerMessage) anObject);
                	    		 broadCastMessageToAllClients(client, stateMsg); 
            	    		 }
            	    	 }            	    		 
            	    }
            	}
            } 
        }
        catch (InterruptedException ie) 
        { 
        } 
    } 
} 


class Client 
{ 
    public static int clientCount = 0;
    public Socket mSocket = null; 
    public ClientListener mClientListener = null; 
    public ClientSender mClientSender = null; 
    Client()
    {
    	clientCount++;
    }
} 
 

class ClientListener extends Thread 
{ 
    private ClientHandler mClientHandler; 
    private Client mClient; 
    private ObjectInputStream ois;
    
    public ClientListener(Client aClient, ClientHandler aClientHandler) throws IOException 
    { 
        mClient = aClient; 
        mClientHandler = aClientHandler; 
        Socket socket = aClient.mSocket; 
        ois = new ObjectInputStream(socket.getInputStream());  
    } 
 

    public void run() 
    { 
        try 
        { 
            while (!isInterrupted()) 
            { 
                try 
                {  
                    Object anObject = ois.readObject();
                    if (anObject == null) 
                        break; 
                    mClientHandler.dispatchObject(mClient, anObject); 
                } 
                catch (SocketTimeoutException | ClassNotFoundException ste)
                { 
                } 
            } 
        } 
        catch (IOException ioex) 
        { 
        } 
 
        mClient.mClientSender.interrupt(); 
        mClientHandler.deleteClient(mClient); 
    } 
} 
 
class ClientSender extends Thread 
{ 
    private Vector<Object> mMessageQueue = new Vector<Object>(); 
   
    private ClientHandler mClientHandler; 
    private Client mClient; 
    private ObjectOutputStream oos;
    
    public ClientSender(Client aClient, ClientHandler aClientHandler) throws IOException
    { 
        mClient = aClient; 
        mClientHandler = aClientHandler; 
        Socket socket = aClient.mSocket; 
        oos = new ObjectOutputStream(socket.getOutputStream());
    } 

    public synchronized void sendObject(Object anObject) 
    { 
        mMessageQueue.add(anObject);
        notify(); 
    } 
 

    private synchronized Object getNextObjectFromQueue() throws InterruptedException
    { 
        while (mMessageQueue.size()==0) 
            wait(); 
        Object objectFromQueue = mMessageQueue.get(0);
        mMessageQueue.removeElementAt(0); 
        return objectFromQueue; 
    } 
 
    
    private void sendObjectToClient(Object anObject)
    { 
    	try
		{	
			oos.writeObject(anObject);
			oos.reset();
		} 
    	catch (IOException e)
		{
			e.printStackTrace();
		}
    	
    } 
 
    public void run() 
    { 
        try 
        { 
            while (!isInterrupted()) 
            { 
                Object anObject = getNextObjectFromQueue(); 
                sendObjectToClient(anObject); 
            } 
        } 
        catch (Exception e) 
        { 
        } 
        mClient.mClientListener.interrupt(); 
        mClientHandler.deleteClient(mClient); 
    } 
}
