package com.petnbu.petnbu.util;

/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Borrowed from github.com/romannurik/muzei
 */
public class MathUtils {

    private MathUtils() { }

    public static float constrain(float min, float max, float v) {
        return Math.max(min, Math.min(max, v));
    }

    public static double trimDouble(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, value >= 0? RoundingMode.FLOOR: RoundingMode.CEILING);
        return bd.doubleValue();
    }
}
