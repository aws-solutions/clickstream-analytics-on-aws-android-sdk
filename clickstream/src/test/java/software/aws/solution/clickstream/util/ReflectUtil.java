/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.aws.solution.clickstream.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

/**
 * reflect Util for test.
 */
public final class ReflectUtil {

    /**
     * hide the default constructor.
     */
    private ReflectUtil() {

    }

    /**
     * modify filed value even if the private static final param, only for test use.
     *
     * @param object        the object to modify.
     * @param fieldName     filed name to modify.
     * @param newFieldValue new filed value to set.
     * @throws Exception exception.
     */
    public static void modifyFiled(Object object, String fieldName, Object newFieldValue) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(object, newFieldValue);
    }

    /**
     * get the private field object of the given object.
     *
     * @param object    the filed parent object.
     * @param fieldName the filed name.
     * @return the filed object.
     * @throws Exception exception.
     */
    public static Object getFiled(Object object, String fieldName) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        return field.get(object);
    }

    /**
     * to invoke private method for not has param.
     *
     * @param object     the object to invoke.
     * @param methodName the method name of the object to invoke.
     * @return the object of the method return.
     * @throws Exception exception.
     */
    public static Object invokeMethod(Object object, String methodName) throws Exception {
        Method method = object.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(object);
    }

    /**
     * to invoke private method for not has param in super class.
     *
     * @param object     the object to invoke.
     * @param methodName the method name of the object to invoke.
     * @return the object of the method return.
     * @throws Exception exception.
     */
    public static Object invokeSuperMethod(Object object, String methodName) throws Exception {
        Method method = Objects.requireNonNull(object.getClass().getSuperclass()).getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(object);
    }

    /**
     * invokeMethod with method and args.
     *
     * @param object object to invoke.
     * @param method the method to invoke.
     * @param args   the method args.
     * @return the method return object.
     * @throws Exception exception
     */
    public static Object invokeMethod(Object object, Method method, Object... args)
        throws Exception {
        method.setAccessible(true);
        return method.invoke(object, args);
    }

    /**
     * new instance for object who has private constructor. the constructor default to get the first.
     *
     * @param clazz  which class to new instance.
     * @param params constructor params.
     * @return the instance of the class.
     * @throws Exception exception.
     */
    public static Object newInstance(Class<?> clazz, Object... params) throws Exception {
        Constructor<?>[] declaredConstructors = clazz.getDeclaredConstructors();
        Constructor<?> declaredConstructor = declaredConstructors[0];
        declaredConstructor.setAccessible(true);
        return declaredConstructor.newInstance(params);
    }
}
