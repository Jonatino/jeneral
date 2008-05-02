package com.ochafik.lang.jeneral.examples;
import com.ochafik.lang.jeneral.annotations.Include;
import com.ochafik.lang.jeneral.annotations.Includes;
import com.ochafik.lang.jeneral.annotations.Property;
import com.ochafik.lang.jeneral.annotations.Template;
import com.ochafik.lang.jeneral.processors.MyUtilityClass;

@Includes({
    @Include(type = MyUtilityClass.class, keepConstructors = false),
    @Include(script = "scripts/test.vm", engine = "Velocity")
})
@Template
public abstract class TestGenerics<T> implements _TestGenerics<T> {
    @Property(construct = true)
    String name;
    
    @Property(construct = true)
    int age;
    
    @Property(construct = true)
    String address;
    
    static class Content {}

    @Include(script = "test/TestMethod.vm", engine = "Velocity", insertion = Include.Insertion.BEGINNING)
    void TestMethod() {
    	// Original method content
    	
    	testMethod();
    }
    
    public static void main(String[] args) {
        TestGenerics<Content> test = TestGenerics.template.newInstance(Content.class, "Mr. X", 25, "here");
        System.out.println(test.getAge());
        System.out.println(test.getAddress());
        System.out.println(test.getName());
    }
}