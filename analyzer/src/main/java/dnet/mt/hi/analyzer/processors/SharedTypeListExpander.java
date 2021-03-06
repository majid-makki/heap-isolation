package dnet.mt.hi.analyzer.processors;

import java.lang.reflect.*;
import java.util.*;

public class SharedTypeListExpander {

    public static List<Class> sharedTypes = new LinkedList<>();

    public void init(Set<String> initialSeed) {
        initialSeed.forEach(s -> {
            sharedTypes.add(Initializer.nameToClassMap.get(s));
        });
    }

    public void expand() {

        addAllAnnotations();
        Queue<Class> queue = new LinkedList<>();
        sharedTypes.forEach(queue::offer);
        Class clazz;
        while (!queue.isEmpty()) {
            clazz = queue.poll();
            if (!Modifier.isPrivate(clazz.getModifiers())) {
                Set<Class> newlyFoundReachableTypes = findNewReachableTypes(clazz);
                newlyFoundReachableTypes.forEach(queue::offer);
            }
            if (!sharedTypes.contains(clazz)) {
                sharedTypes.add(clazz);
            }
        }

    }

    private void addAllAnnotations() {
        for (Class clazz : Initializer.classToNameMap.keySet()) {
            if (clazz.isAnnotation() && !Modifier.isPrivate(clazz.getModifiers())) {
                sharedTypes.add(clazz);
            }
        }
    }

    private Set<Class> findNewReachableTypes(Class clazz) {
        Set<Class> result = new HashSet<>();

        result.addAll(findNewReachableTypesFromFields(clazz));
        result.addAll(findNewReachableTypesFromMethods(clazz));
        result = applyInheritance(result);

        return result;
    }

    private Set<Class> applyInheritance(Set<Class> initialSet) {
        Set<Class> result = new HashSet<>();

        initialSet.forEach(c -> {
            result.add(c);
            Set<Class> allParents = TypeHierarchyBuilder.allTypeNodes.get(c).getAllParents();
            allParents.forEach(aih -> {
                if (!sharedTypes.contains(aih)) {
                    result.add(aih);
                }
            });
        });

        return result;
    }

    private Set<Class> findNewReachableTypesFromMethods(Class clazz) {
        Set<Class> result = new HashSet<>();

        Method[] methods = clazz.getDeclaredMethods();

        Set<Type> signatureTypes;
        for (Method method : methods) {
            if (!Modifier.isPrivate(method.getModifiers())) {
                signatureTypes = new HashSet<>();
                signatureTypes.add(method.getGenericReturnType());
                signatureTypes.addAll(Arrays.asList(method.getGenericParameterTypes()));
                signatureTypes.addAll(Arrays.asList(method.getGenericExceptionTypes()));
                Set<Class> signatureClasses = new HashSet<>();
                signatureTypes.forEach(s -> signatureClasses.addAll(extractClasses(s)));
                signatureClasses.forEach(t -> {
                    if (!sharedTypes.contains(t)) {
                        result.add(t);
                    }
                });
            }
        }

        return result;
    }

    private Set<Class> findNewReachableTypesFromFields(Class clazz) {
        Set<Class> result = new HashSet<>();

        Field[] fields = clazz.getDeclaredFields();

        Set<Class> signatureClasses;
        for (Field field : fields) {
            if (!Modifier.isPrivate(field.getModifiers())) {
                signatureClasses = extractClasses(field.getGenericType());
                signatureClasses.forEach(t -> {
                    if (!sharedTypes.contains(t)) {
                        result.add(t);
                    }
                });
            }
        }

        return result;
    }

    private Set<Class> extractClasses(Type genericType) {
        Set<Class> result = new HashSet<>();

        Class clazz;
        if (genericType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) genericType;
            clazz = Initializer.nameToClassMap.get(pt.getRawType().getTypeName());
            for (Type t : pt.getActualTypeArguments()) {
                result.addAll(extractClasses(t));
            }
        } else {
            clazz = Initializer.nameToClassMap.get(genericType.getTypeName());
        }

        if (clazz != null) {
            result.add(clazz);
        }


        return result;
    }

}
