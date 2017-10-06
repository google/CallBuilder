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

import com.google.callbuilder.util.Preconditions;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;

public class CallBuilderProcessor extends AbstractProcessor {
  @Override
  public Set<String> getSupportedAnnotationTypes() {
    Set<String> types = new HashSet<>();
    types.add(CallBuilder.class.getName());
    return types;
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

  List<String> simpleNames(Iterable<? extends Element> elements) {
    List<String> names = new ArrayList<>();
    for (Element el : elements) {
      names.add(el.getSimpleName().toString());
    }
    return names;
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

  private List<ExecutableElement> callbuilderElements(RoundEnvironment roundEnv) {
    Collection<? extends Element> unfiltered =
        roundEnv.getElementsAnnotatedWith(CallBuilder.class);
    List<ExecutableElement> elements = new ArrayList<>();
    elements.addAll(ElementFilter.constructorsIn(unfiltered));
    elements.addAll(ElementFilter.methodsIn(unfiltered));
    return elements;
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

  private static StringBuilder joinOn(
      StringBuilder builder, String delimiter, Iterable<?> elements) {
    int added = 0;
    for (Object element : elements) {
      if (added++ > 0) {
        builder.append(delimiter);
      }

      builder.append(element.toString());
    }

    return builder;
  }

  private static final class TypeParameters {
    private final List<TypeParameterElement> classParameters;
    private final List<TypeParameterElement> methodParameters;
    private final @Nullable Context context;
    private final boolean isConstructor;

    TypeParameters(List<? extends TypeParameterElement> classParameters,
        List<? extends TypeParameterElement> methodParameters,
        @Nullable Context context,
        boolean isConstructor) {
      this.classParameters = Collections.unmodifiableList(new ArrayList<>(classParameters));
      this.methodParameters = Collections.unmodifiableList(new ArrayList<>(methodParameters));
      this.context = context;
      this.isConstructor = isConstructor;
    }

    private List<TypeParameterElement> allParameters() {
      List<TypeParameterElement> allParameters = new ArrayList<>();
      if ((context != null) || isConstructor) {
        allParameters.addAll(classParameters);
      }
      allParameters.addAll(methodParameters);
      return allParameters;
    }

    /**
     * The type parameters to place on the builder, without the "extends ..." bounds.
     */
    String alligator() {
      List<TypeParameterElement> allParameters = allParameters();
      if (allParameters.isEmpty()) {
        return "";
      }
      StringBuilder alligator = new StringBuilder("<");
      joinOn(alligator, ", ", allParameters);
      return alligator.append(">").toString();
    }

    /**
     * The type parameters to place on the builder, with the "extends ..." bounds.
     */
    String alligatorWithBounds() {
      List<TypeParameterElement> allParameters = allParameters();
      if (allParameters.isEmpty()) {
        return "";
      }

      StringBuilder alligator = new StringBuilder("<");
      String separator = "";
      for (TypeParameterElement param : allParameters) {
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
      Context context = null;
      if (!isConstructor && !el.getModifiers().contains(STATIC)) {
        context = new Context(enclosingType.asType(), uniqueSymbols.get(""));
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
              "@javax.annotation.Generated(\"%s\")"),
              CallBuilderProcessor.class.getName());
          writef(wrt, lines(
              "public final class %s%s {"),
              className, typeParameters.alligatorWithBounds());

          if (context != null) {
            String constructorParameterName = ann.contextName();
            writef(wrt, lines(
                "  private final %s %s;",
                "  public %s(%s %s) {",
                "    %s = %s;",
                "  }"),
                context.getType(), context.getBuilderFieldName(),
                className, context.getType(), constructorParameterName,
                context.getBuilderFieldName(), constructorParameterName);
          }

          List<FieldInfo> fields = FieldInfo.fromAll(elementUtils, el.getParameters());

          for (FieldInfo field : fields) {
            if (field.style() != null) {
              FieldStyle fieldStyle = field.style();
              TypeInference inference = TypeInference.forField(fieldStyle, field.parameter());
              if (inference != null) {
                writef(wrt, lines("  private %s %s = %s.start();"),
                    inference.builderFieldType(), field.name(),
                    qualifiedName(fieldStyle.styleClass()));
                for (ExecutableElement modifier : fieldStyle.modifiers()) {
                  List<? extends VariableElement> parameters = modifier.getParameters();
                  List<String> nonFieldParameterNames =
                      simpleNames(parameters.subList(1, parameters.size()));
                  List<String> nonFieldParameterTypes =
                      inference.modifierParameterTypes(modifier);
                  if (nonFieldParameterTypes != null) {
                    writef(wrt, lines(
                            "  public %s%s %s%s(%s) {",
                            "    this.%s = %s.%s(this.%s, %s);",
                            "    return this;",
                            "  }"),
                        className, typeParameters.alligator(),
                        modifier.getSimpleName(), capitalizeFirst(field.name()),
                        parameterList(nonFieldParameterTypes, nonFieldParameterNames),

                        field.name(),
                        qualifiedName(fieldStyle.styleClass()), modifier.getSimpleName(),
                        field.name(), joinOn(new StringBuilder(), ", ", nonFieldParameterNames));
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
                (context != null)
                    ? context.getBuilderFieldName()
                    : enclosingType.getQualifiedName(),
                el.getSimpleName());
          }

          writef(wrt, lines(
              "  public %s %s() {",
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
   * (unnamed) package then the name is the empty string. Taken from AutoValue source code.
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
      if (field.style() != null) {
        invocations.append(String.format("%s.finish(%s)",
            qualifiedName(field.style().styleClass()), field.name()));
      } else {
        invocations.append(field.name());
      }
    }
    return invocations.toString();
  }
}
