AUTHOR Jalolliddin Yusupov [@yujboss](https://github.com/yujboss)

##### astrid-config is a fork of the VEREFOO project (VErified REFinement and Optimized Orchestration)   designed to provide an automatic way to allocate packet filters – the most common and traditional firewall technology – in a Service Graph defined by the service designer and an auto-configuration technique to create firewall rules with respect to the specified security requirements.




The astrid-config  involves the formulation of
a MaxSMT problem, whose objective is to maximize the
sum of the weights assigned to the satisfied soft clauses,
with respect to hard constraints that always require to
be satisfied. Its targets are on one side the allocation of
the minimum number of NSFs instances to reduce the
resource consumption due to the allocation of the corresponding virtual functions, on the other side the reduction of the rules describing their configuration to improve
the efficiency of the filtering operations. The MaxSMT
problem is formulated so as to provide also a formal verification that the achieved solution is formally correct. 
This document describes the installation details and the initial set of interfaces required for its interaction with other modules of ASTRID. 

## Installation and deployment

### Z3 library support
[Download](https://github.com/Z3Prover/z3/releases) the correct version of Z3 according to your OS and your JVM endianness. For the correct functioning of the application, you must have the Z3 native library and include it to Java Library Path. The most convenient way to do this is add the path that the library to the dynamic linking library path. 

* In Linux is `LD_LIBRARY_PATH`
* In MacOS is `DYLD_LIBRARY_PATH`
* In Windows is `PATH`

> e.g., 
> * `sudo nano /etc/environment` 
> * `LD_LIBRARY_PATH = $LD_LIBRARY_PATH:/home/verefoo/z3/bin/`
> * `Z3 = /home/verefoo/z3/bin/` (also required)

> Troubleshooting
>  
> If z3 throws  ``java.lang.UnsatisfiedLinkError: no libz3java in java.library.path``  exception, add all *.so files of z3 release into ``/usr/lib`` folder.

### Installing astrid-config via Maven (Spring Boot application with Embedded Tomcat)  [Solution 1] 
* install [jdk1.8.X YY](http://www.oracle.comntechnetwork/java/javase/downloads/jdk8-downloads-2133151.html);
* install [maven](https://maven.apache.org/install.html) 
* `mvn clean package`
* `java -jar target/verifoo-0.0.1-SNAPSHOT.jar`

Swagger documentation can be accessed at [localhost:8085/verefoo](localhost:8085/verefoo).

### Installing astrid-config via Ant (Apache Tomcat required) [Solution 2] 
-  install [jdk1.8.X YY](http://www.oracle.comntechnetwork/java/javase/downloads/jdk8-downloads-2133151.html);
-  install [Apache Tomcat 8](https://tomcat.apache.org/download-80.cgi);
	-  set CATALINA HOME ambient variable to the directory where you  installed Apache;
	-  (optional) configure Tomcat Manager:
	-  open the file ``%CATALINA_HOME%\conf\tomcat-users.xml``
	-  under the ``tomcat-users`` tag place, initialize an user with roles  "tomcat, manager-gui, manager-script".  An example is the following  content:
   ``xml   <role rolename="manager-gui"/>  <role rolename="manager-script"/>  <role rolename="admin-gui"/>   <role rolename="admin-script"/>  <user username="admin" password="admin" roles="manager-gui,manager-script,admin-scripts"/>``
	-  edit the "to\_be\_defined" fields in tomcat-build.xml with the before
   defined credentials;
-  execute the `generate` ant task in order to generate the .war;
-  launch Tomcat 8 with the startup script  ``%CATALINA_HOME%\bin\startup.bat`` or by the start-tomcat task ant;
-  (optional) if you previously configured Tomcat Manager you can open a  browser and navigate to `this link <http://localhost:8080/manager>`  and login using the proper username and password (e.g.,  ``admin/admin`` in the previous example);
-  (optional) you can `deploy/undeploy/redeploy` the downloaded WARs   through the web interface.

## API Resources Design


| Resources  | URLs        | XML repr            | Meaning                                             |
|------------|-------------|---------------------|-----------------------------------------------------|
| ROOT       | /           | Hyperlinks          | XML file with the hyperlinks to the other resources |
| deployment | /deployment | NFV                 | XML file with integrated deployment information     |
| dc         | /dc         | InfrastructureInfo  | XML file describing infrastructure information      |
| podevent   | /podevent   | InfrastructureEvent | XML file describing changes in the NFV              |
