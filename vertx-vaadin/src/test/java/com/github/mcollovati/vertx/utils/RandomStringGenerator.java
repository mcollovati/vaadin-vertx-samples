package com.github.mcollovati.vertx.utils;

import com.pholser.junit.quickcheck.generator.java.lang.AbstractStringGenerator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

/**
 * Created by marco on 16/07/16.
 */
public class RandomStringGenerator extends AbstractStringGenerator {

    private static final int MIN = 32;
    private static final int MAX = 126;

    @Override
    protected int nextCodePoint(SourceOfRandomness random) {
        return random.nextInt(MIN, MAX);
    }

    @Override
    protected boolean codePointInRange(int codePoint) {
        return codePoint >= MIN && codePoint <= MAX;
    }
}
