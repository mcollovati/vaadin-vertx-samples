package com.github.mcollovati.vertxvaadin.flowdemo;

import java.io.Serializable;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import com.vaadin.flow.function.SerializableFunction;

public interface SerializableUtils<T> extends Comparator<T>, Serializable {

    static <T, U> Function<T, U> function(SerializableFunction<T, U> fn) {
        return fn;
    }

    static <T> ToIntFunction<T> toIntFunction(SerializableToIntFunction<T> fn) {
        return fn;
    }

    interface SerializableToIntFunction<R> extends ToIntFunction<R>, Serializable {}

}
