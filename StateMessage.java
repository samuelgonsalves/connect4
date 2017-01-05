package connect4_main;

import java.io.Serializable;
import java.util.ArrayList;

public class StateMessage implements Serializable
{
	static final int dimensionX = 6,dimensionY = 7;
	int matrix[][] = new int[dimensionX][dimensionY];
	boolean winCondition = false;
	
	final int slotWidth = 35, slotHeight = 35;
	int winner = 0;
	int pointer = 0;
	int turn = 1;
	
	
	public StateMessage()
	{
		initMatrix();
	}
	
	public void initMatrix()
	{
		for(int i=0;i<dimensionX;i++)
		{
			for(int j=0;j<dimensionY;j++)
				matrix[i][j] = 0;
		}
	}
	
	public boolean checkWinCondition(int row, int col,int player)
	{
		int count = 0;
		boolean start = false;
		for(int i = 0;i<dimensionX; i++)
		{
			if(start == false)
			{
				if(matrix[row][i]==player)
				{
					start = true;
					count++;
				}
	
			}
			else
			{
				if(matrix[row][i]!=player)
				{
					start = false;
					count = 0;
				}
				else if(matrix[row][i]==player)
				{
					count++;
				}
			}
			if(count == 4)
			{
				return true;
			}
			
		}
		
		count = 0;
		start = false;
		for(int i = dimensionX - 1;i >= 0; i--)
		{
			if(start == false)
			{
				if(matrix[i][col]==player)
				{
					start = true;
					count++;
				}	
			}
			else
			{
				if(matrix[i][col]!=player)
				{
					start = false;
					count = 0;
				}
				else if(matrix[i][col]==player)
				{
					count++;
				}
			}
			if(count ==4)
			{
				return true;
			}
		}
	
		
		
		int k=0,l=0;
		for(k=row,l=col;k>0&&l>0;k--,l--)
		{
		}
		
		count = 0;
		start = false;
		for(int i = k,j = l;i<dimensionX&&j<dimensionY; i++,j++)
		{
				if(start == false)
				{
					if(matrix[i][j]==player)
					{
						start = true;
						count++;
					}	
				}
				else
				{
					if(matrix[i][j]!=player)
					{
						start = false;
						count = 0;
					}
					else if(matrix[i][j]==player)
					{
						count++;
					}
					if(count == 4)
					{
						return true;
					}
				}
			}
		
		k=0;
		l=0;
		for(k=row,l=col;k>0&&l<dimensionX-1;k--,l++)
		{
		}
		
		count = 0;
		start = false;
		for(int i = k, j = l;i<dimensionY-1&&j>=0; i++,j--)
		{
				if(start == false)
				{
					if(matrix[i][j]==player)
					{
						start = true;
						count++;
					}	
				}
				else
				{
					if(matrix[i][j]!=player)
					{
						start = false;
						count = 0;
					}
					else if(matrix[i][j]==player)
					{
						count++;
					}
				}
				if(count == 4)
				{
					return true;
				}
		}
		
		
			return false;
		
	}
	public boolean columnFullCondition(int column)
	{
		if(matrix[0][column]==0)
			return false;
		else
			return true;
	}
	public boolean checkMatrixFullCondition()
	{
		for(int i=0;i<dimensionX;i++)
		{
			for(int j=0;j<dimensionY;j++)
			{
				if(matrix[i][j]==0)
				{
					return false;
				}
			}
		}
		return true;
	}
	public int findLowestRow(int column)
	{
		for(int i=dimensionX - 1 ;i>=0;i--)
		{
			if(matrix[i][column]==0)
			{
				return i;
			}
		}
		return Integer.MAX_VALUE;
	}
	public void addCoin(int column,int player)
	{
		int row = findLowestRow(column);
		if(row!=Integer.MAX_VALUE)
		{
			matrix[row][column] = player;
			
			if(checkWinCondition(row, column, player))
			{
				winCondition = true;
				winner = player;
			}	
		}
		
		
	}
	
	public void newGame()
	{
		initMatrix();
		winCondition = false;
		winner = 0;
		pointer = 0;
		turn = 1;
	}
	
	public void switchTurn()
	{
		if(turn == 1)
		{
			turn = 2;
		}
		else if(turn == 2)
		{
			turn = 1;
		}
	}
	public int currentTurn()
	{
		return turn;
	}
	
	
}
