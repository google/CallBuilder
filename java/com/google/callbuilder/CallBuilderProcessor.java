/*
 * Copyright (C) 2015 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.callbuilder;

import static javax.lang.model.element.Modifier.STATIC;

import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;

public class CallBuilderProcessor extends AbstractProcessor {
  @Override
  public ImmutableSet<String> getSupportedAnnotationTypes() {
    ImmutableSet.Builder<String> types = new ImmutableSet.Builder<>();
    types.add(CallBuilder.class.getName());
    // types.add(BuilderField.class.getName());
    return types.build();
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    boolean claimed = (annotations.size() == 1
        && annotations.iterator().next().getQualifiedName().toString().equals(
            CallBuilder.class.getName()));
    if (claimed) {
      process(roundEnv);
      return true;
    } else {
      return false;
    }
  }

  static String capitalizeFirst(String s) {
    return s.substring(0, 1).toUpperCase() + s.substring(1);
  }

  ImmutableList<String> simpleNames(Iterable<? extends Element> elements) {
    ImmutableList.Builder<String> names = new ImmutableList.Builder<>();
    for (Element el : elements) {
      names.add(el.getSimpleName().toString());
    }
    return names.build();
  }

  private static String lines(String... separated) {
    StringBuilder joined = new StringBuilder();
    for (String line : separated) {
      joined.append(line);
      joined.append('\n');
    }
    return joined.toString();
  }

  private static void writef(Writer writer, String format, Object... args) throws IOException {
    writer.write(String.format(format, (Object[]) args));
  }

  private Iterable<ExecutableElement> callbuilderElements(RoundEnvironment roundEnv) {
    Collection<? extends Element> unfiltered =
        roundEnv.getElementsAnnotatedWith(CallBuilder.class);
    return Iterables.concat(
        ElementFilter.constructorsIn(unfiltered),
        ElementFilter.methodsIn(unfiltered));
  }

  /**
   * Information about the context of the builder. This only applies to non-static, non-constructor
   * methods for which a builder is being generated. If an @{@link CallBuilder}-annotated method is
   * not static, then the generated builder must choose on which object to call the method. This is
   * passed as a constructor argument to the builder.
   */
  private static final class Context {
    private final TypeMirror type;
    private final String builderFieldName;

    Context(TypeMirror type, String builderFieldName) {
      this.type = Preconditions.checkNotNull(type);
      this.builderFieldName = Preconditions.checkNotNull(builderFieldName);
    }

    public TypeMirror getType() {
      return type;
    }

    public String getBuilderFieldName() {
      return builderFieldName;
    }
  }

  private static final class TypeParameters {
    private final ImmutableList<TypeParameterElement> classParameters;
    private final ImmutableList<TypeParameterElement> methodParameters;
    private final Optional<Context> context;
    private final boolean isConstructor;

    TypeParameters(Iterable<? extends TypeParameterElement> classParameters,
        Iterable<? extends TypeParameterElement> methodParameters,
        Optional<Context> context,
        boolean isConstructor) {
      this.classParameters = ImmutableList.copyOf(classParameters);
      this.methodParameters = ImmutableList.copyOf(methodParameters);
      this.context = context;
      this.isConstructor = isConstructor;
    }

    private Iterable<TypeParameterElement> allParameters() {
      if (context.isPresent() || isConstructor) {
        return Iterables.concat(classParameters, methodParameters);
      } else {
        return methodParameters;
      }
    }

    /**
     * The type parameters to place on the builder, without the "extends ..." bounds.
     */
    String alligator() {
      if (Iterables.isEmpty(allParameters())) {
        return "";
      }
      StringBuilder alligator = new StringBuilder("<");
      Joiner.on(", ").appendTo(alligator, allParameters());
      return alligator.append(">").toString();
    }

    /**
     * The type parameters to place on the builder, with the "extends ..." bounds.
     */
    String alligatorWithBounds() {
      if (Iterables.isEmpty(allParameters())) {
        return "";
      }

      StringBuilder alligator = new StringBuilder("<");
      String separator = "";
      for (TypeParameterElement param : allParameters()) {
        alligator.append(separator);
        separator = ", ";
        alligator.append(param.toString());
        for (TypeMirror bound : param.getBounds()) {
          alligator.append(" extends ").append(bound);
        }
      }
      return alligator.append(">").toString();
    }
  }

  private void process(RoundEnvironment roundEnv) {
    Elements elementUtils = processingEnv.getElementUtils();

    for (ExecutableElement el : callbuilderElements(roundEnv)) {
      boolean isConstructor = el.getSimpleName().toString().equals("<init>");
      TypeElement enclosingType = (TypeElement) el.getEnclosingElement();
      UniqueSymbols uniqueSymbols = new UniqueSymbols.Builder()
          .addAllUserDefined(simpleNames(el.getParameters()))
          .build();
      Optional<Context> context = Optional.absent();
      if (!isConstructor && !el.getModifiers().contains(STATIC)) {
        context = Optional.of(new Context(enclosingType.asType(), uniqueSymbols.get("")));
      }

      TypeParameters typeParameters = new TypeParameters(
          enclosingType.getTypeParameters(),
          el.getTypeParameters(),
          context,
          isConstructor);

      CallBuilder ann = el.getAnnotation(CallBuilder.class);
      String className;
      if (!ann.className().isEmpty()) {
        className = ann.className();
      } else if (isConstructor) {
        className = enclosingType.getSimpleName() + "Builder";
      } else {
        className = capitalizeFirst(el.getSimpleName().toString()) + "Builder";
      }
      String packageName = packageNameOf(el);
      String generatedCanonicalName =
          packageName.isEmpty() ? className : (packageName + "." + className);
      try {
        JavaFileObject file = processingEnv.getFiler().createSourceFile(generatedCanonicalName, el);
        try (Writer wrt = file.openWriter()) {
          if (!packageName.isEmpty()) {
            writef(wrt, lines(
                "package %s;",
                ""),
                packageName);
          }

          writef(wrt, lines(
              "public final class %s%s {"),
              className, typeParameters.alligatorWithBounds());

          for (Context justContext : context.asSet()) {
            String constructorParameterName = ann.contextName();
            writef(wrt, lines(
                "  private final %s %s;",
                "  public %s(%s %s) {",
                "    %s = %s;",
                "  }"),
                justContext.getType(), justContext.getBuilderFieldName(),
                className, justContext.getType(), constructorParameterName,
                justContext.getBuilderFieldName(), constructorParameterName);
          }

          ImmutableList<FieldInfo> fields = FieldInfo.fromAll(elementUtils, el.getParameters());

          for (FieldInfo field : fields) {
            if (field.style().isPresent()) {
              FieldStyle justStyle = field.style().get();
              for (TypeInference inference :
                  TypeInference.forField(justStyle, field.parameter()).asSet()) {
                writef(wrt, lines("  private %s %s = %s.start();"),
                    inference.builderFieldType(), field.name(),
                    qualifiedName(justStyle.styleClass()));
                for (ExecutableElement modifier : justStyle.modifiers()) {
                  ImmutableList<String> nonFieldParameterNames =
                      simpleNames(Iterables.skip(modifier.getParameters(), 1));
                  for (ImmutableList<String> nonFieldParameterTypes :
                       inference.modifierParameterTypes(modifier).asSet()) {
                    writef(wrt, lines(
                            "  public %s%s %s%s(%s) {",
                            "    this.%s = %s.%s(this.%s, %s);",
                            "    return this;",
                            "  }"),
                        className, typeParameters.alligator(),
                        modifier.getSimpleName(), capitalizeFirst(field.name()),
                        parameterList(nonFieldParameterTypes, nonFieldParameterNames),

                        field.name(),
                        qualifiedName(justStyle.styleClass()), modifier.getSimpleName(),
                        field.name(), Joiner.on(", ").join(nonFieldParameterNames));
                  }
                  // TODO: report warning if could not inference parameter types for some modifier.
                  // TODO: support generic type parameters on the *generated* modifier
                }
              }
              // TODO: report error if TypeInference could not be obtained.
            } else {
              writef(wrt, lines(
                  "  private %s %s;",
                  "  public %s%s set%s(%s %s) {",
                  "    this.%s = %s;",
                  "    return this;",
                  "  }"),
                  field.finishType(), field.name(),

                  className, typeParameters.alligator(),
                  capitalizeFirst(field.name()), field.finishType(), field.name(),

                  field.name(), field.name());
            }
          }

          TypeMirror generatedMethodReturn;
          if (isConstructor) {
            generatedMethodReturn = enclosingType.asType();
          } else {
            generatedMethodReturn = el.getReturnType();
          }

          // invocation: the return expression of the generated method, minus the argument list.
          String invocation;
          if (isConstructor) {
            invocation = String.format("new %s%s",
                enclosingType.getQualifiedName(),
                typeParameters.alligator());
          } else {
            invocation = String.format("%s.%s",
                context.isPresent()
                    ? context.get().getBuilderFieldName()
                    : enclosingType.getQualifiedName(),
                el.getSimpleName());
          }

          writef(wrt, lines(
              "  %s %s() {",
              "    %s%s(%s);",
              "  }",
              "}"),
              generatedMethodReturn, ann.methodName(),

              (generatedMethodReturn.getKind() == TypeKind.VOID) ? "" : "return ",
              invocation, finishInvocations(fields));
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Returns the name of the package that the given element is in. If the element is in the default
   * (unnamed) package then the name is the empty string. Take from AutoValue source.
   */
  static String packageNameOf(Element el) {
    while (true) {
      Element enclosing = el.getEnclosingElement();
      if (enclosing instanceof PackageElement) {
        return ((PackageElement) enclosing).getQualifiedName().toString();
      }
      el = (Element) enclosing;
    }
  }

  private static String parameterList(List<String> parameterTypes, List<String> parameterNames) {
    StringBuilder list = new StringBuilder();
    for (int i = 0; i < parameterTypes.size(); i++) {
      if (i != 0) {
        list.append(", ");
      }
      list.append(parameterTypes.get(i)).append(" ").append(parameterNames.get(i));
    }
    return list.toString();
  }

  static String qualifiedName(DeclaredType type) {
    return ((TypeElement) type.asElement()).getQualifiedName().toString();
  }

  private static String finishInvocations(Iterable<FieldInfo> fields) {
    StringBuilder invocations = new StringBuilder();
    for (FieldInfo field : fields) {
      if (invocations.length() != 0) {
        invocations.append(", ");
      }
      if (field.style().isPresent()) {
        invocations.append(String.format("%s.finish(%s)",
            qualifiedName(field.style().get().styleClass()), field.name()));
      } else {
        invocations.append(field.name());
      }
    }
    return invocations.toString();
  }
}
