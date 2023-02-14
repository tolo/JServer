JServer Framework, version 2.2

http://www.jserver.org
-------------------------------------------------------------------------------


1. Introduction

JServer is a framework/SDK for developing component based standalone server 
side applications. The framework consists of an API for building and 
manipulating the component structure (tree) as well as a large library of 
useful packages, including for instance RPC/message based communication over 
a custom stream protocol, custom socket communication, basic HTTP support, 
object/thread/connection pooling, service discovery and registration, common 
administration interface (GUI and protocol), log management and property 
management.

See also the related (but standalone) project HotBeans 
(https://github.com/tolo/HotBeans), that provides support for hot 
deployable modules. 

JServer was initially created back in 1999 by John-John Roslund and Tobias 
Löfstrand while working at TeleTalk AB (later known as Link Messaging AB). 
In May 2007, the copyright was donated back from Link Messaging AB to the 
original creators, with the intent to publish the software as open source. 

The framework has been continuously developed and extended throughout the 
years, the latest additions being things like Spring and JBoss integration. 
Although some parts of JServer may be a bit dated and non-standard, the 
framework as a whole is still very useful in building distributed, robust 
and lightweight server side applications.

The plan for the future is to continue development and streamline the whole 
framework. This includes replacing and removing obsolete parts, improving the 
core functionality and increasing integration with other frameworks.

JServer is released under the terms of the Apache Software License, 
version 2.0 (see license.txt).


2. Distribution contents

The normal distribution (jserver-X.X.zip) contains the following:
* dist - contains the main jar file (jserver.jar), zipped source (jserver-X.X-src.zip) and the admin tool start script (jadmin.bat).
* docs - documentation and javadoc

The full distribution (jserver-X.X-full.zip) contains the following:
* dist - contains the main jar file (jserver.jar) and the admin tool start script (jadmin.bat).
* docs - documentation and javadoc.
* lib - third party libraries, required for building the project.
* res - images and sounds used by the administration tool.
* samples - some samples demonstration usage.
* src - the source code
* test - test source code and configuration files.


3. Third party library dependencies

The full ("-full") distribution of JServer contains all third party libraries that are 
required to build JServer. All third party libraries are subject to their respective 
licenses (see below). The dependencies are the following:

* HotBeans (https://github.com/tolo/HotBeans)
Required for building: Yes
Required at runtime: Optional
File(s): lib/hotbeans/hotbeans.jar
Version: 1.2
License: Apache License Version 2.0

* Servlet API (from the Apache Tomcat 6.0.13 distribution - http://tomcat.apache.org/)
Required for building: Yes
Required at runtime: Optional
File(s): lib/servlet/servlet-api.jar
Version: 2.5 (specification)
License: Apache License Version 2.0

* Jakarta Commons Codec (http://jakarta.apache.org/commons/codec/)
Required for building: Yes
Required at runtime: Optional (Required only when using the XML-RPC integration API). 
File(s): lib/jakarta-commons/commons-codec.jar
Version: 1.3
License: Apache License Version 2.0

* Jakarta Commons Logging (http://jakarta.apache.org/commons/logging/)
Required for building: Yes
Required at runtime: Yes
File(s): lib/jakarta-commons/commons-logging.jar
Version: 1.0.4
License: Apache License Version 2.0

* Jetty (http://www.mortbay.org)
Required for building: Yes
Required at runtime: Optional
File(s): lib/jetty/jetty-6.1.1.jar, lib/jetty/jetty-util-6.1.1.jar
Version: 2.0.1
License: Apache License Version 2.0

* JUnit (http://www.junit.org)
Required for building: Yes
Required at runtime: No
File(s): lib/junit/junit-4.3.1.jar
Version: 4.3.1
License: Common Public License Version 1.0 (lib/junit/cpl-v10.html)

* Log4J (http://logging.apache.org/log4j)
Required for building: Yes
Required at runtime: Yes
File(s): lib/log4j/log4j-1.2.14.jar
Version: 1.2.14
License: Apache License Version 2.0

* Spring Framework (http://www.springframework.org)
Required for building: Yes
Required at runtime: Optional  
File(s): lib/spring/spring.jar
Version: 1.2.9
License: Apache License Version 2.0

* XML-RPC (http://ws.apache.org/xmlrpc/)
Required for building: Yes
Required at runtime: Optional
File(s): lib/xmlrpc/xmlrpc-2.0.1.jar
Version: 2.0.1
License: Apache License Version 2.0


4. Where to begin?

Sample applications may be found in the "samples" directory and there is API 
documentation available in javadoc format in the "docs" directory. A 
configuration reference document ("JServer Configuration.html") is also available 
in the docs directory.

