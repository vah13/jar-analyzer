#Jar Analyzer
![](https://img.shields.io/badge/build-passing-brightgreen)
![](https://img.shields.io/badge/build-Java%208-orange)
![](https://img.shields.io/github/downloads/4ra1n/jar-analyzer/total)
![](https://img.shields.io/github/v/release/4ra1n/jar-analyzer)
![](https://img.shields.io/badge/Java%20Code%20Lines-4576-orange)

## introduce

A brief introduction: https://mp.weixin.qq.com/s/Rrx6x5M_28YRcQQCdxuEeQ

There is no English document, foreigners please translate by themselves

A GUI tool for analyzing `jar` packages, especially suitable for code security auditing. Multiple `jar` files can be analyzed at the same time, and the target method can be easily searched.
It supports decompiling bytecode and automatically constructs the relationship between classes and methods, helping Java security researchers work more efficiently.

**Note: Do not analyze too large or too many `Jar` packages, it is recommended that the maximum size should not exceed `300M`**

[Go to download](https://github.com/4ra1n/jar-analyzer/releases/latest)

The location of the method can be pinpointed (highlighted with a gray cursor on the left)

![](img/001.png)

Can directly locate strings (analyze constant pool related instructions to achieve precise positioning)

![](img/003.png)

Can directly analyze projects written by `Spring` framework

![](img/005.png)

Why not choose `IDEA` analysis: **Because `IDEA` does not support the analysis of Jar packages without source code**

Supports six search methods:
- Search directly by class and method name (search definition)
- Search by method call (where the method is called)
- Search string (analyze `LDC` instruction to find exact location)
- Regular search string (analyze the `LDC` instruction to find the exact location)
- Brainless search (analyze relevant instructions to find the precise location)
- Binary search (search directly from binary)

Three decompilation methods are supported:
- QuiltFlower (FernFlower variant, recommended way)
- Procyon
- CFR

Use class custom `JSyntaxPane` component (unofficial) to display `Java` code

(A lot of black technology is added on the basis of the library `https://code.google.com/archive/p/jsyntaxpane`)

## Expression search

Supports a super powerful expression search, which can be combined at will to search for the information you want

| expression | parameter | effect |
|:------------------|:-----------|:---------|
| nameContains | String | method name contains |
| startWith | String | method prefix |
| endWith | String | method suffix |
| classNameContains | String | class name contains |
| returnType | String | method return type |
| paramTypeMap | int String | Method parameter correspondence |
| paramsNum | int | Number of method parameters |
| isStatic | boolean | Whether the method is static |
| isSubClassOf | String | Who is the subclass of |
| isSuperClassOf | String | Who is the superclass of |
| hasAnno | String | method annotation |
| hasClassAnno | String | Annotation of the class |
| hasField | String | class field |

Notice:
- `returnType` and `paramTypeMap` require a complete class name, such as `java.lang.String`, and the basic type can be written directly, such as `int`
- `isSubClassOf` and `isSuperClassOf` require the full class name, e.g. `java.awt.Component`
- `hasAnno` and `hasClassAnno` do not require the full class name, just write it directly, such as `Controller`

![](img/007.png)

### 1. Basic search

The basis of search is method, what kind of method do you want to search

For example, I want to search for methods whose method name starts with `set` and ends with `value`

```java
#method
         .startWith("set")
         .endWith("value")
```

For example, I want to search for methods whose class name contains `Context` and whose method name contains `lookup`

```java
#method
         .nameContains("lookup")
         .classNameContains("Context")
```

For example, I want to search for a method that returns a total of 3 parameters of type `Process` and the second parameter is `String`

```java
#method
         .returnType("java. lang. Process")
         .paramsNum(3)
         .paramTypeMap(1,"java.lang.String")
```

### 2. Subclass and parent class

For example, we want to find all subclasses of `javax.naming.spi.ObjectFactory` (including subclasses of subclasses, etc.)

Just write the following rules, and the program will recursively find all parent classes

```java
#method
         .isSubClassOf("javax.naming.spi.ObjectFactory")
```

If you want to find all parent classes of a certain class, use `isSuperClassOf` (note the full class name)

Note that the above will directly find all methods of all eligible classes, so I suggest adding some filtering

For example

```java
#method
         .isSubClassOf("javax.naming.spi.ObjectFactory")
         .startWith("xxx")
         .paramsNum(0)
```

### 3. Annotation search

For example, we want to find all methods of all classes annotated with `@Controller`

Write the following rule

```java
#method
         .hasClassAnno("Controller")
```

For example, if you want to find all the methods annotated with `@RequestMapping`

```java
#method
         .hasAnno("RequestMapping")
```

Similarly, since all methods of all eligible classes are found, I suggest adding some filtering

### 4. Actual combat analysis

According to the `Swing RCE` conditions provided by the online master:
- must have a set method
- The set method must have only one parameter
- This parameter must be of type string
- the class must be a `Component` subclass (including indirect subclasses)

So we write a rule

```java
#method
         .startWith("set")
         .paramsNum(1)
         .paramTypeMap(0,"java.lang.String")
         .isSubClassOf("java.awt.Component")
```

search results

![](img/008.png)

## Quick Start

Important: Please use `Java 8+` to run (11 is recommended and `EXE` version with built-in `Java 11 JRE` is provided)

(A better font is used in `Java 11`, other versions use the default font)

(1) Step 1: Add `jar` file (support single `jar` file and `jar` directory)
- Click the button `Select Jar File` to open the jar file
- Support uploading multiple jar files and they will be analyzed together

Please take it easy, it will take a small amount of time to analyze the jar file

Note: Please wait until the progress bar is full and the analysis is complete

(2) Step 2: Enter the information you search for

We support input in three formats:
- `javax.naming.Context` (for example)
- `javax/naming/Context`
- `Context` (will search all `*.Context` classes)

Provides a quick way to enter

![](img/006.png)

Note: The common search content here can be customized and supplemented

Create a new `search.txt` file in the current directory, and separate the class name and method with `#` one by one, for example

```text
java.lang.Runtime#getRuntime
java.lang.String#equals
```

Binary search will only return existence, not specific information

![](img/004.png)

(3) The third step: you can double-click to decompile

The cursor will point to the exact location of the method call

During decompilation, the relationship between methods will be constructed

You can double-click anywhere on the panel to decompile and build a new method call relationship and display

Please note: if you encounter a situation where you cannot decompile, you need to load the correct jar file

For example, I can't decompile `javax.naming.Context` because I didn't include the `rt.jar` file, if you add it, you can decompile normally

You can use `Ctrl+F` to search code and edit

You can click on any option and the details of the method will be displayed next

You can right click to send options to the chain. You can think of a link as a favorite or record. In the chain, you can also double-click to decompile, and then display the new method call relationship, or display the details on a single machine
If an option in the chain is not what you want, you can right click to delete the option from the chain

So you can build a call chain that only belongs to you

All the method call relationships in `Who Called the Current Method` and `Who Called the Current Method` can also be double-clicked to decompile, click to view details, and right-click to join the link

You can view the current class bytecode with one click

![](img/002.png)

## about

(1) What is the relationship between methods

```java
class Test{
     void a(){
         new Test().b();
     }
    
     void b(){
         Test.c();
     }
    
     static void c(){
         //code
     }
}
```

If the current method is `b`

Who called the current method: `Test` class `a` method

Who is called by the current method: `Test` class `c` method

(2) How to solve the problem of interface implementation

```java
class Demo{
    void demo(){
        new Test().test();
    }
}

    interfaceTest {
        void test();
    }

class Test1Impl implements Test {
    @Override
    public void test() {
        //code
    }
}

class Test2Impl implements Test {
    @Override
    public void test() {
}
```
//code
}
}
```

Now we have `Demo.demo -> Test.test` data, but actually it is `Demo.demo -> TestImpl.test`.

So we added new rules: `Test.test -> Test1Impl.test` and `Test.test -> Test2Impl.test`.

First make sure that the data will not be lost, then we can manually analyze the decompiled code by ourselves
- `Demo.demo -> Test.test`
- `Test.test -> Test1Impl.test`/`Test.test -> Test2Impl.test`

(3) How to solve the inheritance relationship

```java
class Zoo{
     void run(){
         Animal dog = new Dog();
         dog. eat();
     }
}

class Animal {
     void eat() {
         //code
     }
}

class Dog extends Animal {
     @Override
     void eat() {
         //code
     }
}

class Cat extends Animal {
     @Override
     void eat() {
         //code
     }
}
```
The bytecode of `Zoo.run -> dog.cat` is `INVOKEVIRTUAL Animal.eat ()V`, but we only have this rule `Zoo.run -> Animal.eat`, missing `Zoo.run -> Dog.eat` rules

In this case we added new rules: `Animal.eat -> Dog.eat` and `Animal.eat -> Cat.eat`

First make sure that the data will not be lost, then we can manually analyze the decompiled code by ourselves
- `Zoo.run -> Animal.eat`
- `Animal.eat -> Dog.eat` / `Animal.eat -> Cat.eat`