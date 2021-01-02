package com.tdunning;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class SearchTest {
    @Test
    public void increment() {
        // for small limits, we get triangular number of iterations
        Assert.assertEquals(6, counterLoop(new int[]{4, 3}, 2));
        Assert.assertEquals(10, counterLoop(new int[]{4, 3}, 3));
        // bigger limits give complex results
        int[] k = new int[]{5, 4, 3};
        Assert.assertEquals(51, counterLoop(k, 5));
        Assert.assertEquals(69, counterLoop(k, 6));
        // really big limits aren't really limits so we see all combinations
        Assert.assertEquals(6 * 5 * 4, counterLoop(k, 12));

        // regression tests, just because
        Assert.assertEquals(1001, counterLoop(Search.repI(10, 4), 10));
        Assert.assertEquals(10626, counterLoop(Search.repI(20, 4), 20));
    }

    @Test
    public void increment6() {
        int[] k = new int[]{5, 3, 6, 7};
        Assert.assertEquals(397, checkLoop6(k, 4));
        Assert.assertEquals(9828, checkLoop6(k, 10));
        k = new int[]{20, 20, 20, 20};
        Assert.assertEquals(51324, checkLoop6(k, 12));
    }

    private int checkLoop6(int[] k, int limit) {
        int[] e1 = new int[4];
        int[] e2 = new int[4];
        int count = 0;
        Set<Long> hashes = new TreeSet<>();
        do {
            Assert.assertEquals(e1[0] + e1[2], e2[0] + e2[2]);
            Assert.assertEquals(e1[1] + e1[3], e2[1] + e2[3]);
            for (int i = 0; i < k.length; i++) {
                Assert.assertTrue(String.format("bad value for e1[%d]: %d", i, e1[i]), e1[i] <= k[i] && e1[i] >= 0);
                Assert.assertTrue(String.format("bad value for e2[%d]: %d", i, e2[i]), e2[i] <= k[i] && e2[i] >= 0);
                Assert.assertEquals(String.format("%d constraint", i), e1[i] + e1[i ^ 2], e2[i] + e2[i ^ 2]);
            }
            Long h = Arrays.hashCode(e1) + 10031L * Arrays.hashCode(e2);
            if (hashes.contains(h)) {
                System.out.print("duplicate\n");
            }
            hashes.add(h);
            count++;
        } while (Search.increment(e1, e2, k, k, limit));
        Assert.assertEquals(count, hashes.size());
        return count;
    }

    private int counterLoop(int[] k, int limit) {
        int[] e = new int[k.length];
        int cnt = 0;
        do {
            cnt++;
            double sum = 0;
            int i = 0;
            for (double v : e) {
                Assert.assertTrue(v <= k[i]);
                sum += v;
                i++;
            }
            Assert.assertTrue(sum <= limit);
        } while (Search.increment(e, k, limit));
        for (double v : e) {
            Assert.assertEquals(0, v, 0);
        }
        double prod = 1;
        for (double v : k) {
            prod *= v + 1;
        }
        Assert.assertTrue(cnt <= prod);
        return cnt;
    }

    @Test
    public void neighbors() {
        int[] dx = Search.repI(-1, 4);
        Set<Integer> found = new TreeSet<>();
        do {
            int v = 0;
            int zeros = 0;
            for (int x : dx) {
                v = 10 * v + x + 1;
                if (x == 0) {
                    zeros++;
                }
            }
            found.add(v);
            Assert.assertTrue(zeros < 4);
        } while (Search.neighbors(dx));
        Assert.assertEquals(3 * 3 * 3 * 3 - 1, found.size());
    }

    @Test
    public void exhaustive() {
        int[] base = new int[]{10, 20, 20, 10};
        PriorityQueue<Search.ScoredSolution> bottoms = Search.exhaustive(base, 20, 1);
        System.out.printf("%d", bottoms.size());
        while (bottoms.size() > 0) {
            Search.ScoredSolution bottom = bottoms.poll();
            System.out.printf("%s    %.2f\n", bottom, bottom.score);
        }
    }

    @Test
    public void greedy() {
        int[] b1 = new int[]{100, 20, 20, 100};
        int[] b2 = new int[]{80, 40, 40, 80};
        Search.HistoryMap history = new Search.HistoryMap();
        int[] s1 = Search.repI(0, 4);
        int[] s2 = Search.repI(0, 4);
        int limit = 22;
        int minCount = 10000;
        do {
            int remainder = limit - Search.l_0(s1, s2);
            if (remainder > 0 && history.getColor(s1, s2) == 0) {
                Search.treeSearch(b1, s1, b2, s2, remainder, 1, history, Search.searchMode.EXHAUSTIVE);
                if (history.markCount() > minCount) {
                    minCount *= 2;
                    System.out.printf("  >> %d <<\n", history.markCount());
                    Map<Integer, Search.HistoryMap.Basin> histogram = history.histogram();
                    for (Integer c : histogram.keySet()) {
                        Search.HistoryMap.Basin h = histogram.get(c);
                        System.out.printf("%2d %9d %.3f\n", c, h.count, h.value);
                    }
                    System.out.print("\n");
                }
            }
        } while (Search.increment(s1, s2, b1, b2, limit));
    }
}