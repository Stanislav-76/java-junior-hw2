package ru.gb.lesson2.hw;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TestProcessor {

  /**
   * Данный метод находит все void методы без аргументов в классе, и запускеет их.
   * <p>
   * Для запуска создается тестовый объект с помощью конструткора без аргументов.
   */
  public static void runTest(Class<?> testClass) {
    final Constructor<?> declaredConstructor;
    try {
      declaredConstructor = testClass.getDeclaredConstructor();
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("Для класса \"" + testClass.getName() + "\" не найден конструктор без аргументов");
    }

    final Object testObj;
    try {
      testObj = declaredConstructor.newInstance();
    } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
      throw new RuntimeException("Не удалось создать объект класса \"" + testClass.getName() + "\"");
    }

    List<Method> methods = new ArrayList<>();
    Method beforeEach = null;
    Method afterEach = null;
    for (Method method : testClass.getDeclaredMethods()) {
      if (method.isAnnotationPresent(BeforeEach.class)) {
        checkTestMethod(method);
        beforeEach = method;
      }
      if (method.isAnnotationPresent(Test.class) && !method.isAnnotationPresent(Skip.class)) {
        checkTestMethod(method);
        methods.add(method);
      }
      if (method.isAnnotationPresent(AfterEach.class)) {
        checkTestMethod(method);
        afterEach = method;
      }
    }

    methods.sort((it1, it2) -> it1.getDeclaredAnnotation(Test.class).order() - it2.getDeclaredAnnotation(Test.class).order());

    if (beforeEach != null){
      for (Method method: methods) {
        try {
          beforeEach.invoke(testObj);
          runTest(method, testObj);
        } catch (IllegalAccessException | InvocationTargetException e) {
          throw new RuntimeException("Не удалось запустить тестовый метод \"" + beforeEach.getName() + "\"");
        }
      }
    } else {
      methods.forEach(it -> runTest(it, testObj));
    }
    if (afterEach != null) {
      try {
        afterEach.invoke(testObj);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException("Не удалось запустить тестовый метод \"" + afterEach.getName() + "\"");
      }
    }

  }

  private static void checkTestMethod(Method method) {
    if (!method.getReturnType().isAssignableFrom(void.class) || method.getParameterCount() != 0) {
      throw new IllegalArgumentException("Метод \"" + method.getName() + "\" должен быть void и не иметь аргументов");
    }
  }

  private static void runTest(Method testMethod, Object testObj) {
    try {
      testMethod.invoke(testObj);
    } catch (InvocationTargetException | IllegalAccessException e) {
      throw new RuntimeException("Не удалось запустить тестовый метод \"" + testMethod.getName() + "\"");
    } catch (AssertionError e) {

    }
  }

}
