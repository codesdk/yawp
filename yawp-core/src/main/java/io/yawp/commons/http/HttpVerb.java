package io.yawp.commons.http;

import io.yawp.commons.http.annotation.GET;
import io.yawp.commons.http.annotation.PUT;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public enum HttpVerb {

    GET(GET.class), POST, PUT(PUT.class), PATCH, DELETE, OPTIONS;

    private Class<? extends Annotation> annotationClazz;

    private HttpVerb() {
    }

    private HttpVerb(Class<? extends Annotation> annotation) {
        this.annotationClazz = annotation;

    }

    public static HttpVerb fromString(String method) {
        String methodLowerCase = method.toUpperCase();
        return valueOf(methodLowerCase);
    }

    public boolean hasAnnotation(Method method) {
        if (annotationClazz == null) {
            return false;
        }
        return method.isAnnotationPresent(annotationClazz);
    }

    public String getAnnotationValue(Method method) {
        try {
            Annotation annotation = method.getAnnotation(annotationClazz);
            Method valueMethod = annotation.getClass().getMethod("value");
            return (String) valueMethod.invoke(annotation);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
