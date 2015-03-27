#CallBuilder
Make a builder by defining one function.  
(_beta_: be ready for breaking API changes)

*CallBuilder* is a Java code generator that finally makes it easy to make a
builder class. Builders are great when you have a constructor or method with
many parameters. They are even helpful when you have two or more arguments of
the same type, since the order is easy to mix up.

Builders are usually hard to write. You will probably need around 4 lines for
every field, and if it's an inner class, it makes the file long and cumbersome
to navigate. This discourages many people from writing builders, and those
people give up and learn to live with brittle, and hard-to-read code. If you
want to add multiple setters for a field (common for lists that may have add and
addAll), your builder will quickly become a chore to write and a burden to
maintain.

CallBuilder changes that. To use it, you can simply write the method or
constructor as you normally would, but add the `@CallBuilder` annotation:

```java
public class Person {
  @CallBuilder
  Person(
      String familyName,
      String firstName,
      ImmutableList<String> addressLines,
      Optional<Integer> age) {
    // ...
  }
}
```

Now you will have access to a `PersonBuilder` class in the same package!

```java
Person friend = new PersonBuilder()
    .setAddressLines(ImmutableList.of("1123 Easy Street", "Townplace, XZ"))
    .setAge(Optional.of(22))
    .setFirstName("John")
    .setLastName("Doe")
    .build();
```

With the field styles feature, you can define custom behavior for the fields
that are of a certain type. This can make the API more natural and look more
like a manually-written builder:

```java
public class Person {
  @CallBuilder
  Person(
      String familyName,
      String firstName,
      @BuilderField(style = ImmutableListAdding.class) ImmutableList<String> addressLines,
      @BuilderField(style = OptionalSetting.class) Optional<Integer> age) {
    // ...
  }
}
```

This can be used like:

```java
Person friend = new PersonBuilder()
    .addToAddressLines("1123 Easy Street")
    .addToAddressLines("Townplace, XZ")
    .setAge(22)
    .setFirstName("John")
    .setLastName("Doe")
    .build();
```

#How to use
For the time being, CallBuilder requires you to use the
[Bazel](http://bazel.io/) build system for your project.

Copy the files in the CallBuilder repository to your workspace. Then, in your
`java_library` or `java_binary` target, simply add a dependency on CallBuilder
in the `deps` attribute:

```python
java_library(
    name = "person",
    srcs = ["Person.java"],
    deps = ["//java/com/google/callbuilder"],
)
```
