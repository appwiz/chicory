---
sidebar_position: 100
sidebar_label: Annotations
title: Annotations
---
## Host Modules

Instead of writing host functions by hand, you can write a class containing annotated methods
and let the Chicory annotation processor generate the host functions for you. This is especially
useful when you have many host functions.

<!--
```java
//DEPS com.dylibso.chicory:docs-lib:999-SNAPSHOT
//DEPS com.dylibso.chicory:runtime:999-SNAPSHOT
```
-->

```java
@HostModule("demo")
public final class Demo {
    
    public Demo() {};
    
    @WasmExport
    public long add(int a, int b) {
        return a + b;
    }

    @WasmExport // the Wasm name is random_get
    public void randomGet(Memory memory, int ptr, int len) {
        byte[] data = new byte[len];
        random.nextBytes(data);
        memory.write(ptr, data);
    }

    public HostFunction[] toHostFunctions() {
        return Demo_ModuleFactory.toHostFunctions(this);
    }
}
```

The `@HostModule` annotation marks the class as a host module and specifies the module name for
all the host functions. The `@WasmExport` annotation marks a method as host function and optionally
specifies the name of the function. If the name is not specified, then the Java method name is
converted from camel case to snake case, as is a common convention in Wasm.

The `Demo_ModuleFactory` class in `toHostFunctions()` is generated by the annotation processor.

Host functions must be instance methods of the class. Static methods are not supported.
This is because host functions will typically interact with instance state in the host class.

To use the host module, you need to instantiate the host module and fetch the host functions:

<!--
```java
import com.dylibso.chicory.runtime.HostFunction;

// bug in JShell: https://github.com/jbangdev/jbang/issues/1854
public class Demo {
    public Demo() {};

    public HostFunction[] toHostFunctions() {
        return new HostFunction[0];
    }
}
```
-->

```java
import com.dylibso.chicory.runtime.ImportValues;

var demo = new Demo();
var imports = ImportValues.builder().addFunction(demo.toHostFunctions()).build();
```

### Type conversions

The following conversions are supported:

| Java Type         | Wasm Type  |
|-------------------|------------|
| `int`             | `i32`      |
| `long`            | `i64`      |
| `float`           | `f32`      |
| `double`          | `f64`      |

## WasmModuleInterface

If you already have a Wasm module and want to generate scaffolded Java code to interact with it, you can use the `@WasmModuleInterface` annotation.

```java
@WasmModuleInterface("demo.wasm")
public final class Demo {}
```

The annotation accepts a single argument, which can be either:

- The location of the Wasm module on the current classpath (transitive references are not supported).
- An absolute URI pointing to the Wasm module, in the form of `file://....`

This annotation generates several things, depending on the provided module:

- `ModuleExports`: Represents the Wasm module's exported functions, mapped to typed Java parameters and return values.
- `ModuleImports`: Represents the module's imported host functions and includes a convenient `toImportValues()` method to obtain ImportValues after implementing the interfaces.

You can find examples of how to use the generated code in the host-module/it folder of the repository.

## Enabling the Annotation Processor

In order to use host modules, you need to import the relevant annotations, e.g. in Maven:

```xml
<dependency>
  <groupId>com.dylibso.chicory</groupId>
  <artifactId>annotations</artifactId>
  <version>latest-release</version>
</dependency>
```

and configure the Java compiler to include the Chicory `annotations-processor` as an annotation processor.
Exactly how this is done depends on the build system you are using, for instance, with Maven:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <annotationProcessorPaths>
      <path>
        <groupId>com.dylibso.chicory</groupId>
        <artifactId>annotations-processor</artifactId>
        <version>latest-release</version>
      </path>
    </annotationProcessorPaths>
  </configuration>
</plugin>
```

<!--
```java
docs.FileOps.writeResult("docs/usage", "annotations.md.result", "empty");
```
-->
