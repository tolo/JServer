import com.teletalk.jserver.rmi.remote.*;

import java.rmi.*;

public interface RemoteTestSystem extends RemoteSubSystem
{
	public String getTestMessage() throws RemoteException;
}
