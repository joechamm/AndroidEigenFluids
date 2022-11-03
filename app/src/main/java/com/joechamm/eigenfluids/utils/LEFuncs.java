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

package com.joechamm.eigenfluids.utils;

import com.joechamm.eigenfluids.utils.SparseMatrix;

public class LEFuncs {

    public static final float VISCOSITY = 0.005f;
    public static final float DT = 0.1f;
    public static final float FORCE_MAG = 100.0f;
    public static final float DENSITY_VALUE = 1.0f;
    public static final float PDT_MULT = 1.0f;
    public static final float MARGIN = 1e-7f;
    public static int VEL_ROWS = 36;
    public static int VEL_COLS = 36;
    public static int DEN_ROWS = 36;
    public static int DEN_COLS = 36;
    public static float[][][][] VELOCITY_BASIS = null;
    public static float[][][] VELOCITY_FIELD = null;
    public static float[] COEFS = null;
    public static float[] EIGENVALUES = null;
    public static float[] INV_EIGENVALUES = null;
    public static float[] INVROOT_EIGENVALUES = null;
    public static SparseMatrix[] CK_MATRICES = null;
    public static int[][] BASIS_LOOKUP_TABLE = null;
    public static int[][] BASIS_RLOOKUP_TABLE = null;
    public static float[][] DENSITY_FIELD = null;
    public static float[] FORCES_DW = null;
    public static boolean FORCES_PENDING = false;
    public static int DIM_N = 0;
    public static int N_SQRT = 0;

    public LEFuncs () {
    }

    public static void init ( int vel_res, int dens_res, int ckDim ) {

        VEL_ROWS = vel_res;
        VEL_COLS = vel_res;
        DEN_ROWS = dens_res;
        DEN_COLS = dens_res;
        DIM_N = ckDim;
        N_SQRT = (int) ( ( Math.floor ( Math.sqrt ( DIM_N ) ) ) );

        VELOCITY_FIELD = new float[ 2 ][ VEL_COLS + 1 ][ VEL_ROWS + 1 ];
        DENSITY_FIELD = new float[ DEN_COLS ][ DEN_ROWS ];
        init_density ();

        COEFS = new float[ DIM_N ];
        FORCES_DW = new float[ DIM_N ];

        fill_lookup_table ();
        precompute_basis_fields ();
        precompute_dynamics ();

    }

    public static void init_density () {
        int midPt = DEN_COLS / 2;
        int qtrPt = midPt / 2;
        for ( int i = 0; i < DEN_COLS; i++ ) {
            for ( int j = 0; j < DEN_ROWS; j++ ) {
                int x = ( i - midPt ) * ( i - midPt );
                int y = ( j - midPt ) * ( j - midPt );

                if ( x + y < qtrPt * qtrPt ) {
                    DENSITY_FIELD[ i ][ j ] = 1.0f;
                } else {
                    DENSITY_FIELD[ i ][ j ] = 0.0f;
                }
            }
        }
    }

    public static void fill_lookup_table () {
        N_SQRT = (int) ( ( Math.floor ( Math.sqrt ( DIM_N ) ) ) );

        BASIS_LOOKUP_TABLE = new int[ DIM_N ][ 2 ];
        BASIS_RLOOKUP_TABLE = new int[ N_SQRT + 1 ][ N_SQRT + 1 ];

        for ( int k1 = 0; k1 <= N_SQRT; k1++ ) {
            for ( int k2 = 0; k2 <= N_SQRT; k2++ ) {
                BASIS_RLOOKUP_TABLE[ k1 ][ k2 ] = - 1;
            }
        }

        int index = 0;
        for ( int k1 = 0; k1 <= N_SQRT; k1++ ) {
            for ( int k2 = 0; k2 <= N_SQRT; k2++ ) {
                if ( k1 > N_SQRT || k1 < 1 || k2 > N_SQRT || k2 < 1 ) {
                    continue;
                }

                BASIS_LOOKUP_TABLE[ index ][ 0 ] = k1;
                BASIS_LOOKUP_TABLE[ index ][ 1 ] = k2;

                BASIS_RLOOKUP_TABLE[ k1 ][ k2 ] = index;
                index += 1;

            }
        }
    }

    public static void precompute_basis_fields () {
        VELOCITY_BASIS = new float[ DIM_N ][][][];

        for ( int i = 0; i < DIM_N; i++ ) {
            int k1 = basis_lookup ( i, 0 );
            int k2 = basis_lookup ( i, 1 );

            VELOCITY_BASIS[ i ] = basis_field_2d_rect ( k1, k2, 1.0f );
        }
    }

    public static void precompute_dynamics () {
        CK_MATRICES = new SparseMatrix[ DIM_N ];

        for ( int i = 0; i < DIM_N; i++ ) {
            CK_MATRICES[ i ] = new SparseMatrix ( DIM_N, DIM_N );
        }

        EIGENVALUES = new float[ DIM_N ];
        INV_EIGENVALUES = new float[ DIM_N ];
        INVROOT_EIGENVALUES = new float[ DIM_N ];

        for ( int i = 0; i < DIM_N; i++ ) {
            int k1 = basis_lookup ( i, 0 );
            int k2 = basis_lookup ( i, 1 );

            EIGENVALUES[ i ] = ( k1 * k1 + k2 * k2 );
            INV_EIGENVALUES[ i ] = 1.0f / (float) ( k1 * k1 + k2 * k2 );
            INVROOT_EIGENVALUES[ i ] = (float) ( 1.0f / Math.sqrt ( ( k1 * k1 + k2 * k2 ) ) );
        }

        for ( int d1 = 0; d1 < DIM_N; d1++ ) {
            int a1 = basis_lookup ( d1, 0 );
            int a2 = basis_lookup ( d1, 1 );
            float lambda_a = - ( a1 * a1 + a2 * a2 );
            for ( int d2 = 0; d2 < DIM_N; d2++ ) {
                int b1 = basis_lookup ( d2, 0 );
                int b2 = basis_lookup ( d2, 1 );

                float lambda_b = - ( b1 * b1 + b2 * b2 );
                float inv_lambda_b = - 1.0f / ( b1 * b1 + b2 * b2 );

                int k1 = basis_rlookup ( a1, a2 );
                int k2 = basis_rlookup ( b1, b2 );

                int[][] antipairs = new int[ 4 ][ 2 ];
                antipairs[ 0 ][ 0 ] = a1 - b1;
                antipairs[ 0 ][ 1 ] = a2 - b2;
                antipairs[ 1 ][ 0 ] = a1 - b1;
                antipairs[ 1 ][ 1 ] = a2 + b2;
                antipairs[ 2 ][ 0 ] = a1 + b1;
                antipairs[ 2 ][ 1 ] = a2 - b2;
                antipairs[ 3 ][ 0 ] = a1 + b1;
                antipairs[ 3 ][ 1 ] = a2 + b2;

                for ( int c = 0; c < 4; c++ ) {
                    int i = antipairs[ c ][ 0 ];
                    int j = antipairs[ c ][ 1 ];

                    int index = basis_rlookup ( i, j );

                    if ( index != - 1 ) {
                        float coef = - coefdensity ( a1, a2, b1, b2, c, 0 ) * inv_lambda_b;
                        CK_MATRICES[ index ].set ( k1, k2, coef );
                        CK_MATRICES[ index ].set ( k2, k1, - coef * ( lambda_b / lambda_a ) );
                    }
                }
            }
        }
    }

    public static int basis_lookup ( int index, int component ) {
        return BASIS_LOOKUP_TABLE[ index ][ component ];
    }

    public static float[][][] basis_field_2d_rect ( int n, int m, float amp ) {
        int a = n;
        int b = m;

        float xfact = 1.0f;
        if ( n != 0 ) {
            xfact = - 1.0f / ( a * a + b * b );
        }
        float yfact = 1.0f;
        if ( m != 0 ) {
            yfact = - 1.0f / ( a * a + b * b );
        }

        float[][][] vf = new float[ 2 ][ VEL_COLS + 1 ][ VEL_ROWS + 1 ];

        float deltax = 3.14159f / (float) VEL_COLS;
        float deltay = 3.14159f / (float) VEL_ROWS;

        for ( int i = 0; i < VEL_COLS + 1; i++ ) {
            for ( int j = 0; j < VEL_ROWS + 1; j++ ) {
                float x = (float) i * deltax;
                float y = (float) j * deltay;

                vf[ 0 ][ i ][ j ] = - (float) b * amp * xfact * (float) ( ( Math.sin ( a * x ) ) * Math.cos ( b * ( y + 0.5 * deltay ) ) );
                vf[ 1 ][ i ][ j ] = (float) a * amp * yfact * (float) ( ( Math.cos ( a * ( x + 0.5 * deltax ) ) * Math.sin ( b * y ) ) );
            }
        }
        return vf;
    }

    public static int basis_rlookup ( int k1, int k2 ) {
        if ( k1 > N_SQRT || k1 < 1 || k2 > N_SQRT || k2 < 1 ) {
            return - 1;
        }
        return BASIS_RLOOKUP_TABLE[ k1 ][ k2 ];
    }

    public static float coefdensity ( int a1, int b1, int a2, int b2, int c, int tt ) {
        if ( tt == 0 ) {
            // SS x SS
            if ( c == 0 )
                return 0.25f * - ( a1 * b2 - a2 * b1 ); // --
            if ( c == 1 )
                return 0.25f * ( a1 * b2 + a2 * b1 ); // -+
            if ( c == 2 )
                return 0.25f * - ( a1 * b2 + a2 * b1 ); // +-
            if ( c == 3 )
                return 0.25f * ( a1 * b2 - a2 * b1 ); // ++
        } else if ( tt == 1 ) {
            // SC x SS
            if ( c == 0 )
                return 0.25f * - ( a1 * b2 - a2 * b1 ); // --
            if ( c == 1 )
                return 0.25f * - ( a1 * b2 + a2 * b1 ); // -+
            if ( c == 2 )
                return 0.25f * ( a1 * b2 + a2 * b1 ); // +-
            if ( c == 3 )
                return 0.25f * ( a1 * b2 - a2 * b1 ); // ++
        } else if ( tt == 2 ) {
            // CS x SS
            if ( c == 0 )
                return 0.25f * - ( a1 * b2 - a2 * b1 ); // --
            if ( c == 1 )
                return 0.25f * - ( a1 * b2 + a2 * b1 ); // -+
            if ( c == 2 )
                return 0.25f * ( a1 * b2 + a2 * b1 ); // +-
            if ( c == 3 )
                return 0.25f * ( a1 * b2 - a2 * b1 ); // ++
        } else if ( tt == 3 ) {
            // CS x SS
            if ( c == 0 )
                return 0.25f * - ( a1 * b2 - a2 * b1 ); // --
            if ( c == 1 )
                return 0.25f * - ( a1 * b2 + a2 * b1 ); // -+
            if ( c == 2 )
                return 0.25f * ( a1 * b2 + a2 * b1 ); // +-
            if ( c == 3 )
                return 0.25f * ( a1 * b2 - a2 * b1 ); // ++
        }

        return 0;
    }

    public static void step () {
        float[] dw = new float[ DIM_N ];

        float prev_e = cur_energy ();

        float[][] dwt = new float[ 4 ][ DIM_N ];
        float[][] qn = new float[ 4 ][ DIM_N ];

        qn[ 0 ] = COEFS;

        for ( int k = 0; k < DIM_N; k++ ) {
            // calculate C_k matrix vector products
            dwt[ 0 ][ k ] = dot ( qn[ 0 ], CK_MATRICES[ k ].mult ( qn[ 0 ] ) );
            qn[ 1 ][ k ] = qn[ 0 ][ k ] + 0.5f * DT * dwt[ 0 ][ k ];
        }

        for ( int k = 0; k < DIM_N; k++ ) {

            dwt[ 1 ][ k ] = dot ( qn[ 1 ], CK_MATRICES[ k ].mult ( qn[ 1 ] ) );
            qn[ 2 ][ k ] = qn[ 0 ][ k ] + 0.5f * DT * dwt[ 1 ][ k ];
        }

        for ( int k = 0; k < DIM_N; k++ ) {

            dwt[ 2 ][ k ] = dot ( qn[ 2 ], CK_MATRICES[ k ].mult ( qn[ 2 ] ) );
            qn[ 3 ][ k ] = qn[ 0 ][ k ] + 0.5f * DT * dwt[ 2 ][ k ];
        }

        for ( int k = 0; k < DIM_N; k++ ) {

            dwt[ 3 ][ k ] = dot ( qn[ 3 ], CK_MATRICES[ k ].mult ( qn[ 3 ] ) );
            dw[ k ] = ( dwt[ 0 ][ k ] + 2.0f * dwt[ 1 ][ k ] + 2.0f * dwt[ 2 ][ k ] + dwt[ 3 ][ k ] ) / 6.0f;
        }

        for ( int k = 0; k < DIM_N; k++ ) {
            COEFS[ k ] += dw[ k ] * DT;
        }

        if ( prev_e > 1e-5f ) {
            set_energy ( prev_e );
        }

        for ( int k = 0; k < DIM_N; k++ ) {
            float tmp = - 1.0f * EIGENVALUES[ k ] * DT * VISCOSITY;
            float decay = (float) Math.exp ( tmp );
            COEFS[ k ] *= decay;
            COEFS[ k ] += FORCES_DW[ k ];
            FORCES_DW[ k ] = 0.0f;
        }

        expand_basis ();

        if ( DEN_COLS > 0 ) {
            advect_density ();
        }
    }

    public static float cur_energy () {
        float energy = 0.0f;
        for ( int i = 0; i < DIM_N; i++ ) {
            energy += ( INV_EIGENVALUES[ i ] * COEFS[ i ] * COEFS[ i ] );
        }
        return energy;
    }

    public static float dot ( float[] a, float[] b ) {
        float res = 0.0f;
        for ( int i = 0; i < a.length; i++ ) {
            res += a[ i ] * b[ i ];
        }
        return res;
    }

    public static void set_energy ( float desired_e ) {
        float cur_e = cur_energy ();
        float fact = (float) ( Math.sqrt ( desired_e ) / Math.sqrt ( cur_e ) );

        for ( int i = 0; i < DIM_N; i++ ) {
            COEFS[ i ] *= fact;
        }
    }

    public static void expand_basis () {
        VELOCITY_FIELD = new float[ 2 ][ VEL_COLS + 1 ][ VEL_ROWS + 1 ];

        for ( int k = 0; k < DIM_N; k++ ) {
            for ( int i = 0; i < VEL_COLS + 1; i++ ) {
                for ( int j = 0; j < VEL_ROWS + 1; j++ ) {
                    VELOCITY_FIELD[ 0 ][ i ][ j ] += COEFS[ k ] * VELOCITY_BASIS[ k ][ 0 ][ i ][ j ];
                    VELOCITY_FIELD[ 1 ][ i ][ j ] += COEFS[ k ] * VELOCITY_BASIS[ k ][ 1 ][ i ][ j ];
                }
            }
        }
    }

    public static void advect_density () {
        float[][] density_new = new float[ DEN_COLS ][ DEN_ROWS ];
        float pdt = DT * PDT_MULT;

        boolean RK2 = false;

        for ( int i = 0; i < DEN_COLS; i++ ) {
            for ( int j = 0; j < DEN_ROWS; j++ ) {
                float x = ( (float) i + 0.5f ) / DEN_COLS;
                float y = ( (float) j + 0.5f ) / DEN_ROWS;

                float nx = 0.0f;
                float ny = 0.0f;

                if ( RK2 ) {
                    float[] v0 = vel_at_bilinear ( x, y );
                    float[] v1 = vel_at_bilinear ( x - 0.666f * pdt * v0[ 0 ], y - 0.666f * pdt * v0[ 1 ] );

                    nx = x - pdt * ( v0[ 0 ] + 3.0f * v1[ 0 ] ) / 4.0f;
                    ny = y - pdt * ( v0[ 1 ] + 3.0f * v1[ 1 ] ) / 4.0f;
                } else {
                    float[] v = vel_at_bilinear ( x, y );

                    nx = x - pdt * v[ 0 ];
                    ny = y - pdt * v[ 1 ];
                }

                density_new[ i ][ j ] = density_at ( nx, ny );
            }
        }

        DENSITY_FIELD = density_new;
    }

    public static float[] vel_at_bilinear ( float xx, float yy ) {
        float[] v = new float[ 2 ];
        xx *= VEL_COLS;
        yy *= VEL_ROWS;

        v[ 0 ] = getInterpolatedValue ( xx, yy - 0.5f, 0 );
        v[ 1 ] = getInterpolatedValue ( xx - 0.5f, yy, 1 );

        return v;
    }

    public static float density_at ( float xxx, float yyy ) {
        float x = xxx * DEN_COLS;
        float y = yyy * DEN_ROWS;

        float xx = clampd ( x - 0.5f, 0.0f, (float) ( DEN_COLS - 1 ) );
        float yy = clampd ( y - 0.5f, 0.0f, (float) ( DEN_ROWS - 1 ) );

        int x1 = clampi ( (int) xx, 0, DEN_COLS - 1 );
        int x2 = clampi ( (int) xx + 1, 0, DEN_COLS - 1 );

        int y1 = clampi ( (int) yy, 0, DEN_ROWS - 1 );
        int y2 = clampi ( (int) yy + 1, 0, DEN_ROWS - 1 );

        float b1 = DENSITY_FIELD[ x1 ][ y1 ];
        float b2 = DENSITY_FIELD[ x2 ][ y1 ] - DENSITY_FIELD[ x1 ][ y1 ];
        float b3 = DENSITY_FIELD[ x1 ][ y2 ] - DENSITY_FIELD[ x1 ][ y1 ];
        float b4 = DENSITY_FIELD[ x1 ][ y1 ] - DENSITY_FIELD[ x2 ][ y1 ] -
                DENSITY_FIELD[ x1 ][ y2 ] + DENSITY_FIELD[ x2 ][ y2 ];

        float dx = xx - (float) x1;
        float dy = yy - (float) y1;

        float tot = b1 + b2 * dx + b3 * dy + b4 * dx * dy;
        return tot;
    }

    public static float getInterpolatedValue ( float x, float y, int index ) {
        int i = (int) Math.floor ( x );
        int j = (int) Math.floor ( y );

        float tot = 0.0f;
        int den = 0;

        if ( i >= 0 && i <= VEL_COLS && j >= 0 && j <= VEL_ROWS ) {
            tot += ( i + 1 - x ) * ( j + 1 - y ) * VELOCITY_FIELD[ index ][ i ][ j ];
            den++;
        }

        if ( i + 1 >= 0 && i + 1 <= VEL_COLS && j >= 0 && j <= VEL_ROWS ) {
            tot += ( x - i ) * ( j + 1 - y ) * VELOCITY_FIELD[ index ][ i + 1 ][ j ];
            den++;
        }

        if ( i >= 0 && i <= VEL_COLS && j + 1 >= 0 && j + 1 <= VEL_ROWS ) {
            tot += ( i + 1 - x ) * ( y - j ) * VELOCITY_FIELD[ index ][ i ][ j + 1 ];
            den++;
        }

        if ( i + 1 >= 0 && i + 1 <= VEL_COLS && j + 1 >= 0 && j + 1 <= VEL_ROWS ) {
            tot += ( x - i ) * ( y - j ) * VELOCITY_FIELD[ index ][ i + 1 ][ j + 1 ];
            den++;
        }

        if ( den == 0 ) {
            return 0.0f;
        }

        tot = tot / (float) den;

        return tot;
    }

    public static float clampd ( float f, float a, float b ) {
        if ( f < a ) {
            return a;
        }
        if ( f > b ) {
            return b;
        }
        return f;
    }

    public static int clampi ( int f, int a, int b ) {
        if ( f < a ) {
            return a;
        }
        if ( f > b ) {
            return b;
        }
        return f;
    }

    public static void stir ( float[][] force_path ) {
        float[] dw = project_forces ( force_path );
        for ( int i = 0; i < DIM_N; i++ ) {
            FORCES_DW[ i ] += dw[ i ];
        }
    }

    public static float[] project_forces ( float[][] force_path ) {
        float[] dw = new float[ DIM_N ];

        for ( int i = 0; i < DIM_N; i++ ) {
            float tot = 0.0f;

            int a = basis_lookup ( i, 0 );
            int b = basis_lookup ( i, 1 );

            float xfact = 1.0f;
            if ( a != 0 ) {
                xfact = - 1.0f / ( a * a + b * b );
            }
            float yfact = 1.0f;
            if ( b != 0 ) {
                yfact = - 1.0f / ( a * a + b * b );
            }

            for ( int j = 0; j < force_path.length - 1; j++ ) {
                float x = force_path[ j ][ 0 ];
                float y = force_path[ j ][ 1 ];
                float fx = force_path[ j ][ 2 ];
                float fy = force_path[ j ][ 3 ];

                if ( x >= 1.00001f || x <= - 0.00001f || y >= 1.00001f || y <= - 0.00001f ) {
                    continue;
                }

                x *= 3.14159f;
                y *= 3.14159f;

                float vx = - (float) b * DT * xfact * (float) ( ( Math.sin ( a * x ) * Math.cos ( b * y ) ) );
                float vy = (float) a * DT * yfact * (float) ( ( Math.cos ( a * x ) * Math.sin ( b * y ) ) );

                tot += ( vx * fx + vy * fy );
            }
            dw[ i ] = tot;
        }
        return dw;
    }

    public static float[] vel_at_cubic ( float xx, float yy ) {
        float[] v = new float[ 2 ];
        float[] f = new float[ 4 ];

        float tk;
        xx *= VEL_COLS;
        yy *= VEL_ROWS;

        int k = 1;

        int[] x = new int[ 4 ];
        x[ k ] = clampi ( (int) Math.floor ( xx ), 0, VEL_COLS );
        x[ k + 1 ] = clampi ( x[ k ] + 1, 0, VEL_COLS );
        x[ k + 2 ] = clampi ( x[ k ] + 2, 0, VEL_COLS );
        x[ k - 1 ] = clampi ( x[ k ] - 1, 0, VEL_COLS );

        int[] y = new int[ 4 ];
        y[ k ] = clampi ( (int) Math.floor ( yy ), 0, VEL_ROWS );
        y[ k + 1 ] = clampi ( y[ k ] + 1, 0, VEL_ROWS );
        y[ k + 2 ] = clampi ( y[ k ] + 2, 0, VEL_ROWS );
        y[ k - 1 ] = clampi ( y[ k ] - 1, 0, VEL_ROWS );

        f[ k - 1 ] = VELOCITY_FIELD[ 0 ][ x[ k - 1 ] ][ y[ k ] ];
        f[ k ] = VELOCITY_FIELD[ 0 ][ x[ k ] ][ y[ k ] ];
        f[ k + 1 ] = VELOCITY_FIELD[ 0 ][ x[ k + 1 ] ][ y[ k ] ];
        f[ k + 2 ] = VELOCITY_FIELD[ 0 ][ x[ k + 2 ] ][ y[ k ] ];

        tk = xx - x[ k ];

        v[ 0 ] = f[ k - 1 ] * ( - 0.5f * tk + tk * tk - 0.5f * tk * tk * tk ) + f[ k ] *
                ( 1.0f - ( 5.0f / 2.0f ) * tk * tk + ( 3.0f / 2.0f ) * tk * tk * tk ) + f[ k + 1 ] *
                ( 0.5f * tk + 2.0f * tk * tk - ( 3.0f / 2.0f ) * tk * tk * tk ) + f[ k + 2 ] *
                ( - 0.5f * tk * tk + 0.5f * tk * tk * tk );

        f[ k - 1 ] = VELOCITY_FIELD[ 1 ][ x[ k ] ][ y[ k - 1 ] ];
        f[ k ] = VELOCITY_FIELD[ 1 ][ x[ k ] ][ y[ k ] ];
        f[ k + 1 ] = VELOCITY_FIELD[ 1 ][ x[ k ] ][ y[ k + 1 ] ];
        f[ k + 2 ] = VELOCITY_FIELD[ 1 ][ x[ k ] ][ y[ k + 2 ] ];

        tk = yy - y[ k ];

        v[ 1 ] = f[ k - 1 ] * ( - 0.5f * tk + tk * tk - 0.5f * tk * tk * tk ) + f[ k ] *
                ( 1.0f - ( 5.0f / 2.0f ) * tk * tk + ( 3.0f / 2.0f ) * tk * tk * tk ) + f[ k + 1 ] *
                ( 0.5f * tk + 2.0f * tk * tk - ( 3.0f / 2.0f ) * tk * tk * tk ) + f[ k + 2 ] *
                ( - 0.5f * tk * tk + 0.5f * tk * tk * tk );

        return v;
    }
}
