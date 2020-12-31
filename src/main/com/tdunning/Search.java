package com.tdunning;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Search {
    /**
     * Returns the gini score for two counts. This is largest when the counts are equal.
     *
     * @param a One count
     * @param b The other
     * @return The Gini score
     */
    static double gini(double a, double b) {
        return a * b / (a + b);
    }

    /**
     * Returns the decrease in weighted Gini due to a split. This is computed
     * by computing the Gini score without the split less the scores for each
     * side of the fork weighted by the fraction of samples in each fork.
     * <p>
     * For a split that makes no difference in the count imbalance, the difference
     * will be zero. For a split that splits relatively evenly weighted counts into
     * (oppositely) biased splits on each side of the fork, we get a higher score.
     *
     * @param v The two counts for each of two sides of a fork
     * @return The difference in Gini after the split relative to before.
     */
    static double g4(int[] v) {
        double a = v[0];
        double b = v[1];
        double c = v[2];
        double d = v[3];

        double n = a + b + c + d;
        double w1 = (a + b) / n;
        double w2 = (c + d) / n;
        return gini(a + c, b + d) - gini(a, b) - gini(c, d);
    }

    /**
     * Returns the degree two which one split is better than another based on Gini difference. Positive
     * if the first split is better.
     *
     * @param a The first split
     * @param b The second
     * @return The difference in Gini difference scores for the two splits
     */
    static double g8(int[] a, int[] b) {
        return g4(a) - g4(b);
    }

    static int[] diff(int[] r, int[] a, int[] b) {
        if (r == null) {
            r = repI(0, a.length);
        }
        assert a.length == b.length;
        for (int i = 0; i < 4; i++) {
            r[i] = a[i] - b[i];
        }
        return r;
    }

    static int[] sum(int[] r, int[] a, int[] b) {
        if (r == null) {
            r = Arrays.copyOf(a, a.length);
        }
        for (int i = 0; i < 4; i++) {
            r[i] = a[i] + b[i];
        }
        return r;
    }

    static int[] repI(int value, int times) {
        int[] r = new int[times];
        Arrays.fill(r, value);
        return r;
    }

    /**
     * Scans the neighboring solutions for splits that preserve the invariant.
     *
     * @param s1 Step for left hand split.
     * @param s2 Step for right hand split.
     * @return True if the new neighbor is valid, false if the iteration should stop.
     */
    static boolean neighbors(int[] s1, int[] s2) {
        assert s1[0] + s1[2] == s2[0] + s2[2] && s1[1] + s1[3] == s2[1] + s2[3];

        for (int i = 0; i < 2; i++) {
            s2[i]++;
            s2[i + 2]--;
            if (s2[i] <= 1 && s2[i + 2] >= -1) {
                return true;
            }
            int k = s1[i] + s1[i + 2];
            s2[i] = Math.max(-1, k - 1);
            s2[i + 2] = s1[i] + s1[i + 2] - s2[i];
        }
        boolean r = neighbors(s1);
        if (r) {
            for (int i = 0; i < 2; i++) {
                int k = s1[i] + s1[i + 2];
                s2[i] = Math.max(-1, k - 1);
                s2[i + 2] = s1[i] + s1[i + 2] - s2[i];
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Iterates through the immediate neighbors (including diagonals). Mutates
     * step. Note that when we return false, step is reset to all -1 values.
     *
     * @param step The change in coordinate
     * @return true if the new value is a valid neighbor, false if we finished
     */
    static boolean neighbors(int[] step) {
        for (int i = 0; i < step.length; i++) {
            step[i]++;
            if (step[i] <= 1) {
                return true;
            }
            step[i] = -1;
        }
        return false;
    }

    static class Solution {
        int[] s1;
        int[] s2;

        public Solution(int[] s1, int[] s2) {
            this.s1 = Arrays.copyOf(s1, s1.length);
            this.s2 = Arrays.copyOf(s2, s2.length);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Solution)) return false;
            Solution solution = (Solution) o;
            return Arrays.equals(s1, solution.s1) &&
                    Arrays.equals(s2, solution.s2);
        }

        @Override
        public int hashCode() {
            int result = 0;
            for (int x : s1) {
                result = 10 * result + x;
            }
            for (int x : s2) {
                result = 10 * result + x;
            }
            return result;
        }
    }

    static class HistoryMap {
        // positive colors indicate a cell with a best path
        // zero color indicates a cell has never been explored
        // A -1 color indicates that the cell is subject to a current exploration
        // and will be colored with a positive color as the recursion retreats
        int maxColor = 0;
        private final Map<Solution, Integer> color = new HashMap<>();
        private double[] best;

        public HistoryMap() {
            best = new double[2];
        }

        void mark(int[] w1, int[] w2, int newColor) {
            this.color.put(new Solution(w1, w2), newColor);
        }

        int nextColor() {
            return ++maxColor;
        }

        int getColor(int[] w1, int[] w2) {
            return color.getOrDefault(new Solution(w1, w2), 0);
        }

        int getColor(Solution w) {
            return getColor(w.s1, w.s2);
        }

        double getBest(int color) {
            return best[color];
        }

        void setBest(int color, double value) {
            while (best.length <= color) {
                best = Arrays.copyOf(best, 2 * best.length + 1);
            }
            best[color] = value;
        }

        public static class Basin {
            int count;
            double value;

            public Basin(int count, double value) {
                this.count = count;
                this.value = value;
            }
        }

        public TreeMap<Integer, Basin> histogram() {
            int[] counts = new int[best.length];
            for (Solution w : color.keySet()) {
                int c = getColor(w);
                counts[c]++;
            }
            TreeMap<Integer, Basin> basins = new TreeMap<>();
            for (int c = 0; c < maxColor; c++) {
                if (counts[c] > 5) {
                    basins.put(c, new Basin(counts[c], best[c]));
                }
            }
            return basins;
        }
    }

    /**
     * Recursive search for minimum values and attraction basins.
     * <p>
     * Each neighbor is recursively searched if it hasn't already been visited.
     * We can tell it was visited by checking the history map. Whenever a colored
     * neighbor is encountered, we compare that result and keep it as our return
     * color if it is our first known result or better than our current best.
     * When no neighbor leads to a value better than our current value, we have
     * found a new local minimum and get to pick a new color and record the value.
     *
     * @param base1   Coordinate of the base of the search
     * @param step1   Offset from base of search
     * @param base2   Coordinate of the base of the search
     * @param step2   Offset from base of search
     * @param limit   Maximum length of path to search
     * @param history History of marked paths
     * @param greedy  If true, only consider steps to neighbors with better scores
     * @return The color of the best minimum from this starting point
     */
    static int treeSearch(int[] base1, int[] step1, int[] base2, int[] step2, int limit, double sign, HistoryMap history, boolean greedy) {
        if (limit < 0) {
            // in too deep
            return -2;
        }
        int[] n1 = diff(null, base1, step1);
        int[] n2 = diff(null, base2, step2);
        if (anyNegative(n1) || anyNegative(n2)) {
            // out of bounds, made counts negative
            return -3;
        }
        int myColor = history.getColor(step1, step2);
        double myBest = Double.POSITIVE_INFINITY;
        if (myColor != 0) {
            // already solved this square if positive
            // already working on it if -1
            return myColor;
        }
        myColor = -1;
        double v = sign * g8(n1, n2);

        // mark current square as being in progress
        history.mark(step1, step2, -1);

        // scan the neighbors
        int[] x1 = repI(-1, base1.length);
        int[] x2 = repI(-1, base1.length);
        do {
            sum(n1, step1, x1);
            if (anyNegative(n1)) {
                continue;
            }
            sum(n2, step2, x2);
            if (anyNegative(n2)) {
                continue;
            }

            if (greedy) {
                // don't consider any neighbors but those that show short term win
                double vn = sign * g8(diff(null, base1, n1), diff(null, base2, n2));
                if (vn > v) {
                    continue;
                }
            }

            int c = treeSearch(base1, n1, base2, n2, limit - 1, sign, history, greedy);
            if (c > 0) {
                // found a solution for this neighbor, but is it better?
                double vx = history.getBest(c);
                if (vx < myBest) {
                    myColor = c;
                    myBest = vx;
                }
            }
        } while (neighbors(x1, x2));
        if (myColor <= 0) {
            myColor = history.nextColor();
            history.setBest(myColor, v);
        }
        history.mark(step1, step2, myColor);
        return myColor;
    }

    static class ScoredSolution implements Comparable<ScoredSolution> {
        static AtomicInteger idCounter = new AtomicInteger(0);
        double score;
        int id = idCounter.getAndIncrement();
        int[] x1;
        int[] x2;

        public ScoredSolution(double score, int[] x1, int[] x2) {
            this.score = score;
            this.x1 = Arrays.copyOf(x1, x1.length);
            this.x2 = Arrays.copyOf(x2, x2.length);
        }

        @Override
        public String toString() {
            return "ScoredSolution{" +
                    "score=" + score +
                    ", x1=" + Arrays.toString(x1) +
                    ", x2=" + Arrays.toString(x2) +
                    '}';
        }


        @Override
        public int compareTo(ScoredSolution o) {
            int r = Double.compare(score, o.score);
            if (r == 0) {
                return id - o.id;
            }
            return r;
        }
    }

    static PriorityQueue<ScoredSolution> exhaustive(int[] base, int limit, double sign) {
        int[] step1 = repI(0, base.length);
        int[] step2 = repI(0, base.length);
        int[] x1 = repI(0, base.length);
        int[] x2 = repI(0, base.length);
        PriorityQueue<ScoredSolution> pq = new PriorityQueue<>();
        double best = Double.POSITIVE_INFINITY;
        do {
            x1 = diff(x1, base, step1);
            if (anyNegative(x1)) {
                continue;
            }
            x2 = diff(x2, base, step2);
            if (anyNegative(x2)) {
                continue;
            }
            double v = sign * g8(x1, x2);
            if (v < best) {
                best = v;
            }
            if (v < best + 1) {
                pq.add(new ScoredSolution(-v, x1, x2));
                while (pq.size() > 20) {
                    pq.poll();
                }
            }
        } while (increment(step1, step2, base, base, limit));
        return pq;
    }

    private static boolean anyOver(int[] position, double limit) {
        for (int x : position) {
            if (x >= limit) {
                return true;
            }
        }
        return false;
    }

    private static boolean anyNegative(int[] dx) {
        for (int x : dx) {
            if (x < 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Iterates e through the cartesian product of \prod_i [0..k_i] where n is the length of k.
     * Returns true every time until the time you increment the last value back to the beginning.
     * Leaves e with all zeros after returning true.
     *
     * @param e     The thing to increment
     * @param k     The limits
     * @param limit The limit on the sum of e_i
     * @return true except when the last value is incremented back to zero
     */
    static boolean increment(int[] e, int[] k, int limit) {
        double sum = 0;
        int n = k.length;
        for (int i = 0; i < n; i++) {
            sum += e[i];
        }
        if (sum > limit) {
            return false;
        }
        for (int i = 0; i < n; i++) {
            e[i]++;
            sum++;
            if (e[i] <= k[i] && sum <= limit) {
                return true;
            }
            sum -= e[i];
            e[i] = 0;
        }
        return false;
    }

    /**
     * Increments two correlated vectors e1 and e2. The first vector increments through
     * all values in the style of #increment. The second vector increments similarly
     * except for the constraints that e1[0]+e1[2] == e2[0]+e2[2] and e1[1]+e1[3] == e2[1]+e2[3]
     */
    static boolean increment(int[] e1, int[] e2, int[] k1, int[] k2, int limit) {
        // verify structure
        assert e1.length == 4 && e2.length == 4 && k1.length == 4 && k2.length == 4;
        // verify invariant before starting
        assert e1[0] + e1[2] == e2[0] + e2[2] && e1[1] + e1[3] == e2[1] + e2[3];
        assert k1[0] + k1[2] == k2[0] + k2[2] && k1[1] + k1[3] == k2[1] + k2[3];
        for (int i = 0; i < 2; i++) {
            e2[i]++;
            e2[i + 2]--;
            if (e1[i] <= k1[i] && e2[i] <= k2[i] && e2[i + 2] >= 0) {
                return true;
            }
            resetE2(e1, e2, k2, i);
        }
        boolean r = increment(e1, k1, limit);
        if (!r) {
            return r;
        } else {
            for (int i = 0; i < 2; i++) {
                resetE2(e1, e2, k2, i);
            }
            return true;
        }
    }

    private static void resetE2(int[] e1, int[] e2, int[] k2, int i) {
        e2[i] = 0;
        e2[i + 2] = e1[i] + e1[i + 2];
        if (e2[i + 2] > k2[i + 2]) {
            e2[i + 2] = k2[i + 2];
            e2[i] = e1[i] + e1[i + 2] - e2[i + 2];
        }
    }
}
