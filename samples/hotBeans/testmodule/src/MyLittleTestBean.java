/**
 * 
 */
public class MyLittleTestBean implements TestBeanInterface
{
   private Object parent;

   
   public Object getParent()
   {
      return parent;
   }
   
   public void setParent(Object parent)
   {
      this.parent = parent;
   }
   
   
   public String formatMessage(final String message)
   {
      return "<MyLittleTestBean@" + System.identityHashCode(this) +  ">" + message + " - parent: " + this.parent;
   }
}
