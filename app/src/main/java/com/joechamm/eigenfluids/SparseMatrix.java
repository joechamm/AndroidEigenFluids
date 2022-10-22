/*
 * MIT License
 *
 * Copyright (c) 2022 Joseph Cunningham
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.joechamm.eigenfluids;

public class SparseMatrix {

    public SparseMatrix(int colCount, int rowCount) {
        mIRowCount = rowCount;
        mIColumnCount = colCount;
        mFValues = new double[mIRowCount][];
        mIColumnIndices = new int[mIRowCount][];
        mICounters = new int[mIRowCount];
    }

    public static void main(String[] argv) {
        SparseMatrix a = new SparseMatrix(4, 4);
        double[] v = new double[4];
        a.set(3, 0, 1.0);
        a.set(3, 1, -2.0);
        a.set(3, 2, 3.0);
        a.set(3, 3, -4.0);

        v[0] = 10.0;
        v[1] = 10.0;
        v[2] = 10.0;
        v[3] = 10.0;

        a.dump();

        double[] x = a.mult(v);
        for (int i = 0; i < 4; i++) {
            System.out.printf("%f\n", x[i]);
        }

    }

    public double get(int col, int row) {
        if (mIColumnIndices[row] == null) {
            return 0.0;
        }
        int columnIndex = binarySearch(mIColumnIndices[row], 0,mICounters[row] - 1, col);

        if (columnIndex < 0) {
            return 0.0;
        }

        return mFValues[row][columnIndex];
    }

    private static int binarySearch(int[] array, int startIndex, int endIndex,
                                    int value) {
        if (value < array[startIndex]) {
            return (-startIndex - 1);
        }

        if (value > array[endIndex]) {
            return (-(endIndex + 1) - 1);
        }

        if (startIndex == endIndex) {
            if (array[startIndex] == value) {
                return startIndex;
            } else {
                return (-(startIndex + 1) - 1);
            }
        }

        int midIndex = (startIndex + endIndex) / 2;
        if (value == array[midIndex]) {
            return midIndex;
        }

        if (value < array[midIndex]) {
            return binarySearch(array, startIndex, midIndex - 1, value);
        } else {
            return binarySearch(array, midIndex + 1, endIndex, value);
        }

    }

    public void set(int col, int row, double value) {
        if (mIColumnIndices[row] == null) {
            mIColumnIndices[row] = new int[2];
            mFValues[row] = new double[2];
            mIColumnIndices[row][0] = col;
            mFValues[row][0] = value;
            mICounters[row] = 1;
            return;
        }

        int colIdx = binarySearch(mIColumnIndices[row], 0, mICounters[row] - 1,
                col);

        if (colIdx >= 0) {
            mFValues[row][colIdx] = value;
            return;
        } else {
            int insertionPoint = -(colIdx + 1);
            int oldLength = mICounters[row];
            int newLength = oldLength + 1;
            if (newLength <= mIColumnIndices[row].length) {
                if (insertionPoint != oldLength) {
                    for (int i = oldLength; i > insertionPoint; i--) {
                        mFValues[row][i] = mFValues[row][i - 1];
                        mIColumnIndices[row][i] = mIColumnIndices[row][i - 1];
                    }
                }

                mIColumnIndices[row][insertionPoint] = col;
                mFValues[row][insertionPoint] = value;
                mICounters[row]++;
                return;
            }

            int[] newColIndices = new int[2 * oldLength];
            double[] newValues = new double[2 * oldLength];

            if (insertionPoint == oldLength) {
                System.arraycopy(mIColumnIndices[row], 0, newColIndices, 0,
                        oldLength);
                System.arraycopy(mFValues[row], 0, newValues, 0, oldLength);
            } else {
                System.arraycopy(mIColumnIndices[0], 0, newColIndices, 0,
                        insertionPoint);
                System.arraycopy(mFValues[row], 0, newValues, 0, insertionPoint);
                System.arraycopy(mIColumnIndices[row], insertionPoint,
                        newColIndices, insertionPoint + 1, oldLength
                                - insertionPoint);
                System.arraycopy(mFValues[row], insertionPoint, newValues,
                        insertionPoint + 1, oldLength - insertionPoint);
            }

            newColIndices[insertionPoint] = col;
            newValues[insertionPoint] = value;
            mIColumnIndices[row] = null;
            mIColumnIndices[row] = newColIndices;
            mFValues[row] = null;
            mFValues[row] = newValues;
            mICounters[row]++;
        }
    }

    public void dump() {
        System.out.println("MATRIX " + mIRowCount + "*" + mIColumnCount);
        for (int row = 0; row < mIRowCount; row++) {
            int[] columnIndices = mIColumnIndices[row];
            if (columnIndices == null) {
                for (int col = 0; col < mIColumnCount; col++) {
                    System.out.print("0.0 ");
                }
            } else {
                int prevColumnIndex = 0;
                for (int colIndex = 0; colIndex < mICounters[row]; colIndex++) {
                    int currColumnIndex = columnIndices[colIndex];
                    for (int col = prevColumnIndex; col < currColumnIndex; col++) {
                        System.out.print("0.0 ");
                    }

                    System.out.print(mFValues[row][colIndex] + " ");
                    prevColumnIndex = currColumnIndex + 1;
                }

                for (int col = prevColumnIndex; col < mIColumnCount; col++) {
                    System.out.print("0.0 ");

                }
            }

            System.out.println();
        }
    }

    public void dumpInt() {
        System.out.println("MATRIX " + mIRowCount + "*" + mIColumnCount);
        for (int row = 0; row < mIRowCount; row++) {
            int[] columnIndices = mIColumnIndices[row];
            if (columnIndices == null) {
                for (int col = 0; col < mIColumnCount; col++) {
                    System.out.print("0 ");
                }
            } else {
                int prevColumnIndex = 0;
                for (int colIndex = 0; colIndex < mICounters[row]; colIndex++) {
                    int currColumnIndex = columnIndices[colIndex];

                    for (int col = prevColumnIndex; col < currColumnIndex; col++) {
                        System.out.print("0 ");
                    }

                    System.out.print((int) mFValues[row][colIndex] + " ");
                    prevColumnIndex = currColumnIndex + 1;
                }

                for (int col = prevColumnIndex; col < mIColumnCount; col++) {
                    System.out.print("0 ");
                }
            }

            System.out.println();
        }
    }

    public void addEmptyColumns(int columns) {
        mIColumnCount += columns;
    }

    public double[] mult(double[] vector) {
        if (mIColumnCount != vector.length) {
            return null;
        }

        int n = mIRowCount;
        double[] result = new double[n];
        for (int row = 0; row < n; row++) {
            double sum = 0.0;

            int[] nzIndexes = mIColumnIndices[row];
            int nzLength = mICounters[row];
            if (nzLength == 0) {
                continue;
            }

            for (int colIndex = 0; colIndex < nzLength; colIndex++) {
                double c = vector[nzIndexes[colIndex]];

                sum += (mFValues[row][colIndex] * c);
            }

            result[row] = sum;
        }

        return result;
    }

    public double getSum(int row) {
        double sum = 0.0;

        int nzLength = mICounters[row];

        if (nzLength == 0) {
            return 0.0;
        }

        for (int colIndex = 0; colIndex < nzLength; colIndex++) {
            sum += mFValues[row][colIndex];
        }

        return sum;
    }

    public double getSum(int row, boolean[] toConsider) {
        double sum = 0.0;

        int nzLength = mICounters[row];

        if (nzLength == 0) {
            return 0.0;
        }

        for (int colIndex = 0; colIndex < nzLength; colIndex++) {
            if (toConsider[mIColumnIndices[row][colIndex]]) {
                sum += mFValues[row][colIndex];
            }
        }

        return sum;
    }

    public int getNzCount() {
        int allNz = 0;
        for (int i : mICounters) {
            allNz += i;
        }

        return allNz;
    }

    public void normalize() {
        double minValue = 0.0;
        double maxValue = 0.0;
        boolean isFirst = true;
        for(int i = 0; i < mIRowCount; i++) {
            if(mICounters[i] == 0) {
                continue;
            }

            for(int j = 0; j < mICounters[i]; j++) {
                double val = mFValues[i][j];

                if(isFirst) {
                    minValue = val;
                    maxValue = val;
                    isFirst = false;
                } else {
                    if(val < minValue) {
                        minValue = val;
                    }
                    if(val > maxValue) {
                        maxValue = val;
                    }
                }
            }
        }

        for(int i = 0; i < mIRowCount; i++) {
            if(mICounters[i] == 0) {
                continue;
            }

            for(int j = 0; j < mICounters[i]; j++) {
                mFValues[i][j] /= maxValue;
            }
        }
    }

    protected double[][] mFValues;
    protected int[][] mIColumnIndices;
    protected int[] mICounters;
    protected int mIColumnCount;
    protected int mIRowCount;
}
