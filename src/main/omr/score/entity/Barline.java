//----------------------------------------------------------------------------//
//                                                                            //
//                               B a r l i n e                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.Shape;

import omr.log.Logger;

import omr.score.common.PagePoint;
import omr.score.common.PageRectangle;
import omr.score.common.SystemRectangle;
import omr.score.visitor.ScoreVisitor;

import omr.sheet.Scale;

import omr.stick.Stick;

import java.awt.*;
import java.util.*;

/**
 * Class <code>Barline</code> encapsulates a logical bar line, that may be
 * composed of several physical components : repeat dots, thin and thick bars.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Barline
    extends PartNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Barline.class);

    //~ Instance fields --------------------------------------------------------

    /** Precise bar line shape */
    private Shape shape;

    /**
     * Related physical sticks (bar sticks and dots), which is kept sorted on
     * stick abscissa
     */
    private SortedSet<Stick> sticks = new TreeSet<Stick>();

    /** Signature of this bar line, as abstracted from its constituents */
    private String signature;

    //~ Constructors -----------------------------------------------------------

    //---------//
    // Barline //
    //---------//
    /**
     * Create a bar line, in a containing measure
     *
     * @param measure the containing measure
     */
    public Barline (Measure measure)
    {
        super(measure);
    }

    //---------//
    // Barline //
    //---------//
    /**
     * Needed for XML binding
     */
    private Barline ()
    {
        super(null);
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // getLeftX //
    //----------//
    /**
     * Report the abscissa of the left side of the bar line
     *
     * @return abscissa (in units wrt system top left) of the left side
     */
    public int getLeftX ()
    {
        PagePoint topLeft = getSystem()
                                .getTopLeft();

        for (Stick stick : getSticks()) {
            if ((stick.getShape() == Shape.THICK_BARLINE) ||
                (stick.getShape() == Shape.THIN_BARLINE)) {
                // Beware : Vertical sticks using Horizontal line equation
                int x = stick.getLine()
                             .yAt(getScale()
                                      .toPixelPoint(topLeft).y);

                return getScale()
                           .pixelsToUnits(x) - topLeft.x;
            }
        }

        // No usable stick
        addError("No usable stick to compute barline abscissa");

        return 0;
    }

    //-----------//
    // getRightX //
    //-----------//
    /**
     * Report the abscissa of the right side of the bar line
     *
     * @return abscissa (in units wrt staff top left) of the right side
     */
    public int getRightX ()
    {
        int       right = 0;
        PagePoint topLeft = getSystem()
                                .getTopLeft();

        for (Stick stick : getSticks()) {
            if (stick.isBar()) {
                // Beware : Vertical sticks using Horizontal line equation
                int x = stick.getLine()
                             .yAt(getScale()
                                      .toPixelPoint(topLeft).y);

                if (x > right) {
                    right = x;
                }
            }
        }

        return getScale()
                   .pixelsToUnits(right) - topLeft.x;
    }

    //----------//
    // getShape //
    //----------//
    /**
     * Report the shape of this bar line
     *
     * @return the (lazily determined) shape
     */
    public Shape getShape ()
    {
        if (shape == null) {
            // Use the map of signatures
            shape = Signatures.map.get(getSignature());
        }

        return shape;
    }

    //-----------//
    // getSticks //
    //-----------//
    /**
     * Report the collection of physical sticks that compose this bar line
     *
     * @return the collection of sticks, or an empty set if sticks is null
     */
    public Collection<Stick> getSticks ()
    {
        return sticks;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //----------//
    // addStick //
    //----------//
    /**
     * Include a new individual bar stick in the (complex) bar line. This
     * automatically invalidates the other bar line parameters, which will be
     * lazily re-computed when needed.
     *
     * @param stick the bar stick to include
     */
    public void addStick (Stick stick)
    {
        sticks.add(stick);

        // Invalidate parameters
        reset();
    }

    //------------//
    // forceShape //
    //------------//
    /**
     * Normally, shape should be inferred from the signature of stick
     * combination that compose the bar line, so this method is provided only
     * for the (rare) cases when we want to force the bar line shape.
     *
     * @param shape the forced shape
     */
    public void forceShape (Shape shape)
    {
        this.shape = shape;
    }

    //----------------//
    // joinsAllStaves //
    //----------------//
    /**
     * Check whether all staves are physically connected by the sticks of the
     * barline
     *
     * @param staves the collection of staves to check
     * @return true if the barline touches all staves, false otherwise
     */
    public boolean joinsAllStaves (Collection<Staff> staves)
    {
        Scale scale = getSystem()
                          .getScale();

        for (Staff staff : staves) {
            boolean overlap = false;

            for (Stick stick : getSticks()) {
                // Extrema of glyph
                PageRectangle box = scale.toUnits(stick.getContourBox());
                int           top = box.y;
                int           bot = box.y + box.height;

                // Check that staff and stick overlap vertically
                final int topStaff = staff.getPageTopLeft().y;
                final int botStaff = topStaff + staff.getHeight();

                if (Math.max(topStaff, top) < Math.min(botStaff, bot)) {
                    overlap = true;

                    break;
                }
            }

            if (!overlap) {
                return false;
            }
        }

        return true;
    }

    //-----------//
    // mergeWith //
    //-----------//
    /**
     * Merge into this bar line the components of another bar line
     *
     * @param other the other (merged) stick
     */
    public void mergeWith (Barline other)
    {
        for (Stick stick : other.getSticks()) {
            addStick(stick);
        }
    }

    //--------//
    // render //
    //--------//
    /**
     * Render the bar contour
     *
     * @param g the graphics context
     */
    public void render (Graphics g)
    {
        for (Stick stick : getSticks()) {
            if (stick.isBar()) {
                stick.renderLine(g);
            }
        }
    }

    //-------//
    // reset //
    //-------//
    /**
     * Invalidate cached data, so that it gets lazily recomputed when needed
     */
    public void reset ()
    {
        signature = null;
        setCenter(null);
        shape = null;
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report a readable description
     *
     * @return a string based on main members
     */
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Barline");

        try {
            sb.append(" ")
              .append(getShape())
              .append(" center=")
              .append(getCenter())
              .append(" sig=")
              .append(getSignature())
              .append(" sticks[");

            for (Stick stick : getSticks()) {
                sb.append("#")
                  .append(stick.getId());
            }

            sb.append("]");
        } catch (NullPointerException e) {
            sb.append(" INVALID");
        }

        sb.append("}");

        return sb.toString();
    }

    //---------------//
    // computeCenter //
    //---------------//
    @Override
    protected void computeCenter ()
    {
        setCenter(computeGlyphsCenter(getSticks()));
    }

    //-----------//
    // getLetter //
    //-----------//
    private String getLetter (Shape shape)
    {
        if (shape == null) {
            logger.warning("Barline. getLetter for null shape");

            return null;
        }

        switch (shape) {
        case THICK_BARLINE :
            return "K";

        case THIN_BARLINE :
            return "N";

        case DOUBLE_BARLINE :
            return "NN";

        case FINAL_BARLINE :
            return "NK";

        case REVERSE_FINAL_BARLINE :
            return "KN";

        case LEFT_REPEAT_SIGN :
            return "KNO";

        case RIGHT_REPEAT_SIGN :
            return "ONK";

        case BACK_TO_BACK_REPEAT_SIGN :
            return "ONKNO";

        case DOT :
        case REPEAT_DOTS :
            return "O"; // Capital o (not zero)

        default :
            addError("Unknown bar component : " + shape);

            return null;
        }
    }

    //--------------//
    // getSignature //
    //--------------//
    /**
     * Compute a signature for this barline, based on the composing sticks.
     * We elaborate this signature for first staff of the part only, to get rid
     * of sticks roughly one above the other
     */
    private String getSignature ()
    {
        if (signature == null) {
            final Measure       measure = (Measure) getParent();
            final ScoreSystem   system = measure.getSystem();
            final StringBuilder sb = new StringBuilder();
            final Staff         staffRef = measure.getPart()
                                                  .getFirstStaff();
            final int           topStaff = staffRef.getPageTopLeft().y -
                                           system.getTopLeft().y;
            final int           botStaff = topStaff + staffRef.getHeight();
            String              last = null; // Last stick

            for (Stick stick : getSticks()) {
                String letter = getLetter(stick.getShape());

                if (letter != null) {
                    if (letter.equals("O")) {
                        // DOT
                        Staff staff = system.getStaffAt(
                            system.toSystemPoint(stick.getLocation()));

                        if (staff != staffRef) {
                            continue;
                        }
                    } else {
                        // BAR : Check overlap with staff reference
                        SystemRectangle box = system.toSystemRectangle(
                            stick.getContourBox());

                        if (Math.max(box.y, topStaff) > Math.min(
                            box.y + box.height,
                            botStaff)) {
                            continue;
                        }
                    }

                    if (last == null) {
                        sb.append(letter);
                    } else {
                        if (last.equals(letter)) {
                            if (letter.equals("N")) {
                                sb.append(letter);
                            }
                        } else {
                            sb.append(letter);
                        }
                    }

                    last = letter;
                }
            }

            signature = sb.toString();

            if (logger.isFineEnabled()) {
                logger.fine("sig=" + sb);
            }
        }

        return signature;
    }

    //~ Inner Classes ----------------------------------------------------------

    //------------//
    // Signatures //
    //------------//
    private static class Signatures
    {
        //~ Static fields/initializers -----------------------------------------

        public static final Map<String, Shape> map = new HashMap<String, Shape>();

        static {
            map.put("N", Shape.THIN_BARLINE);
            map.put("NN", Shape.DOUBLE_BARLINE);
            map.put("NK", Shape.FINAL_BARLINE);
            map.put("KN", Shape.REVERSE_FINAL_BARLINE);
            map.put("ONK", Shape.RIGHT_REPEAT_SIGN);
            map.put("KNO", Shape.LEFT_REPEAT_SIGN);

            map.put("ONKNO", Shape.BACK_TO_BACK_REPEAT_SIGN);
            map.put("NKNO", Shape.BACK_TO_BACK_REPEAT_SIGN); // For convenience
            map.put("ONKN", Shape.BACK_TO_BACK_REPEAT_SIGN); // For convenience
            map.put("NKN", Shape.BACK_TO_BACK_REPEAT_SIGN); // For convenience
        }
    }
}
