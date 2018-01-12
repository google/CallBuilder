# CallBuilder
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
      String givenName,
      List<String> addressLines,
      @Nullable Integer age) {
    // ...
  }
}
```

Now you will have access to a `PersonBuilder` class in the same package!

```java
Person friend = new PersonBuilder()
    .setAddressLines(Arrays.asList("1123 Easy Street", "Townplace, XZ"))
    .setAge(22)
    .setGivenName("John")
    .setFamilyName("Doe")
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
      String givenName,
      @BuilderField(style = ArrayListAdding.class) ArrayList<String> addressLines,
      @Nullable Integer age) {
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
    .setGivenName("John")
    .setFamilyName("Doe")
    .build();
```
