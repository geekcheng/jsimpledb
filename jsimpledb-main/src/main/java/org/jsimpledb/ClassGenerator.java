
/*
 * Copyright (C) 2015 Archie L. Cobbs. All rights reserved.
 */

package org.jsimpledb;

import com.google.common.base.Converter;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;

import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import org.dellroad.stuff.java.Primitive;
import org.jsimpledb.core.DatabaseException;
import org.jsimpledb.core.ObjId;
import org.jsimpledb.core.Transaction;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates JSimpleDB model classes.
 *
 * <p>
 * The generated classes are subclasses of the the user-provided Java object classes (typically these are abstract classes).
 * The various annotated getter and setter methods will be overridden in the generated class.
 * If the user class implements {@link JObject}, then those methods will also be overridden with concrete implementations
 * in the generated class.
 */
class ClassGenerator<T> {

     // String to use for the "source file" attribute in generated classes.
    static final String GEN_SOURCE = "[GeneratedByJSimpleDB]";

    // Names of generated fields
    static final String TX_FIELD_NAME = "$tx";
    static final String ID_FIELD_NAME = "$id";
    static final String JFIELD_FIELD_PREFIX = "$f";
    static final String CACHED_FIELD_PREFIX = "$fc";
    static final String ENUM_CONVERTER_FIELD_PREFIX = "$ec";

    // JObject method handles
    static final Method JOBJECT_GET_OBJ_ID_METHOD;
    static final Method JOBJECT_GET_SCHEMA_VERSION_METHOD;
    static final Method JOBJECT_GET_TRANSACTION;
    static final Method JOBJECT_DELETE_METHOD;
    static final Method JOBJECT_EXISTS_METHOD;
    static final Method JOBJECT_IS_SNAPSHOT_METHOD;
    static final Method JOBJECT_RECREATE_METHOD;
    static final Method JOBJECT_UPGRADE_METHOD;
    static final Method JOBJECT_REVALIDATE_METHOD;
    static final Method JOBJECT_COPY_OUT_METHOD;
    static final Method JOBJECT_COPY_IN_METHOD;
    static final Method JOBJECT_COPY_TO_METHOD;
    static final Method JOBJECT_RESET_CACHED_FIELD_VALUES_METHOD;

    // JTransaction method handles
    static final Method GET_CURRENT_METHOD;
    static final Method READ_SIMPLE_FIELD_METHOD;
    static final Method WRITE_SIMPLE_FIELD_METHOD;
    static final Method READ_COUNTER_FIELD_METHOD;
    static final Method READ_SET_FIELD_METHOD;
    static final Method READ_LIST_FIELD_METHOD;
    static final Method READ_MAP_FIELD_METHOD;
    static final Method DELETE_METHOD;
    static final Method EXISTS_METHOD;
    static final Method RECREATE_METHOD;
    static final Method GET_SCHEMA_VERSION_METHOD;
    static final Method UPDATE_SCHEMA_VERSION_METHOD;
    static final Method REVALIDATE_METHOD;
    static final Method IS_SNAPSHOT_METHOD;
    static final Method COPY_TO_METHOD;
    static final Method GET_SNAPSHOT_TRANSACTION_METHOD;
    static final Method GET_TRANSACTION_METHOD;
    static final Method GET_METHOD;
    static final Method REGISTER_JOBJECT_METHOD;

    // Converter method handles
    static final Method CONVERTER_CONVERT_METHOD;
    static final Method CONVERTER_REVERSE_METHOD;

    // EnumConverter method handles
    static final Method ENUM_CONVERTER_CREATE_METHOD;

    // Transaction method handles
    static final Method TRANSACTION_READ_SIMPLE_FIELD_METHOD;
    static final Method TRANSACTION_WRITE_SIMPLE_FIELD_METHOD;

    static {
        try {

            // JObject methods
            JOBJECT_GET_OBJ_ID_METHOD = JObject.class.getMethod("getObjId");
            JOBJECT_GET_SCHEMA_VERSION_METHOD = JObject.class.getMethod("getSchemaVersion");
            JOBJECT_GET_TRANSACTION = JObject.class.getMethod("getTransaction");
            JOBJECT_DELETE_METHOD = JObject.class.getMethod("delete");
            JOBJECT_EXISTS_METHOD = JObject.class.getMethod("exists");
            JOBJECT_IS_SNAPSHOT_METHOD = JObject.class.getMethod("isSnapshot");
            JOBJECT_RECREATE_METHOD = JObject.class.getMethod("recreate");
            JOBJECT_UPGRADE_METHOD = JObject.class.getMethod("upgrade");
            JOBJECT_REVALIDATE_METHOD = JObject.class.getMethod("revalidate", Class[].class);
            JOBJECT_COPY_TO_METHOD = JObject.class.getMethod("copyTo",
              JTransaction.class, ObjId.class, CopyState.class, String[].class);
            JOBJECT_COPY_OUT_METHOD = JObject.class.getMethod("copyOut", String[].class);
            JOBJECT_COPY_IN_METHOD = JObject.class.getMethod("copyIn", String[].class);
            JOBJECT_RESET_CACHED_FIELD_VALUES_METHOD = JObject.class.getMethod("resetCachedFieldValues");

            // JTransaction methods
            GET_CURRENT_METHOD = JTransaction.class.getMethod("getCurrent");
            READ_SIMPLE_FIELD_METHOD = JTransaction.class.getMethod("readSimpleField", ObjId.class, int.class, boolean.class);
            WRITE_SIMPLE_FIELD_METHOD = JTransaction.class.getMethod("writeSimpleField",
              JObject.class, int.class, Object.class, boolean.class);
            READ_COUNTER_FIELD_METHOD = JTransaction.class.getMethod("readCounterField", ObjId.class, int.class, boolean.class);
            READ_SET_FIELD_METHOD = JTransaction.class.getMethod("readSetField", ObjId.class, int.class, boolean.class);
            READ_LIST_FIELD_METHOD = JTransaction.class.getMethod("readListField", ObjId.class, int.class, boolean.class);
            READ_MAP_FIELD_METHOD = JTransaction.class.getMethod("readMapField", ObjId.class, int.class, boolean.class);
            DELETE_METHOD = JTransaction.class.getMethod("delete", JObject.class);
            EXISTS_METHOD = JTransaction.class.getMethod("exists", ObjId.class);
            RECREATE_METHOD = JTransaction.class.getMethod("recreate", JObject.class);
            GET_SCHEMA_VERSION_METHOD = JTransaction.class.getMethod("getSchemaVersion", ObjId.class);
            UPDATE_SCHEMA_VERSION_METHOD = JTransaction.class.getMethod("updateSchemaVersion", JObject.class);
            REVALIDATE_METHOD = JTransaction.class.getMethod("revalidate", ObjId.class, Class[].class);
            IS_SNAPSHOT_METHOD = JTransaction.class.getMethod("isSnapshot");
            COPY_TO_METHOD = JTransaction.class.getMethod("copyTo",
              JTransaction.class, JObject.class, ObjId.class, CopyState.class, String[].class);
            GET_SNAPSHOT_TRANSACTION_METHOD = JTransaction.class.getMethod("getSnapshotTransaction");
            GET_TRANSACTION_METHOD = JTransaction.class.getMethod("getTransaction");
            GET_METHOD = JTransaction.class.getMethod("get", ObjId.class);
            REGISTER_JOBJECT_METHOD = JTransaction.class.getMethod("registerJObject", JObject.class);

            // Transaction methods
            TRANSACTION_READ_SIMPLE_FIELD_METHOD = Transaction.class.getMethod("readSimpleField",
              ObjId.class, int.class, boolean.class);
            TRANSACTION_WRITE_SIMPLE_FIELD_METHOD = Transaction.class.getMethod("writeSimpleField",
              ObjId.class, int.class, Object.class, boolean.class);

            // Converter
            CONVERTER_CONVERT_METHOD = Converter.class.getMethod("convert", Object.class);
            CONVERTER_REVERSE_METHOD = Converter.class.getMethod("reverse");

            // EnumConverter
            ENUM_CONVERTER_CREATE_METHOD = EnumConverter.class.getMethod("createEnumConverter", Class.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("internal error", e);
        }
    }

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    protected final JSimpleDB jdb;
    protected final JClass<T> jclass;
    protected final Class<T> modelClass;

    private Class<? extends T> subclass;
    private Constructor<? extends T> constructor;

    /**
     * Constructor for application classes.
     */
    ClassGenerator(JClass<T> jclass) {
        this(jclass.jdb, jclass, jclass.type);
    }

    /**
     * Constructor for a "JObject" class with no fields.
     */
    ClassGenerator(JSimpleDB jdb, Class<T> modelClass) {
        this(jdb, null, modelClass);
    }

    /**
     * Internal constructor.
     */
    private ClassGenerator(JSimpleDB jdb, JClass<T> jclass, Class<T> modelClass) {
        this.jdb = jdb;
        this.jclass = jclass;
        this.modelClass = modelClass;
    }

    /**
     * Get generated subclass' constructor.
     */
    public Constructor<? extends T> getConstructor() {
        if (this.constructor == null) {
            if (this.subclass == null)
                this.subclass = this.generateClass();
            try {
                this.constructor = this.subclass.getConstructor(JTransaction.class, ObjId.class);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("internal error", e);
            }
            this.constructor.setAccessible(true);
        }
        return this.constructor;
    }

    /**
     * Generate the Java class for this instance's {@link JClass}.
     */
    @SuppressWarnings("unchecked")
    public Class<? extends T> generateClass() {
        try {
            return (Class<? extends T>)this.jdb.loader.loadClass(this.getClassName().replace('/', '.'));
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("internal error", e);
        }
    }

    /**
     * Get class internal name. Note: this name contains slashes, not dots.
     */
    public String getClassName() {
        return this.getSuperclassName() + JSimpleDB.GENERATED_CLASS_NAME_SUFFIX;
    }

    /**
     * Get superclass (i.e., original Java model class) internal name.
     */
    public String getSuperclassName() {
        return Type.getInternalName(this.modelClass);
    }

// Database class

    /**
     * Generate the Java class bytecode for this instance's {@link JClass}.
     */
    protected byte[] generateBytecode() {

        // Generate class
        this.log.debug("begin generating class " + this.getClassName());
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER | Opcodes.ACC_SYNTHETIC,
          this.getClassName(), null, this.getSuperclassName(), new String[] { Type.getInternalName(JObject.class) });
        cw.visitSource(GEN_SOURCE, null);
        this.outputFields(cw);
        this.outputConstructors(cw);
        this.outputMethods(cw);
        cw.visitEnd();
        final byte[] classfile = cw.toByteArray();
        this.log.debug("done generating class " + this.getClassName());
        this.debugDump(System.out, classfile);

        // Done
        return classfile;
    }

    private void outputFields(ClassWriter cw) {

        // Output "$tx" field
        final FieldVisitor fv = cw.visitField(Opcodes.ACC_PROTECTED | Opcodes.ACC_FINAL | Opcodes.ACC_TRANSIENT,
          TX_FIELD_NAME, Type.getDescriptor(JTransaction.class), null, null);
        fv.visitEnd();

        // Output "$id" field
        final FieldVisitor idField = cw.visitField(Opcodes.ACC_PROTECTED | Opcodes.ACC_FINAL,
          ID_FIELD_NAME, Type.getDescriptor(ObjId.class), null, null);
        idField.visitEnd();

        // Output fields associated with JFields
        if (this.jclass != null) {
            for (JField jfield : this.jclass.jfields.values())
                jfield.outputFields(this, cw);
        }

        // Output field(s) for cached simple field flags
        if (this.jclass != null) {
            final int[] simpleFieldStorageIds = this.jclass.simpleFieldStorageIds;
            String lastFieldName = null;
            for (int i = 0; i < simpleFieldStorageIds.length; i++) {
                final String fieldName = this.getCachedFlagFieldName(i);
                if (fieldName.equals(lastFieldName))
                    continue;
                final FieldVisitor flagsField = cw.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_TRANSIENT,
                  fieldName, Type.getDescriptor(this.getCachedFlagFieldType(i)), null, null);
                flagsField.visitEnd();
                lastFieldName = fieldName;
            }
        }
    }

    private void outputConstructors(ClassWriter cw) {

        // Foo(JTransaction tx, ObjId id)
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>",
          Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(JTransaction.class), Type.getType(ObjId.class)), null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitInsn(Opcodes.DUP);
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ALOAD, 1);                                                              // this.tx = tx
        mv.visitFieldInsn(Opcodes.PUTFIELD, this.getClassName(), TX_FIELD_NAME, Type.getDescriptor(JTransaction.class));
        mv.visitVarInsn(Opcodes.ALOAD, 2);                                                              // this.id = id
        mv.visitFieldInsn(Opcodes.PUTFIELD, this.getClassName(), ID_FIELD_NAME, Type.getDescriptor(ObjId.class));
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, this.getSuperclassName(), "<init>", "()V", false);    // super()
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void outputMethods(ClassWriter cw) {

        // Output <clinit>, if needed
        if (this.jclass != null) {

            // Do any fields require initialization bytecode?
            boolean needClassInitializer = false;
            for (JField jfield : this.jclass.jfields.values()) {
                if (jfield.hasClassInitializerBytecode()) {
                    needClassInitializer = true;
                    break;
                }
            }

            // If so, add <clinit> method
            if (needClassInitializer) {
                MethodVisitor mv = cw.visitMethod(Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE, "<clinit>", "()V", null, null);
                mv.visitCode();
                for (JField jfield : this.jclass.jfields.values()) {
                    if (jfield.hasClassInitializerBytecode())
                        jfield.outputClassInitializerBytecode(this, mv);
                }
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
        }

        // Output JObject.getTransaction()
        MethodVisitor mv = this.startMethod(cw, JOBJECT_GET_TRANSACTION);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, this.getClassName(), TX_FIELD_NAME, Type.getDescriptor(JTransaction.class));
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // Add JObject.getObjId()
        mv = this.startMethod(cw, JOBJECT_GET_OBJ_ID_METHOD);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, this.getClassName(), ID_FIELD_NAME, Type.getDescriptor(ObjId.class));
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // Add JObject.getSchemaVersion()
        mv = this.startMethod(cw, JOBJECT_GET_SCHEMA_VERSION_METHOD);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, this.getClassName(), TX_FIELD_NAME, Type.getDescriptor(JTransaction.class));
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, this.getClassName(), ID_FIELD_NAME, Type.getDescriptor(ObjId.class));
        this.emitInvoke(mv, GET_SCHEMA_VERSION_METHOD);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // Add JObject.delete()
        mv = this.startMethod(cw, JOBJECT_DELETE_METHOD);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, this.getClassName(), TX_FIELD_NAME, Type.getDescriptor(JTransaction.class));
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        this.emitInvoke(mv, DELETE_METHOD);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // Add JObject.exists()
        mv = this.startMethod(cw, JOBJECT_EXISTS_METHOD);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, this.getClassName(), TX_FIELD_NAME, Type.getDescriptor(JTransaction.class));
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, this.getClassName(), ID_FIELD_NAME, Type.getDescriptor(ObjId.class));
        this.emitInvoke(mv, EXISTS_METHOD);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // Add JObject.isSnapshot()
        mv = this.startMethod(cw, JOBJECT_IS_SNAPSHOT_METHOD);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, this.getClassName(), TX_FIELD_NAME, Type.getDescriptor(JTransaction.class));
        this.emitInvoke(mv, IS_SNAPSHOT_METHOD);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // Add JObject.recreate()
        mv = this.startMethod(cw, JOBJECT_RECREATE_METHOD);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, this.getClassName(), TX_FIELD_NAME, Type.getDescriptor(JTransaction.class));
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        this.emitInvoke(mv, RECREATE_METHOD);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // Add JObject.revalidate()
        mv = this.startMethod(cw, JOBJECT_REVALIDATE_METHOD);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, this.getClassName(), TX_FIELD_NAME, Type.getDescriptor(JTransaction.class));
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, this.getClassName(), ID_FIELD_NAME, Type.getDescriptor(ObjId.class));
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        this.emitInvoke(mv, REVALIDATE_METHOD);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // Add JObject.upgrade()
        mv = this.startMethod(cw, JOBJECT_UPGRADE_METHOD);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, this.getClassName(), TX_FIELD_NAME, Type.getDescriptor(JTransaction.class));
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        this.emitInvoke(mv, UPDATE_SCHEMA_VERSION_METHOD);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // Add JObject.copyTo()
        mv = this.startMethod(cw, JOBJECT_COPY_TO_METHOD);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, this.getClassName(), TX_FIELD_NAME, Type.getDescriptor(JTransaction.class));
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        this.emitInvoke(mv, COPY_TO_METHOD);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // Add JObject.copyOut()
        mv = this.startMethod(cw, JOBJECT_COPY_OUT_METHOD);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitInsn(Opcodes.DUP);
        mv.visitFieldInsn(Opcodes.GETFIELD, this.getClassName(), TX_FIELD_NAME, Type.getDescriptor(JTransaction.class));
        this.emitInvoke(mv, GET_SNAPSHOT_TRANSACTION_METHOD);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitTypeInsn(Opcodes.NEW, Type.getType(CopyState.class).getInternalName());
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getType(CopyState.class).getInternalName(), "<init>", "()V", false);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        this.emitInvoke(mv, JOBJECT_COPY_TO_METHOD);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // Add JObject.copyIn()
        mv = this.startMethod(cw, JOBJECT_COPY_IN_METHOD);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        this.emitInvoke(mv, GET_CURRENT_METHOD);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitTypeInsn(Opcodes.NEW, Type.getType(CopyState.class).getInternalName());
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getType(CopyState.class).getInternalName(), "<init>", "()V", false);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        this.emitInvoke(mv, JOBJECT_COPY_TO_METHOD);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // Add JOBject.resetCachedFieldValues()
        mv = this.startMethod(cw, JOBJECT_RESET_CACHED_FIELD_VALUES_METHOD);
        mv.visitCode();
        if (this.jclass != null) {
            final int[] simpleFieldStorageIds = this.jclass.simpleFieldStorageIds;
            String lastFieldName = null;
            for (int i = 0; i < simpleFieldStorageIds.length; i++) {
                final String fieldName = this.getCachedFlagFieldName(i);
                if (fieldName.equals(lastFieldName))
                    continue;
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitInsn(Opcodes.ICONST_0);
                mv.visitFieldInsn(Opcodes.PUTFIELD, this.getClassName(),
                  fieldName, Type.getDescriptor(this.getCachedFlagFieldType(i)));
                lastFieldName = fieldName;
            }
        }
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // If no associated JClass, we're done
        if (this.jclass == null)
            return;

        // Add methods that override field getters & setters
        for (JField jfield : this.jclass.jfields.values())
            jfield.outputMethods(this, cw);
    }

// Helper Methods

    // Debug dump - requires asm-util
    protected void debugDump(PrintStream out, byte[] classfile) {
        // CHECKSTYLE OFF: GenericIllegalRegexp
        // java.io.PrintWriter pw = new java.io.PrintWriter(out, true);
        // pw.println("***************** BEGIN CLASSFILE ******************");
        // org.objectweb.asm.ClassReader cr = new org.objectweb.asm.ClassReader(classfile);
        // cr.accept(new org.objectweb.asm.util.TraceClassVisitor(pw), 0);
        // pw.flush();
        // pw.println("***************** END CLASSFILE ******************");
        // CHECKSTYLE ON: GenericIllegalRegexp
    }

    /**
     * Emit code that wraps the primitive value on the top of the stack.
     */
    void wrap(MethodVisitor mv, Primitive<?> primitive) {
        final Type wrapperType = Type.getType(primitive.getWrapperType());
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, wrapperType.getInternalName(), "valueOf",
          Type.getMethodDescriptor(wrapperType, Type.getType(primitive.getType())), false);
    }

    /**
     * Emit code that unwraps the primitive value on the top of the stack.
     */
    void unwrap(MethodVisitor mv, Primitive<?> primitive) {
        final Method unwrapMethod = primitive.getUnwrapMethod();
        this.emitInvoke(mv, unwrapMethod);
    }

    /**
     * Emit code to invoke a method. This assumes the stack is loaded.
     */
    void emitInvoke(MethodVisitor mv, Method method) {
        final boolean isInterface = method.getDeclaringClass().isInterface();
        final boolean isStatic = (method.getModifiers() & Modifier.STATIC) != 0;
        mv.visitMethodInsn(isInterface ? Opcodes.INVOKEINTERFACE : isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL,
          Type.getInternalName(method.getDeclaringClass()), method.getName(), Type.getMethodDescriptor(method), isInterface);
    }

    /**
     * Emit code to INVOKEVIRTUAL a method using the specified class. This assumes the stack is loaded.
     */
    void emitInvoke(MethodVisitor mv, String className, Method method) {
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, method.getName(), Type.getMethodDescriptor(method), false);
    }

    /**
     * Create {@link MethodVisitor} to implement or override the given method.
     */
    MethodVisitor startMethod(ClassWriter cw, Method method) {
        return cw.visitMethod(
          method.getModifiers() & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED),
          method.getName(), Type.getMethodDescriptor(method), null, this.getExceptionNames(method));
    }

    /**
     * Get list of exception types for method.
     */
    String[] getExceptionNames(Method method) {
        ArrayList<String> list = new ArrayList<String>();
        for (Class<?> type : method.getExceptionTypes())
            list.add(Type.getType(type).getInternalName());
        return list.toArray(new String[list.size()]);
    }

    // Callback interface for emitting bytecode
    interface CodeEmitter {

        /**
         * Output some method bytecode or whatever.
         */
        void emit(MethodVisitor mv);
    }

// Cached value flags field(s)

    String getCachedFlagFieldName(JSimpleField jfield) {
        return this.getCachedFlagFieldName(this.getCachedFlagIndex(jfield));
    }

    int getCachedFlagBit(JSimpleField jfield) {
        return 1 << (this.getCachedFlagIndex(jfield) % 32);
    }

    Class<?> getCachedFlagFieldType(JSimpleField jfield) {
        return this.getCachedFlagFieldType(this.getCachedFlagIndex(jfield));
    }

    private Class<?> getCachedFlagFieldType(int simpleFieldIndex) {
        final int numSimpleFields = this.jclass.simpleFieldStorageIds.length;
        Preconditions.checkArgument(simpleFieldIndex >= 0 && simpleFieldIndex < numSimpleFields);
        if (simpleFieldIndex / 32 < numSimpleFields / 32)
            return int.class;
        final int tail = numSimpleFields % 32;
        return tail <= 8 ? byte.class : tail <= 16 ? short.class : int.class;
    }

    private String getCachedFlagFieldName(int simpleFieldIndex) {
        Preconditions.checkArgument(simpleFieldIndex >= 0 && simpleFieldIndex < this.jclass.simpleFieldStorageIds.length);
        return ClassGenerator.CACHED_FIELD_PREFIX + (simpleFieldIndex / 32);
    }

    private int getCachedFlagIndex(JSimpleField jfield) {
        Preconditions.checkArgument(jfield.parent == this.jclass);
        final int simpleFieldIndex = Ints.indexOf(this.jclass.simpleFieldStorageIds, jfield.storageId);
        Preconditions.checkArgument(simpleFieldIndex != -1);
        return simpleFieldIndex;
    }
}

