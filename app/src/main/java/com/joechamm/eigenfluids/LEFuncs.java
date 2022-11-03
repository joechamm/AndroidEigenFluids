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

public class LEFuncs {

    public int mx;
    public int my;

    public int dmx;
    public int dmy;

    public float[][][][] vel_basis;
    public float[] coef;
    public float[][][] vfield;
    public float[] eigs;
    public float[] eigs_inv;
    public float[] eigs_inv_root;

    public SparseMatrix[] Ck;

    public float visc = 0.0f;
    public float dt = 0.1f;
    public float pdt_mult = 1.0f;
    public float margin = 1e-7f;
    public int N;
    public int N_sqrt;

    public int[][] basis_lookup_table;
    public int[][] basis_rlookup_table;

    public float[] forces_dw;
    public boolean forces_pending;

    public float[][] density_field;

    LEFuncs ( int grid_res, int N, int dens_grid_res ) {
        this.mx = grid_res;
        this.my = grid_res;

        this.dmx = dens_grid_res;
        this.dmy = dens_grid_res;

        this.N = N;

        this.vfield = new float[ 2 ][ this.mx + 1 ][ this.my + 1 ];
        this.density_field = new float[ this.dmx ][ this.dmy ];

        this.coef = new float[ N ];
        this.forces_dw = new float[ N ];

        this.fill_lookup_table ();
        this.precompute_basis_fields ();
        this.precompute_dynamics ();

        init_density ();
    }

    public void step () {
        float[] dw = new float[ this.N ];

        float prev_e = cur_energy ();

        float[][] dwt = new float[ 4 ][ this.N ];
        float[][] qn = new float[ 4 ][ this.N ];

        qn[ 0 ] = this.coef;

        for ( int k = 0; k < this.N; k++ ) {
            // calculate C_k matrix vector products
            dwt[ 0 ][ k ] = this.dot ( qn[ 0 ], this.Ck[ k ].mult ( qn[ 0 ] ) );
            qn[ 1 ][ k ] = qn[ 0 ][ k ] + 0.5f * this.dt * dwt[ 0 ][ k ];
        }

        for ( int k = 0; k < this.N; k++ ) {

            dwt[ 1 ][ k ] = this.dot ( qn[ 1 ], this.Ck[ k ].mult ( qn[ 1 ] ) );
            qn[ 2 ][ k ] = qn[ 0 ][ k ] + 0.5f * this.dt * dwt[ 1 ][ k ];
        }

        for ( int k = 0; k < this.N; k++ ) {

            dwt[ 2 ][ k ] = this.dot ( qn[ 2 ], this.Ck[ k ].mult ( qn[ 2 ] ) );
            qn[ 3 ][ k ] = qn[ 0 ][ k ] + 0.5f * this.dt * dwt[ 2 ][ k ];
        }

        for ( int k = 0; k < this.N; k++ ) {

            dwt[ 3 ][ k ] = this.dot ( qn[ 3 ], this.Ck[ k ].mult ( qn[ 3 ] ) );
            dw[ k ] = ( dwt[ 0 ][ k ] + 2.0f * dwt[ 1 ][ k ] + 2.0f * dwt[ 2 ][ k ] + dwt[ 3 ][ k ] ) / 6.0f;
        }

        for ( int k = 0; k < this.N; k++ ) {
            this.coef[ k ] += dw[ k ] * this.dt;
        }

        if ( prev_e > 1e-5f ) {
            this.set_energy ( prev_e );
        }

        for ( int k = 0; k < this.N; k++ ) {
            float tmp = - 1.0f * this.eigs[ k ] * this.dt * this.visc;
            float decay = (float) java.lang.Math.exp ( tmp );
            coef[ k ] *= decay;
            coef[ k ] += this.forces_dw[ k ];
            forces_dw[ k ] = 0;
        }

        this.expand_basis ();

        if ( this.dmx > 0 ) {
            this.advect_density ();
        }
    }


    public void precompute_basis_fields () {
        this.vel_basis = new float[ this.N ][][][];

        for ( int i = 0; i < this.N; i++ ) {
            int k1 = this.basis_lookup ( i, 0 );
            int k2 = this.basis_lookup ( i, 1 );

            this.vel_basis[ i ] = this.basis_field_2d_rect ( k1, k2, 1.0f );
        }
    }

    public void precompute_dynamics () {
        this.Ck = new SparseMatrix[ N ];

        for ( int i = 0; i < N; i++ ) {
            this.Ck[ i ] = new SparseMatrix ( N, N );
        }

        this.eigs = new float[ N ];
        this.eigs_inv = new float[ N ];
        this.eigs_inv_root = new float[ N ];

        for ( int i = 0; i < N; i++ ) {
            int k1 = this.basis_lookup ( i, 0 );
            int k2 = this.basis_lookup ( i, 1 );
            this.eigs[ i ] = ( k1 * k1 + k2 * k2 );
            this.eigs_inv[ i ] = 1.0f / ( k1 * k1 + k2 * k2 );
            this.eigs_inv_root[ i ] = 1.0f / (float) ( java.lang.Math.sqrt ( k1 * k1 + k2 * k2 ) );
        }

        for ( int d1 = 0; d1 < N; d1++ ) {
            int a1 = this.basis_lookup ( d1, 0 );
            int a2 = this.basis_lookup ( d1, 1 );
            float lambda_a = - ( a1 * a1 + a2 * a2 );
            for ( int d2 = 0; d2 < N; d2++ ) {
                int b1 = this.basis_lookup ( d2, 0 );
                int b2 = this.basis_lookup ( d2, 1 );

                float lambda_b = - ( b1 * b1 + b2 * b2 );
                float inv_lambda_b = - 1.0f / ( b1 * b1 + b2 * b2 );

                int k1 = this.basis_rlookup ( a1, a2 );
                int k2 = this.basis_rlookup ( b1, b2 );

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

                    int index = this.basis_rlookup ( i, j );

                    if ( index != - 1 ) {
                        float coef = - this.coefdensity ( a1, a2, b1, b2, c, 0 ) * inv_lambda_b;
                        this.Ck[ index ].set ( k1, k2, coef );
                        this.Ck[ index ].set ( k2, k1, coef * - lambda_b / lambda_a );
                    }
                }
            }
        }
    }

    public float coefdensity ( int a1, int b1, int a2, int b2, int c, int tt ) {
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

    public float[][][] basis_field_2d_rect ( int n, int m, float amp ) {
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

        float[][][] vf = new float[ 2 ][ this.mx + 1 ][ this.my + 1 ];

        float deltax = 3.14159f / this.mx;
        float deltay = 3.14159f / this.my;

        for ( int i = 0; i < this.mx + 1; i++ ) {
            for ( int j = 0; j < this.my + 1; j++ ) {
                float x = (float) i * deltax;
                float y = (float) j * deltay;

                vf[ 0 ][ i ][ j ] = - (float) b * amp * xfact * (float) ( ( java.lang.Math.sin ( a * x ) ) * java.lang.Math.cos (
                        b * ( y + 0.5 * deltay ) ) );
                vf[ 1 ][ i ][ j ] = (float) a * amp * yfact * (float) ( ( java.lang.Math.cos (
                        a * ( x + 0.5 * deltax ) ) * java.lang.Math.sin ( b * y ) ) );
            }
        }
        return vf;
    }

    public float cur_energy () {
        float energy = 0.0f;
        for ( int i = 0; i < this.N; i++ ) {
            energy += ( this.eigs_inv[ i ] * ( this.coef[ i ] * this.coef[ i ] ) );
        }
        return energy;

    }

    public void set_energy ( float desired_e ) {
        float cur_e = this.cur_energy ();
        float fact = (float) ( java.lang.Math.sqrt ( desired_e ) / java.lang.Math.sqrt ( cur_e ) );

        for ( int i = 0; i < this.N; i++ ) {
            this.coef[ i ] *= fact;
        }
    }

    public float getInterpolatedValue ( float x, float y, int index ) {
        int i = (int) java.lang.Math.floor ( x );
        int j = (int) java.lang.Math.floor ( y );

        float tot = 0.0f;
        int den = 0;

        if ( i >= 0 && i <= mx && j >= 0 && j <= my ) {
            tot += ( i + 1 - x ) * ( j + 1 - y ) * this.vfield[ index ][ i ][ j ];
            den++;
        }

        if ( i + 1 >= 0 && i + 1 <= mx && j >= 0 && j <= my ) {
            tot += ( x - i ) * ( j + 1 - y ) * this.vfield[ index ][ i + 1 ][ j ];
            den++;
        }

        if ( i >= 0 && i <= mx && j + 1 >= 0 && j + 1 <= my ) {
            tot += ( i + 1 - x ) * ( y - j ) * this.vfield[ index ][ i ][ j + 1 ];
            den++;
        }

        if ( i + 1 >= 0 && i + 1 <= mx && j + 1 >= 0 && j + 1 <= my ) {
            tot += ( x - i ) * ( y - j ) * this.vfield[ index ][ i + 1 ][ j + 1 ];
            den++;
        }

        if ( den == 0 ) {
            return 0.0f;
        }

        tot = tot / (float) den;

        return tot;
    }

    public float[] vel_at_bilinear ( float xx, float yy ) {
        float[] v = new float[ 2 ];
        xx *= this.mx;
        yy *= this.my;

        v[ 0 ] = getInterpolatedValue ( xx, yy - 0.5f, 0 );
        v[ 1 ] = getInterpolatedValue ( xx - 0.5f, yy, 1 );

        return v;
    }

    public float[] vel_at_cubic ( float xx, float yy ) {
        float[] v = new float[ 2 ];
        float[] f = new float[ 4 ];

        float tk;
        xx *= this.mx;
        yy *= this.my;

        int k = 1;

        int[] x = new int[ 4 ];
        x[ k ] = clampi ( (int) java.lang.Math.floor ( xx ), 0, mx );
        x[ k + 1 ] = clampi ( x[ k ] + 1, 0, mx );
        x[ k + 2 ] = clampi ( x[ k ] + 2, 0, mx );
        x[ k - 1 ] = clampi ( x[ k ] - 1, 0, mx );

        int[] y = new int[ 4 ];
        y[ k ] = clampi ( (int) java.lang.Math.floor ( yy ), 0, my );
        y[ k + 1 ] = clampi ( y[ k ] + 1, 0, my );
        y[ k + 2 ] = clampi ( y[ k ] + 2, 0, my );
        y[ k - 1 ] = clampi ( y[ k ] - 1, 0, my );

        f[ k - 1 ] = this.vfield[ 0 ][ x[ k - 1 ] ][ y[ k ] ];
        f[ k ] = this.vfield[ 0 ][ x[ k ] ][ y[ k ] ];
        f[ k + 1 ] = this.vfield[ 0 ][ x[ k + 1 ] ][ y[ k ] ];
        f[ k + 2 ] = this.vfield[ 0 ][ x[ k + 2 ] ][ y[ k ] ];

        tk = xx - x[ k ];

        v[ 0 ] = f[ k - 1 ] * ( - 0.5f * tk + tk * tk - 0.5f * tk * tk * tk ) + f[ k ] *
                ( 1.0f - ( 5.0f / 2.0f ) * tk * tk + ( 3.0f / 2.0f ) * tk * tk * tk ) + f[ k + 1 ] *
                ( 0.5f * tk + 2.0f * tk * tk - ( 3.0f / 2.0f ) * tk * tk * tk ) + f[ k + 2 ] *
                ( - 0.5f * tk * tk + 0.5f * tk * tk * tk );

        f[ k - 1 ] = this.vfield[ 1 ][ x[ k ] ][ y[ k - 1 ] ];
        f[ k ] = this.vfield[ 1 ][ x[ k ] ][ y[ k ] ];
        f[ k + 1 ] = this.vfield[ 1 ][ x[ k ] ][ y[ k + 1 ] ];
        f[ k + 2 ] = this.vfield[ 1 ][ x[ k ] ][ y[ k + 2 ] ];

        tk = yy - y[ k ];

        v[ 1 ] = f[ k - 1 ] * ( - 0.5f * tk + tk * tk - 0.5f * tk * tk * tk ) + f[ k ] *
                ( 1.0f - ( 5.0f / 2.0f ) * tk * tk + ( 3.0f / 2.0f ) * tk * tk * tk ) + f[ k + 1 ] *
                ( 0.5f * tk + 2.0f * tk * tk - ( 3.0f / 2.0f ) * tk * tk * tk ) + f[ k + 2 ] *
                ( - 0.5f * tk * tk + 0.5f * tk * tk * tk );

        return v;
    }

    public int clampi ( int f, int a, int b ) {
        if ( f < a ) {
            return a;
        }
        if ( f > b ) {
            return b;
        }
        return f;
    }

    public float clampd ( float f, float a, float b ) {
        if ( f < a ) {
            return a;
        }
        if ( f > b ) {
            return b;
        }
        return f;
    }

    public void init_density () {
        int midPt = this.dmx / 2;
        int qtrPt = midPt / 2;
        for ( int i = 0; i < this.dmx; i++ ) {
            for ( int j = 0; j < this.dmy; j++ ) {
                int x = ( i - midPt ) * ( i - midPt );
                int y = ( j - midPt ) * ( j - midPt );

                if ( x + y < qtrPt * qtrPt ) {
                    this.density_field[ i ][ j ] = 1.0f;
                } else {
                    this.density_field[ i ][ j ] = 0.0f;
                }
            }
        }
    }

    public float density_at ( float xxx, float yyy ) {
        float x = xxx * this.dmx;
        float y = yyy * this.dmy;

        float xx = clampd ( x - 0.5f, 0.0f, (float) ( dmx - 1 ) );
        float yy = clampd ( y - 0.5f, 0.0f, (float) ( dmy - 1 ) );

        int x1 = clampi ( (int) xx, 0, dmx - 1 );
        int x2 = clampi ( (int) xx + 1, 0, dmx - 1 );

        int y1 = clampi ( (int) yy, 0, dmy - 1 );
        int y2 = clampi ( (int) yy + 1, 0, dmy - 1 );

        float b1 = this.density_field[ x1 ][ y1 ];
        float b2 = this.density_field[ x2 ][ y1 ] - this.density_field[ x1 ][ y1 ];
        float b3 = this.density_field[ x1 ][ y2 ] - this.density_field[ x1 ][ y1 ];
        float b4 = this.density_field[ x1 ][ y1 ] - this.density_field[ x2 ][ y1 ] -
                this.density_field[ x1 ][ y2 ] + this.density_field[ x2 ][ y2 ];

        float dx = xx - (float) x1;
        float dy = yy - (float) y1;

        float tot = b1 + b2 * dx + b3 * dy + b4 * dx * dy;
        return tot;
    }

    public void advect_density () {
        float[][] density_new = new float[ this.dmx ][ this.dmy ];
        float pdt = this.dt * this.pdt_mult;

        boolean RK2 = false;

        for ( int i = 0; i < this.dmx; i++ ) {
            for ( int j = 0; j < this.dmy; j++ ) {
                float x = ( (float) i + 0.5f ) / this.dmx;
                float y = ( (float) j + 0.5f ) / this.dmy;

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

        this.density_field = density_new;
    }

    public int basis_lookup ( int index, int component ) {
        return this.basis_lookup_table[ index ][ component ];
    }

    public int basis_rlookup ( int k1, int k2 ) {
        if ( k1 > this.N_sqrt || k1 < 1 || k2 > this.N_sqrt || k2 < 1 ) {
            return - 1;
        }
        return this.basis_rlookup_table[ k1 ][ k2 ];
    }

    public void expand_basis () {
        this.vfield = new float[ 2 ][ mx + 1 ][ my + 1 ];

        for ( int k = 0; k < this.N; k++ ) {
            for ( int i = 0; i < this.mx + 1; i++ ) {
                for ( int j = 0; j < this.my + 1; j++ ) {
                    this.vfield[ 0 ][ i ][ j ] += this.coef[ k ] * this.vel_basis[ k ][ 0 ][ i ][ j ];
                    this.vfield[ 1 ][ i ][ j ] += this.coef[ k ] * this.vel_basis[ k ][ 1 ][ i ][ j ];
                }
            }
        }
    }

    public void fill_lookup_table () {
        this.N_sqrt = (int) java.lang.Math.floor ( java.lang.Math.sqrt ( (float) N ) );

        this.basis_lookup_table = new int[ this.N ][ 2 ];
        this.basis_rlookup_table = new int[ this.N_sqrt + 1 ][ this.N_sqrt + 1 ];

        for ( int k1 = 0; k1 <= this.N_sqrt; k1++ ) {
            for ( int k2 = 0; k2 <= this.N_sqrt; k2++ ) {
                this.basis_rlookup_table[ k1 ][ k2 ] = - 1;
            }
        }

        int index = 0;
        for ( int k1 = 0; k1 <= this.N_sqrt; k1++ ) {
            for ( int k2 = 0; k2 <= this.N_sqrt; k2++ ) {
                if ( k1 > this.N_sqrt || k1 < 1 || k2 > this.N_sqrt || k2 < 1 ) {
                    continue;
                }

                this.basis_lookup_table[ index ][ 0 ] = k1;
                this.basis_lookup_table[ index ][ 1 ] = k2;

                this.basis_rlookup_table[ k1 ][ k2 ] = index;
                index += 1;

            }
        }
    }


    public float dot ( float[] a, float[] b ) {
        float res = 0.0f;
        for ( int i = 0; i < a.length; i++ ) {
            res += a[ i ] * b[ i ];
        }
        return res;
    }

    public float[] project_forces ( float[][] force_path ) {
        float[] dw = new float[ this.N ];

        for ( int i = 0; i < this.N; i++ ) {
            float tot = 0.0f;

            int a = this.basis_lookup ( i, 0 );
            int b = this.basis_lookup ( i, 1 );

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

                float vx = - (float) b * this.dt * xfact * (float) ( ( java.lang.Math.sin ( a * x ) * java.lang.Math.cos ( b * y ) ) );
                float vy = (float) a * this.dt * yfact * (float) ( ( java.lang.Math.cos ( a * x ) * java.lang.Math.sin ( b * y ) ) );

                tot += ( vx * fx + vy * fy );
            }
            dw[ i ] = tot;
        }
        return dw;
    }

    public void stir ( float[][] force_path ) {
        float[] dw = this.project_forces ( force_path );
        for ( int i = 0; i < this.N; i++ ) {
            this.forces_dw[ i ] += dw[ i ];
        }
    }

}
