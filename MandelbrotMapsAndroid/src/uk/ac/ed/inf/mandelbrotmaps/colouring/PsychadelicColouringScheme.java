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
 * A colouring scheme based on a conical spiral function.  Recall that a conical 
 * function is defined as:
 * <ol>
 *  <li>r(t) = t</li>
 *  <li>x(t) = r(t) * (a.r(t) * cos(t))</li>
 *  <li>y(t) = r(t) * (a.r(t) * sin(t))</li>
 *  <li>z(t) = t</li>
 *  <li> where t is an angle in radians and a is a constant</li>
 * </ol> (Refer to http://www.mathematische-basteleien.de/spiral.htm for more info)
 * 
 * Using this definition of a conical spiral, we model our conical spiral with 
 * a = 2 and which resides only in the positive space of the xyz-plane.
 *  
 * @author mallia
 */
public class PsychadelicColouringScheme implements ColouringScheme {

    /**
     * Colours a point which is bound to the set.  All points are coloured white.
     * @return The RGB colour for the point
     */
    public int colourInsidePoint() {
        return 0xFFFFFFFF;
    }
       
    /**
     * Colours a point which has escaped from the set.  The number of iterations
     * converted into a radian angle, where 2pi represents 255 iterations.
     * @param iterations the number of iterations the point escaped at
     * @return The RGB colour for the point
     */
    public int colourOutsidePoint(int iterations, int maxIterations){
        //return black if the point escaped after 0 iterations
        if (iterations == 0){
            return 0xFF000000;
        }
        
        //calucalate theta - 2pi represents 255 iterations
        double theta = (double) ((double)iterations / (double)255) * 2 * Math.PI;
        
        //compute r
        double r = r(theta);

        //compute x
        double x = x(theta);

        //compute y
        double y = y(theta);        
        
        //defines the number of colours used in each component of RGB
        int colourRange = 230;
        //the starting point in each compenent of RGB
        int startColour = 25;

        //compute the red compnent
        int colourCodeR = (int) (colourRange * r);
        colourCodeR = boundColour(colourCodeR, colourRange);
        colourCodeR += startColour;
        
        //compute the green component
        int colourCodeG = (int) (colourRange * y);
        colourCodeG = boundColour(colourCodeG, colourRange);
        colourCodeG += startColour;
        
        //compute the blue component
        int colourCodeB = (int) (colourRange * x);
        colourCodeB = boundColour(colourCodeB, colourRange);
        colourCodeB += startColour;

        //compute colour from the three components
        int colourCodeHex = (0xFF << 24) + (colourCodeR << 16) + (colourCodeG << 8) + (colourCodeB);

        //return colour
        return colourCodeHex;
    }
   
    
    /**
     * Bounds colour to a limit.  If that limit is reached the colour follows a path
     * back to 0 and then starts off again.
     * @return
     */
    private int boundColour(int colour, int colourRange){
        if (colour > (colourRange * 2)){ 
            int i = (int) (colour / (colourRange * 2));

            colour = colour - (colourRange * 2 * i);
        }
        if (colour > (colourRange)){
            colour = colourRange - (colour - colourRange);
        }
        
        return colour;
    }
    
    /**
     * Calculates r.  In this case, returns the value of theta
     * @param theta An angle in radians
     * @return The value of r for the conical spiral
     */
    private double r(double theta){
        return theta;
    }

    /**
     * Calucates x.  The mathematical function has been modified to return only 
     * positive numbers (not the +1 which shifts all return values of cos between
     * 0 and 2).
     * @param theta An angle in radians
     * @return The value of x for the conical spiral
     */
    private double x(double t){
        return  t * (2.0 * (Math.cos(t) + 1));
    }

    /**
     * Calucates y.  The mathematical function has been modified to return only 
     * positive numbers (not the +1 which shifts all return values of sin between
     * 0 and 2).
     * @param theta An angle in radians
     * @return The value of y for the conical spiral
     */
    private double y(double t){
        return t * (2.0 * (Math.sin(t) + 1));
    }

}

