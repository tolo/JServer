/**
 */
public interface TestDao
{
   public TestUser getUserByName1(String name);
   
   public TestUser getUserByName2(String name);
   
   public TestUser getUserByNameBroken(String name);
}
