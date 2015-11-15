/**
 *
 * Copyright (c) 2006-2015, Speedment, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); You may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.speedment.util;

import com.speedment.annotation.Api;
import com.speedment.stream.MapStream;
import com.speedment.internal.logging.Logger;
import com.speedment.internal.logging.LoggerManager;
import com.speedment.encoder.JsonEncoder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.Optional;
import java.util.StringJoiner;

import static java.util.stream.Collector.Characteristics.CONCURRENT;
import static com.speedment.util.StaticClassUtil.instanceNotAllowed;
import static com.speedment.util.NullUtil.requireNonNulls;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static com.speedment.util.NullUtil.requireNonNulls;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * Utility methods for collecting Speedment streams in various ways.
 * 
 * @author pemi
 * @author Emil Forslund
 * @since 2.1
 */
@Api(version = "2.2")
public final class CollectorUtil {

    private final static Logger LOGGER = LoggerManager.getLogger(CollectorUtil.class);
    private static final String NULL_TEXT = " must not be null";

    /**
     * Returns a collector that calls the {@link #toJson(java.lang.Object)} method
     * for each element in the stream and joins the resuling stream separated
     * by commas and surrounded by square brackets. 
     * <p>
     * The result of the stream:
     * <pre>    a b c</pre> 
     * would be:
     * <pre>    ['a', 'b', 'c']</pre> 
     * 
     * @param <T>  the type of the stream
     * @return     the json string
     */
    public static <T> Collector<T, ?, String> toJson() {
        return new Parser<>(CollectorUtil::toJson, l -> "[" + l.stream().collect(joining(", ")) + "]");
    }

    /**
     * Returns a collector that calls the specified encoder for each element in 
     * the stream and joins the resuling stream separated by commas and 
     * surrounded by square brackets. 
     * <p>
     * The result of the stream:
     * <pre>    a b c</pre> 
     * would be:
     * <pre>    ['a', 'b', 'c']</pre> 
     * 
     * @param <T>      the type of the stream
     * @param encoder  the enocder to use
     * @return         the json string
     */
    public static <T> Collector<T, ?, String> toJson(JsonEncoder<T> encoder) {
        requireNonNull(encoder);
        return new Parser<>(encoder::apply, l -> "[" + l.stream().collect(joining(", ")) + "]");
    }

    /**
     * If the specified object has a method called {@code toJson}, execute it
     * and cast the result into a {@code String}. If something goes wrong, an
     * error is logged and {@code null} is returned.
     * 
     * @param <T>     the type of the entity
     * @param entity  the entity to enocde
     * @return        the encoded string or {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <T> String toJson(T entity) {
        requireNonNull(entity);
        
        try {
            final Method m = entity.getClass().getMethod("toJson");
            return (String) m.invoke(entity);
            
        } catch (NoSuchMethodException | 
                 SecurityException | 
                 IllegalAccessException | 
                 IllegalArgumentException | 
                 InvocationTargetException | 
                 ClassCastException ex) {

            LOGGER.error(
                ex,
                "Could not parse entity to Json. Make sure '"
                + entity + "' is generated by Speedment."
            );

            return null;
        }
    }

    private static class Parser<T> implements Collector<T, List<String>, String> {

        private final Function<T, String> converter;
        private final Function<List<String>, String> merger;

        public Parser(Function<T, String> converter, Function<List<String>, String> merger) {
            this.converter = requireNonNull(converter);
            this.merger = requireNonNull(merger);
        }

        @Override
        public Supplier<List<String>> supplier() {
            return () -> Collections.synchronizedList(new ArrayList<>());
        }

        @Override
        public BiConsumer<List<String>, T> accumulator() {
            return (l, t) -> {
                synchronized (l) {
                    l.add(converter.apply(t));
                }
            };
        }

        @Override
        public BinaryOperator<List<String>> combiner() {
            return (l1, l2) -> {
                synchronized (l1) {
                    l1.addAll(l2);
                    return l1;
                }
            };
        }

        @Override
        public Function<List<String>, String> finisher() {
            return merger::apply;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return EnumSet.of(CONCURRENT);
        }
    }

    
    @SafeVarargs
    @SuppressWarnings({"unchecked", "varargs"})
    public static <T> T of(Supplier<T> supplier, Consumer<T> modifier, Consumer<T>... additionalModifiers) {
        requireNonNull(supplier, "supplier" + NULL_TEXT);
        requireNonNull(modifier, "modifier" + NULL_TEXT);
        requireNonNulls(additionalModifiers, "additionalModifiers" + NULL_TEXT);
        final T result = supplier.get();
        modifier.accept(result);
        Stream.of(additionalModifiers).forEach((Consumer<T> c) -> {
            c.accept(result);
        });
        return result;
    }

    public static <I, T> T of(Supplier<I> supplier, Consumer<I> modifier, Function<I, T> finisher) {
        Objects.requireNonNull(supplier, "supplier" + NULL_TEXT);
        Objects.requireNonNull(modifier, "modifier" + NULL_TEXT);
        Objects.requireNonNull(finisher, "finisher" + NULL_TEXT);
        final I intermediateResult = supplier.get();
        modifier.accept(intermediateResult);
        return finisher.apply(intermediateResult);
    }

    public static <T> Collector<T, Set<T>, Set<T>> toUnmodifiableSet() {
        return Collector.of(HashSet::new, Set::add, (left, right) -> {
            left.addAll(right);
            return left;
        }, Collections::unmodifiableSet, Collector.Characteristics.UNORDERED);
    }

    @SafeVarargs
    @SuppressWarnings({"unchecked", "varargs"})
    public static <T> Set<T> unmodifiableSetOf(T... items) {
        requireNonNulls(items);
        return Stream.of(items).collect(toUnmodifiableSet());
    }

    
    public static Collector<String, ?, String> joinIfNotEmpty(String delimiter) {
        return joinIfNotEmpty(delimiter, "", "");
    }

    /**
     * Similar to the 
     * {@link java.util.stream.Collectors#joining(java.lang.CharSequence, java.lang.CharSequence, java.lang.CharSequence) } 
     * method except that this method surrounds the result with the specified
     * {@code prefix} and {@code suffix} even if the stream is empty.
     * 
     * @param delimiter  the delimiter to separate the strings
     * @param prefix     the prefix to put before the result
     * @param suffix     the suffix to put after the result
     * @return           a collector for joining string elements
     */
    public static Collector<String, ?, String> joinIfNotEmpty(String delimiter, String prefix, String suffix) {
        return new CollectorImpl<>(
            () -> new StringJoiner(delimiter),
            StringJoiner::add,
            StringJoiner::merge,
            s -> s.length() > 0
                ? prefix + s + suffix
                : s.toString(),
            Collections.emptySet()
        );
    }

    /**
     * Returns the specified string wrapped as an Optional. If the string was
     * null or empty, the Optional will be empty.
     * 
     * @param str  the string to wrap
     * @return     the string wrapped as an optional
     */
    public static Optional<String> ifEmpty(String str) {
        return Optional.ofNullable(str).filter(s -> !s.isEmpty());
    }

    /**
     * Returns a new {@link MapStream} where the elements have been grouped together using
     * the specified function.
     * 
     * @param <T>      the stream element type
     * @param <C>      the type of the key to group by
     * @param grouper  the function to use for grouping
     * @return         a {@link MapStream} grouped by key
     */
    public static <T, C> Collector<T, ?, MapStream<C, List<T>>> groupBy(Function<T, C> grouper) {
        return new CollectorImpl<>(
            () -> new GroupHolder<>(grouper),
            GroupHolder::add,
            GroupHolder::merge,
            GroupHolder::finisher,
            Collections.emptySet()
        );
    }

    private static class GroupHolder<C, T> {

        private final Function<T, C> grouper;
        private final Map<C, List<T>> elements;

        private final Function<C, List<T>> createList = c -> new ArrayList<>();

        public GroupHolder(Function<T, C> grouper) {
            this.grouper = grouper;
            this.elements = new HashMap<>();
        }

        public void add(T element) {
            final C key = grouper.apply(element);
            elements.computeIfAbsent(key, createList)
                .add(element);
        }

        public GroupHolder<C, T> merge(GroupHolder<C, T> holder) {
            holder.elements.entrySet().stream()
                .forEach(e
                    -> elements.computeIfAbsent(e.getKey(), createList)
                    .addAll(e.getValue())
                );

            return this;
        }

        public MapStream<C, List<T>> finisher() {
            return MapStream.of(elements);
        }
    }

    /**
     * Simple implementation class for {@code Collector}.
     *
     * @param <T> the type of elements to be collected
     * @param <A> the type of the intermediate holder
     * @param <R> the type of the result
     */
    static class CollectorImpl<T, A, R> implements Collector<T, A, R> {

        private final Supplier<A> supplier;
        private final BiConsumer<A, T> accumulator;
        private final BinaryOperator<A> combiner;
        private final Function<A, R> finisher;
        private final Set<Collector.Characteristics> characteristics;

        CollectorImpl(Supplier<A> supplier,
            BiConsumer<A, T> accumulator,
            BinaryOperator<A> combiner,
            Function<A, R> finisher,
            Set<Collector.Characteristics> characteristics) {
            this.supplier = supplier;
            this.accumulator = accumulator;
            this.combiner = combiner;
            this.finisher = finisher;
            this.characteristics = characteristics;
        }

        @SuppressWarnings("unchecked")
        CollectorImpl(Supplier<A> supplier,
            BiConsumer<A, T> accumulator,
            BinaryOperator<A> combiner,
            Set<Collector.Characteristics> characteristics) {

            this(supplier, accumulator, combiner, i -> (R) i, characteristics);
        }

        @Override
        public BiConsumer<A, T> accumulator() {
            return accumulator;
        }

        @Override
        public Supplier<A> supplier() {
            return supplier;
        }

        @Override
        public BinaryOperator<A> combiner() {
            return combiner;
        }

        @Override
        public Function<A, R> finisher() {
            return finisher;
        }

        @Override
        public Set<Collector.Characteristics> characteristics() {
            return characteristics;
        }
    }

    /**
     * Utility classes should not be instantiated.
     */
    private CollectorUtil() {
        instanceNotAllowed(getClass());
    }
}