import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import javax.sql.DataSource;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.object.MappingSqlQuery;

import com.teletalk.jserver.event.EventQueue;


/**
 */
public class JdbcTestDao extends JdbcDaoSupport implements TestDao
{
   private static final String SELECT_BY_NAME_SQL = "select * from users where name=?";
   
   abstract class AbstractSelect extends MappingSqlQuery 
   {
		public AbstractSelect(DataSource dataSource, String sql) 
		{
			super(dataSource, sql);
		}

		protected Object mapRow(ResultSet rs, int rowNum) throws SQLException 
		{
			TestUser user = new TestUser();

			user.setId(rs.getInt("USER_ID"));
			user.setName(rs.getString("NAME"));
			
			return user;
		}
	}
	class SelectByName extends AbstractSelect {

		public SelectByName(DataSource dataSource) 
		{
			super(dataSource, SELECT_BY_NAME_SQL);
			declareParameter(new SqlParameter(Types.VARCHAR));
		}
	}
   
   class MyRowMapper implements RowMapper
   {
      public Object mapRow(ResultSet rs, int rowNum) throws SQLException
      {
         TestUser user = new TestUser();

         user.setId(rs.getInt("USER_ID"));
         user.setName(rs.getString("NAME"));
         
         return user;
      }
   }
	
	private SelectByName selectByName;
   
   private EventQueue eventQueue;
   
   private String testValue;
   
   
	public EventQueue getEventQueue()
   {
      return eventQueue;
   }

   public void setEventQueue(EventQueue eventQueue)
   {
      System.out.println("Setting event queue");      
      this.eventQueue = eventQueue;
   }
   
   public String getTestValue()
   {
      return testValue;
   }

   public void setTestValue(String testValue)
   {
      System.out.println("setTestValue: " + testValue);
      this.testValue = testValue;
   }
   
   

   protected void initDao() throws Exception 
	{
		super.initDao();
		selectByName = new SelectByName(getDataSource());
	}
   
   /**
    * getUserByName1
    */
   public TestUser getUserByName1(String name)
   {
      System.out.println("getUserByName1 - attempting to find user " + name);
      return (TestUser)this.selectByName.findObject(name);
   }
   
   /**
    * getUserByName2
    */
   public TestUser getUserByName2(String name)
   {
      System.out.println("getUserByName2 - attempting to find user " + name);
      return (TestUser)super.getJdbcTemplate().queryForObject(SELECT_BY_NAME_SQL, new String[]{name}, new MyRowMapper());
      //return (TestUser)this.selectByName.findObject(name);
   }
   
   /**
    * getUserByNameBroken
    */
   public TestUser getUserByNameBroken(String name)
   {
      System.out.println("getUserByNameBroken - attempting to find user " + name);
      super.getJdbcTemplate().queryForObject("select sdfasdfasdf from SDFSDAF", new String[]{name}, new MyRowMapper());
      return (TestUser)this.selectByName.findObject(name);
   }
}
