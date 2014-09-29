import com.teletalk.jserver.pool.PoolWorker;
import com.teletalk.jserver.pool.PoolWorkerFactory;


public class ALittlePoolWorkerFactory implements PoolWorkerFactory
{
   public PoolWorker createPoolWorker()
   {
      return new ALittlePoolWorker();
   }
}
