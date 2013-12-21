
public class Transaction 
{
	//Keeps track of acknowledgement of Index Requests
	
	Index idx;
	long transTime;
	
	public Transaction(Index i, long ttime)
	{
		idx = i;
		transTime = ttime;
	}
	
	public Index GetIndex()
	{
		return idx;
	}
	
	public long GetTransactionTime()
	{
		return transTime;
	}

}
