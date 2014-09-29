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
package com.teletalk.jserver.statistics;

import java.io.Serializable;

/**
 * Interface representing a statistics entry, i.e. a placeholder object for a statistics value.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0.2
 */
public interface StatisticsEntry extends Serializable
{
   /**
    * Gets the value of this StatisticsEntry.
    */
   public String getValue();
   
   /**
    * Resets the value of this StatisticsEntry.
    */
   public void reset();
}
