import com.teletalk.jserver.rmi.adapter.*;

import java.rmi.*;

public final class TestSystemRmiAdapter extends SubSystemRmiAdapter implements RemoteTestSystem
{
	public TestSystemRmiAdapter(TestSystem testSystem) throws RemoteException
	{
		super(testSystem);
	}
	
	public String getTestMessage()
	{
		return ((TestSystem)adaptee).getTestMessage();
	}
}
