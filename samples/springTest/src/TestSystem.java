import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.pool.ThreadPool;


/**
 * 
 */
public class TestSystem extends SubSystem
{
   private TestComponent testComponent1;
   
   private TestComponent testComponent2;
   
   private ThreadPool testPool;
   
   
   public TestSystem()
   {
      super("TestSystem");
   }
   
   public TestComponent getTestComponent1()
   {
      return testComponent1;
   }

   public void setTestComponent1(TestComponent testComponent1)
   {
      this.testComponent1 = testComponent1;
   }

   public TestComponent getTestComponent2()
   {
      return testComponent2;
   }

   public void setTestComponent2(TestComponent testComponent2)
   {
      this.testComponent2 = testComponent2;
   }
   
   public ThreadPool getTestPool()
   {
      return testPool;
   }

   public void setTestPool(ThreadPool testPool)
   {
      this.testPool = testPool;
   }
   
   public void run()
   {
      while(super.canRun)
      {
         try
         {
            Thread.sleep(1000000000L);
         }
         catch (Exception e) 
         {
            e.printStackTrace();
         }
      }
   }
}
