import com.teletalk.jserver.*;
import com.teletalk.jserver.log.*;
import com.teletalk.jserver.property.*;
import com.teletalk.jserver.tcp.*;

public class PropertySystem extends SubSystem
{
	private NumberProperty numberP;
	private StringProperty stringP;
	private BooleanProperty boolP;
   private MultiStringProperty multiString;
	private EnumProperty enumP;
	private String[] enums = {"Kahuna!", "Yeah baby yeah!", "Groovy!"};
	private TcpEndPointIdentifierProperty addresses;
	
	public PropertySystem(SubSystem parent)
	{
		super(parent, "PropertySystem");
		
		numberP = new NumberProperty(this, "number", 0, Property.MODIFIABLE_NO_RESTART);
		numberP.setDescription("A little number property!");
		
		stringP = new StringProperty(this, "string", "Hejsan", Property.MODIFIABLE_NO_RESTART);
		stringP.setDescription("A little string property!");
		
		boolP = new BooleanProperty(this, "boolean", true, Property.MODIFIABLE_NO_RESTART);
		boolP.setDescription("A little boolean property!");
      
      multiString = new MultiStringProperty(this, "multiString", new String[]{"str1", "str2"}, Property.MODIFIABLE_NO_RESTART);
      multiString.setDescription("A little multi string property!");
		
		enumP = new EnumProperty(this, "enum", 0, enums, Property.MODIFIABLE_NO_RESTART);
		enumP.setDescription("A little enum property!");
		
		addresses = new TcpEndPointIdentifierProperty(this, "addresses", "", Property.MODIFIABLE_NO_RESTART);
		addresses.setDescription("Some addresses.");
		
		addProperty(numberP);
		addProperty(stringP);
		addProperty(boolP);
      addProperty(multiString);
		addProperty(enumP);
		addProperty(addresses);
	}
	
	protected void doInitialize()
	{
		System.out.println("Nu kör vi!");
		super.doInitialize();
	}
	
	protected void doShutDown()
	{
		System.out.println("Nu stoppar vi!");
		super.doShutDown();
	}
	
	public void propertyModified(Property property)
	{
		if(property == numberP)
		{
			System.out.println("numberP changed value to " + numberP.getValueAsString());
		}
		else if(property == stringP)
		{
			System.out.println("stringP changed value to " + stringP.getValueAsString());
		}
		else if(property == boolP)
		{
			System.out.println("boolP changed value to " + boolP.getValueAsString());
		}
      else if(property == multiString)
      {
         System.out.println("multiString changed value to " + multiString.getValueAsString());
      }
		else if(property == enumP)
		{
			System.out.println("enumP changed value to " + enumP.getValueAsString());
		}
      else if(property == addresses)
      {
         System.out.println("addresses changed value to " + addresses.getValueAsString());
      }
		
		super.propertyModified(property);
	}
	
	public void run()
	{
		while(canRun)
		{
			System.out.println("");
			System.out.println("* Property values: *");
			System.out.println("* numberP: " + numberP.getValueAsString());
			System.out.println("* stringP: " + stringP.getValueAsString());
			System.out.println("* boolP: " + boolP.getValueAsString());
         System.out.println("* multiString: " + multiString.getValueAsString());
			System.out.println("* enumP: " + enumP.getValueAsString());
			System.out.println("* addresses: " + addresses.getValueAsString());
			System.out.println("");
			
			try
			{
				this.getThread().sleep(30*1000);
			}
			catch(InterruptedException e)
			{
				if(!canRun) break;
			}
		}
	}
}
