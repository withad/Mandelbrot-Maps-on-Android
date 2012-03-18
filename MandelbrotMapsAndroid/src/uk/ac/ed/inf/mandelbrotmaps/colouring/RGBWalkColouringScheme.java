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
public class RGBWalkColouringScheme implements ColouringScheme {

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

        //number of iterations that we can handle in each segment of the colour scheme
        int maxValueForColour = 220;
        double iterationsPerSegmentDouble = ((double) maxValueForColour) / (double) COLOUR_SPACING;
        int iterationsPerSegment = (int) Math.floor(iterationsPerSegmentDouble);

        int iterationsPerPeriod = iterationsPerSegment * 6;

        //normalise the iteration count to be between 1 and iterationsPerSegment * noOfSegments (i.e. 6)
        boolean exceeded = false;


        while (iterations >= iterationsPerPeriod){
            exceeded = true;
            int i = iterations / iterationsPerPeriod;
            iterations = iterations - (iterationsPerPeriod * i);
        }

        //int m = 0;
        int colourCodeR = 0;
        int colourCodeG = 0;
        int colourCodeB = 0;

        //1. From Black (0,0,0) to Blue (0,0,255)
        if ((iterations < (iterationsPerSegment)) && !(exceeded)){
            //the colour sequence from within the segment
            int segmentSequenceNo = iterations;
            colourCodeR = 0;
            colourCodeG = 0;
            colourCodeB = (int) (segmentSequenceNo * COLOUR_SPACING);
        }
        //7. From Magenta (255,0,255) to Blue (0,0,255)
        else if ((iterations < (iterationsPerSegment)) && (exceeded)){
            int segmentSequenceNo = iterations;
            colourCodeR = maxValueForColour - ((int) (segmentSequenceNo * COLOUR_SPACING));
            colourCodeG = 0;
            colourCodeB = maxValueForColour;
        }
        //2. From Blue (0,0,255) to Cyan (0,255,255)
        else if ((iterations >= iterationsPerSegment) && (iterations < iterationsPerSegment * 2)){
            int segmentSequenceNo = iterations - (int) iterationsPerSegment;
            colourCodeR = 0;
            colourCodeG = ((int) (segmentSequenceNo * COLOUR_SPACING));
            colourCodeB = maxValueForColour;
        }
        //3. From Cyan (0,255,255) to Green (0,255,0)
        else if ((iterations >= iterationsPerSegment * 2) && (iterations < iterationsPerSegment * 3)){
            int segmentSequenceNo = iterations - (int) (iterationsPerSegment * 2);
            colourCodeR = 0;
            colourCodeG = maxValueForColour;
            colourCodeB = maxValueForColour - ((int) (segmentSequenceNo * COLOUR_SPACING));
        }
        //4. From Green (0,255,0) to Yellow (255,255,0)
        else if ((iterations >= iterationsPerSegment * 3) && (iterations < iterationsPerSegment * 4)){
            int segmentSequenceNo = iterations - (int) (iterationsPerSegment * 3);
            colourCodeR = ((int) (segmentSequenceNo * COLOUR_SPACING));
            colourCodeG = maxValueForColour;
            colourCodeB = 0;
        }
        //5. From Yellow (255,255,0) to Red (255,0,0)
        else if ((iterations >= iterationsPerSegment * 4) && (iterations < iterationsPerSegment * 5)){
            int segmentSequenceNo = iterations - (int) (iterationsPerSegment * 4);
            colourCodeR = maxValueForColour;
            colourCodeG = maxValueForColour - ((int) (segmentSequenceNo * COLOUR_SPACING));
            colourCodeB = 0;
        }
        //6. From Red (255,0,0) to Magenta (255,0,255)
        else if ((iterations >= iterationsPerSegment * 5) && (iterations < iterationsPerSegment * 6)){
            int segmentSequenceNo = iterations - (int) (iterationsPerSegment * 5);
            colourCodeR = maxValueForColour;
            colourCodeG = 0;
            colourCodeB = ((int) (segmentSequenceNo * COLOUR_SPACING));
        }

        int colourCodeHex = (0xFF << 24) + (colourCodeR << 16) + (colourCodeG << 8) + (colourCodeB);

        return colourCodeHex;
    }

    public int colourInsidePoint() {
        return 0xFFFFFFFF;
        //return 0xFF000000;
    }


}

