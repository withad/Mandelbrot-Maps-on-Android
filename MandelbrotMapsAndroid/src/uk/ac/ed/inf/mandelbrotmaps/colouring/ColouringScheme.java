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
 * A colouring is used to dictate what colours the points on the screen will have.
 * @author mallia
 */
public interface ColouringScheme {

    /**
     * Colours a point which escapes from the set
     * @param iterations The number of iterations that the point needed to escape from the set
     * @return RGB colour as int
     */
    public int colourOutsidePoint(int iterations, int maxIterations);

    /**
     * Colours a point which is bounded to the set
     * @return returns RGB colour as int
     */
    public int colourInsidePoint();
}
