import com.teletalk.jserver.pool.PoolWorker;


public class ALittlePoolWorker extends PoolWorker
{

   protected void work()
   {
      System.out.println("I am a little coffepot!");
      
   }

}
