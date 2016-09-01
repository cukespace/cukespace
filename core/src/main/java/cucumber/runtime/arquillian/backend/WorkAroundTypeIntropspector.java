package cucumber.runtime.arquillian.backend;

import cucumber.runtime.java.TypeIntrospector;
import sun.reflect.ConstantPool;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import static java.lang.ClassLoader.getSystemClassLoader;

// imported from master cause otherwise j8 support is broken until we get a cucumber > 1.2.4
public class WorkAroundTypeIntropspector implements TypeIntrospector {
    public static final TypeIntrospector INSTANCE = new WorkAroundTypeIntropspector();

    private static final Method CLASS_GET_CONSTANT_POOL;
    private static final Method TYPE_GET_ARGUMENT_TYPES;
    private static final Method TYPE_GET_CLASS_NAME;

    static {
        try {
            CLASS_GET_CONSTANT_POOL = Class.class.getDeclaredMethod("getConstantPool");
            CLASS_GET_CONSTANT_POOL.setAccessible(true);
            final Class<?> type = Class.forName("jdk.internal.org.objectweb.asm.Type", false, getSystemClassLoader());
            TYPE_GET_ARGUMENT_TYPES = type.getMethod("getArgumentTypes", String.class);
            TYPE_GET_ARGUMENT_TYPES.setAccessible(true);
            TYPE_GET_CLASS_NAME = type.getMethod("getClassName");
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Type[] getGenericTypes(final Class<?> clazz) throws Exception {
        final ConstantPool constantPool = (ConstantPool) CLASS_GET_CONSTANT_POOL.invoke(clazz);
        final String typeString = getTypeString(constantPool);
        final Object argumentTypes = TYPE_GET_ARGUMENT_TYPES.invoke(null, typeString);
        final Type[] typeArguments = new Type[Array.getLength(argumentTypes)];
        for (int i = 0; i < typeArguments.length; i++) {
            typeArguments[i] = Class.forName(String.class.cast(TYPE_GET_CLASS_NAME.invoke(Array.get(argumentTypes, i))));
        }
        return typeArguments;
    }

    private String getTypeString(final ConstantPool constantPool) {
        final int size = constantPool.getSize();
        String[] memberRef = null;
        for (int i = size - 1; i > -1; i--) {
            try {
                memberRef = constantPool.getMemberRefInfoAt(i);
                break;
            } catch (final IllegalArgumentException e) {
                // eat error; null entry at ConstantPool index?
            }
        }
        return memberRef[2];
    }
}
