import com.teletalk.jserver.SubSystem;

public class TestSystem extends SubSystem
{
   private TestBeanInterface testInterface; 
   
   public TestSystem(SubSystem parent, String name)
   {
      super(parent, name);
      // TODO Auto-generated constructor stub
   }

   public TestSystem(String name)
   {
      super(name);
      // TODO Auto-generated constructor stub
   }
   
   public TestSystem()
   {
      super("TestSystem");
      // TODO Auto-generated constructor stub
   }
   
   public TestBeanInterface getTestInterface()
   {
      return testInterface;
   }
   
   public void setTestInterface(TestBeanInterface testInterface)
   {
      this.testInterface = testInterface;
   }
   
   public void run()
   {
      for(int i=0; super.canRun; i++)
      {
         try
         {
            System.out.print(i + " - ");
            System.out.println(testInterface.formatMessage("Hello"));
         }
         catch(Exception e)
         {
            System.out.println("Error invoking TestBeanInterface.formatMessage - " + e);
         }
         
         System.out.println(" --- ");
         try{
         Thread.sleep(2500);
         }catch(InterruptedException ie){}
      }
   }
}
