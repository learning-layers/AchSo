package fi.aalto.legroup.achso.views.utilities;

import java.util.Random;

/**
 * Generates colours based on an object's identity. Will return the same colour for equal objects.
 */
public final class ColorGenerator {

    private static final int COLOR_RANGE_MIN = 0xFF000000;
    private static final int COLOR_RANGE_MAX = 0xFFFFFFFF;

    public static int getSeededColor(Object seedObject) {
        int seed;

        if (seedObject == null) {
            seed = 0;
        } else {
            seed = seedObject.hashCode();
        }

        return getRandomIntegerInRange(COLOR_RANGE_MIN, COLOR_RANGE_MAX, seed);
    }

    protected static int getRandomIntegerInRange(int min, int max, long seed) {
        // Adding 1 makes nextInt inclusive of the max value
        return new Random(seed).nextInt((max - min) + 1) + min;
    }

}
