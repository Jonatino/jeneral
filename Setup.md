# NetBeans #
  * TODO

# Eclipse #
  * Download jeneral.jar.
  * Right click on your project and open its properties
  * Go to Java Compiler / Annotation Processing and check all the boxes ("Enable project specific settings", "Enable annotation processing" and "Enable processing in editor")
  * Go to Factory Path (under Java Compiler / Annotation Processing) and add jeneral.jar as an external JAR.
  * Click on OK, that's it !
  * Now go to the [QuickStart](QuickStart.md) section.

More info on this configuration step is available in eclipse documentation :  [Configure Apt in Eclipse](http://help.eclipse.org/help33/index.jsp?topic=/org.eclipse.jdt.doc.isv/guide/jdt_apt_getting_started.htm)

# Bare JDK (5+) #
  * Download jeneral.jar.
  * Use apt instead of javac to compile your code :
```
	javac -cp myDir1:lib1.jar... path/myClass.java
```
> becomes :
```
	apt -factorypath jeneral.jar -cp jeneral.jar:myDir1:lib1.jar... path/myClass.java
```
  * Now go to the [QuickStart](QuickStart.md) section.