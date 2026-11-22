package com.alex.mycelialmind.world;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Random;

/**
 * SimpleTiledModel Wave Function Collapse (mxgmn/WaveFunctionCollapse, MIT-style)
 * for generating deterministic modular mycelial cavern/outbreak floor layouts.
 */
public final class WfcCavernLayout {
    public enum Tile {
        STONE(0, 0, 0, 0),
        MYCELIUM_EDGE(0, 1, 1, 0),
        MYCELIUM_CORE(1, 1, 1, 1),
        VENT_PATCH(1, 1, 1, 1);

        final int n, e, s, w;

        Tile(int n, int e, int s, int w) {
            this.n = n;
            this.e = e;
            this.s = s;
            this.w = w;
        }
    }

    private WfcCavernLayout() {
    }

    public static Tile[][] generate(int width, int height, long seed) {
        int nTiles = Tile.values().length;
        int fullMask = (1 << nTiles) - 1;
        Tile[] all = Tile.values();
        Random rng = new Random(seed);

        int[] compatN = new int[nTiles];
        int[] compatE = new int[nTiles];
        int[] compatS = new int[nTiles];
        int[] compatW = new int[nTiles];
        for (int i = 0; i < nTiles; i++) {
            for (int j = 0; j < nTiles; j++) {
                if (all[i].n == all[j].s) compatN[i] |= (1 << j);
                if (all[i].e == all[j].w) compatE[i] |= (1 << j);
                if (all[i].s == all[j].n) compatS[i] |= (1 << j);
                if (all[i].w == all[j].e) compatW[i] |= (1 << j);
            }
        }

        int[][] wave = new int[height][width];
        for (int attempt = 0; attempt < 8; attempt++) {
            for (int y = 0; y < height; y++) {
                Arrays.fill(wave[y], fullMask);
            }
            boolean failed = false;
            while (true) {
                int bestX = -1;
                int bestY = -1;
                int bestCount = nTiles + 1;
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int c = Integer.bitCount(wave[y][x]);
                        if (c == 0) {
                            failed = true;
                            break;
                        }
                        if (c > 1 && c < bestCount) {
                            bestCount = c;
                            bestX = x;
                            bestY = y;
                        }
                    }
                    if (failed) break;
                }
                if (failed || bestX < 0) break;

                int mask = wave[bestY][bestX];
                int pickIdx = rng.nextInt(Integer.bitCount(mask));
                int chosenBit = 0;
                for (int t = 0; t < nTiles; t++) {
                    if ((mask & (1 << t)) != 0) {
                        if (pickIdx == 0) {
                            chosenBit = (1 << t);
                            break;
                        }
                        pickIdx--;
                    }
                }
                wave[bestY][bestX] = chosenBit;

                Deque<int[]> stack = new ArrayDeque<>();
                stack.push(new int[]{bestX, bestY});
                while (!stack.isEmpty() && !failed) {
                    int[] cur = stack.pop();
                    int cx = cur[0];
                    int cy = cur[1];
                    int curMask = wave[cy][cx];
                    int[][] dirs = {{0, -1, 0}, {1, 0, 1}, {0, 1, 2}, {-1, 0, 3}};
                    for (int[] d : dirs) {
                        int nx = cx + d[0];
                        int ny = cy + d[1];
                        if (nx < 0 || nx >= width || ny < 0 || ny >= height) continue;
                        int allowed = 0;
                        int[] table = switch (d[2]) {
                            case 0 -> compatN;
                            case 1 -> compatE;
                            case 2 -> compatS;
                            default -> compatW;
                        };
                        for (int t = 0; t < nTiles; t++) {
                            if ((curMask & (1 << t)) != 0) allowed |= table[t];
                        }
                        int before = wave[ny][nx];
                        int after = before & allowed;
                        if (after == 0) {
                            failed = true;
                            break;
                        }
                        if (after != before) {
                            wave[ny][nx] = after;
                            stack.push(new int[]{nx, ny});
                        }
                    }
                }
            }
            if (!failed) {
                Tile[][] out = new Tile[height][width];
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        out[y][x] = all[Integer.numberOfTrailingZeros(wave[y][x])];
                    }
                }
                return out;
            }
        }
        Tile[][] fallback = new Tile[height][width];
        for (int y = 0; y < height; y++) {
            Arrays.fill(fallback[y], Tile.MYCELIUM_CORE);
        }
        return fallback;
    }
}
