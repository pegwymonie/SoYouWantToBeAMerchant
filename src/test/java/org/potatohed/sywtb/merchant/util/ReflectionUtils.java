package org.potatohed.sywtb.merchant.util;

import java.lang.reflect.Field;

public class ReflectionUtils {

  private ReflectionUtils(){};

  public static void reflectiveSet(Object target, String fieldName, Object value) {
    try {
      Field fieldToSet = target.getClass().getDeclaredField(fieldName);
      fieldToSet.setAccessible(true);
      fieldToSet.set(target, value);
      fieldToSet.setAccessible(false);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }
}
