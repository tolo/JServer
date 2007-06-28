import java.lang.reflect.Method;

import com.teletalk.jserver.util.exception.AOPInvocationExceptionHandler;
import com.teletalk.jserver.util.exception.ErrorCodeException;

/**
 * Translates exceptions.
 */
public class MyInvocationExceptionTranslator extends AOPInvocationExceptionHandler
{
   public Throwable translate(Object target, Method method, Object[] args, Throwable exception)
   {
      return new ErrorCodeException(12345, "A LITTLE EXCEPTION, BABY", exception);
   }
}
