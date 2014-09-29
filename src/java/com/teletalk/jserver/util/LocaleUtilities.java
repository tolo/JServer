/*
 * Copyright 2007 the project originators.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.teletalk.jserver.util;

import java.util.Locale;

/**
 * Utility class containing useful methods concerning the class <code>java.util.Locale</code>.
 *  
 * @author Tobias Löfstrand
 * 
 * @since 2.0 (20041223)
 */
public class LocaleUtilities
{
	/**
	 * Parses a Locale object from a string containing the language, country and variant separated by underbars (possibly previously generated 
	 * by the method Locale.toString()). Language is always lower case, and country is always upper case (example for swedish/sweden: sv_SE). 
	 * 
	 * @param localeString the string to parse a locale from.
	 * 
	 * @return the parsed Locale object or <code>null</code> if the string was null, had a length of 0 characters or had an invalid format.
	 */
	public static final Locale parseLocale(String localeString)
	{
		if(localeString != null)
		{
			String language;			String country;					if(localeString.length() == 0) return null;
					int pos = localeString.indexOf('_');
					if(pos == -1) //Only language
				return new Locale(localeString, "", "");        
			language = localeString.substring(0, pos);			localeString = localeString.substring(pos + 1);		
			pos = localeString.indexOf('_');		
			if (pos == -1) //Only language & country
			    return new Locale(language, localeString, "");

			country = localeString.substring(0, pos);
			localeString = localeString.substring(pos + 1); //Get variant

			return new Locale(language, country, localeString);
		}		else return null;
	}
}
