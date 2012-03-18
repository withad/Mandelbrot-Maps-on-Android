/*
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package uk.ac.ed.inf.mandelbrotmaps.colouring;


/**
 * Defines a colour scheme that moves from black to blue, cyan, green, yellow,
 * red, magenta and back to blue.
 * @author mallia
 */
public class DefaultColouringScheme implements ColouringScheme {

    /**
     * The spacing of the colours.  For example, when creating blue colours, the
     * next blue colour will be new = old + COLOUR_SPACING.
     */
    private static final int COLOUR_SPACING = 30;


    /**
     * The colouring algorithm colours in the following manner (values in brackets are RGB values):
     *   1. From Black (0,0,0) to Blue (0,0,255)
     *   2. From Blue (0,0,255) to Cyan (0,255,255)
     *   3. From Cyan (0,255,255) to Green (0,255,0)
     *   4. From Green (0,255,0) to Yellow (255,255,0)
     *   5. From Yellow (255,255,0) to Red (255,0,0)
     *   6. From Red (255,0,0) to Magenta (255,0,255)
     *   7. From Magenta (255,0,255) to Blue (0,0,255)
     *   8. Continue from step 2
     * The number of iterations handled in each segment is determined by the {@link #COLOUR_SPACING} value.
     * @param iterations
     * @return RGB colour as int
     */
    public int colourOutsidePoint(int iterations, int maxIterations){
        if (iterations <= 0){
            return 0xFF000000;
        }   
        
        int colourCodeR, colourCodeG, colourCodeB;
        double colourCode;

        // Percentage (0.0 -- 1.0)
		colourCode = (double)iterations / (double)maxIterations;
		
		// Red
		colourCodeR = Math.min((int)(255 * 6*colourCode), 255);
		
		// Green
		colourCodeG = (int)(255*colourCode);
		
		// Blue
		colourCodeB = (int)(127.5 - 127.5*Math.cos(7 * Math.PI * colourCode));
		
        //Compute colour from the three components
		int colourCodeHex = (0xFF << 24) + (colourCodeR << 16) + (colourCodeG << 8) + (colourCodeB);

        return colourCodeHex;
    }

    public int colourInsidePoint() {
        return 0xFFFFFFFF;
    }


}

