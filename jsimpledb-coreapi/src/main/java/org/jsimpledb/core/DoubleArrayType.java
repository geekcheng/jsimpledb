
/*
 * Copyright (C) 2015 Archie L. Cobbs. All rights reserved.
 */

package org.jsimpledb.core;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Doubles;
import com.google.common.reflect.TypeToken;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jsimpledb.util.ByteReader;
import org.jsimpledb.util.ByteWriter;

/**
 * {@code double[]} array type. Does not support null arrays.
 *
 * <p>
 * Array elements are encoded using {@link DoubleType}, and the array is terminated by {@code 0x0000000000000000L},
 * which is an encoded value that can never be emitted by {@link DoubleType}.
 */
class DoubleArrayType extends Base64ArrayType<double[], Double> {

    private static final long serialVersionUID = 7502947488125172882L;

    private static final int NUM_BYTES = 8;
    private static final byte[] END = new byte[NUM_BYTES];

    private final DoubleType doubleType = new DoubleType();

    @SuppressWarnings("serial")
    DoubleArrayType() {
        super(FieldTypeRegistry.DOUBLE, new TypeToken<double[]>() { });
    }

    @Override
    public double[] read(ByteReader reader) {
        final ArrayList<Double> list = new ArrayList<>();
        while (true) {
            final byte[] next = reader.readBytes(NUM_BYTES);
            if (Arrays.equals(next, END))
                break;
            list.add(this.doubleType.read(new ByteReader(next)));
        }
        return this.createArray(list);
    }

    @Override
    public void write(ByteWriter writer, double[] array) {
        Preconditions.checkArgument(array != null, "null array");
        final int length = this.getArrayLength(array);
        for (int i = 0; i < length; i++)
            this.doubleType.write(writer, array[i]);
        writer.write(END);
    }

    @Override
    public void skip(ByteReader reader) {
        while (true) {
            final byte[] next = reader.readBytes(NUM_BYTES);
            if (Arrays.equals(next, END))
                break;
        }
    }

    @Override
    public boolean hasPrefix0xff() {
        return this.doubleType.hasPrefix0xff();
    }

    @Override
    protected int getArrayLength(double[] array) {
        return array.length;
    }

    @Override
    protected Double getArrayElement(double[] array, int index) {
        return array[index];
    }

    @Override
    protected double[] createArray(List<Double> elements) {
        return Doubles.toArray(elements);
    }

    @Override
    protected void encode(double[] array, DataOutputStream output) throws IOException {
        for (int i = 0; i < array.length; i++)
            output.writeDouble(array[i]);
    }

    @Override
    protected double[] decode(DataInputStream input, int numBytes) throws IOException {
        final double[] array = this.checkDecodeLength(numBytes);
        for (int i = 0; i < array.length; i++)
            array[i] = input.readDouble();
        return array;
    }
}

