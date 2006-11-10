//----------------------------------------------------------------------------//
//                                                                            //
//                      R e n d e r i n g V i s i t o r                       //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score.visitor;

import omr.glyph.Glyph;

import omr.score.Barline;
import omr.score.Clef;
import omr.score.KeySignature;
import omr.score.Measure;
import omr.score.MusicNode;
import omr.score.Score;
import omr.score.Slur;
import omr.score.Staff;
import omr.score.StaffNode;
import omr.score.System;
import omr.score.TimeSignature;

import omr.sheet.Ending;
import omr.sheet.Ledger;
import omr.sheet.Sheet;
import omr.sheet.StaffInfo;
import omr.sheet.SystemInfo;

import omr.stick.Stick;

import omr.ui.view.Zoom;

import omr.util.Logger;

import java.awt.*;
import omr.score.Beam;
import omr.score.Chord;

/**
 * Class <code>SheetPainter</code> defines for every node in Score hierarchy
 * the rendering of related sections (with preset colors) in the dedicated
 * <b>Sheet</b> display.
 *
 * <p>Nota: It has been extended to deal with rendering of initial sheet
 * elements.
 *
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class SheetPainter
    implements Visitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SheetPainter.class);

    //~ Instance fields --------------------------------------------------------

    /** Graphic context */
    private final Graphics g;

    /** Display zoom */
    private final Zoom z;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // SheetPainter //
    //--------------//
    /**
     * Creates a new SheetPainter object.
     *
     *
     * @param g Graphic context
     * @param z zoom factor
     */
    public SheetPainter (Graphics g,
                         Zoom     z)
    {
        this.g = g;
        this.z = z;
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // visit Barline //
    //---------------//
    public boolean visit (Barline barline)
    {
        return true;
    }

    //------------//
    // visit Beam //
    //------------//
    public boolean visit (Beam beam)
    {
        return true;
    }

    //-------------//
    // visit Chord //
    //-------------//
    public boolean visit (Chord chord)
    {
        return true;
    }

    //------------//
    // visit Clef //
    //------------//
    public boolean visit (Clef clef)
    {
        return true;
    }

    //--------------------//
    // visit KeySignature //
    //--------------------//
    public boolean visit (KeySignature keySignature)
    {
        return true;
    }

    //---------------//
    // visit Measure //
    //---------------//
    public boolean visit (Measure measure)
    {
        // Render the measure ending barline, if within the clipping area
        measure.getBarline()
               .render(g, z);

        return true;
    }

    //-----------------//
    // visit MusicNode //
    //-----------------//
    public boolean visit (MusicNode musicNode)
    {
        return true;
    }

    //-------------//
    // visit Score //
    //-------------//
    public boolean visit (Score score)
    {
        score.acceptChildren(this);

        return false;
    }

    //------------//
    // visit Slur //
    //------------//
    public boolean visit (Slur slur)
    {
        return true;
    }

    //-------------//
    // visit Staff //
    //-------------//
    public boolean visit (Staff staff)
    {
        StaffInfo info = staff.getInfo();

        // Render the staff lines, if within the clipping area
        if ((info != null) && info.render(g, z)) {
            // Render the staff starting barline, if any
            if (staff.getStartingBarline() != null) {
                staff.getStartingBarline()
                     .render(g, z);
            }

            return true;
        } else {
            return false;
        }
    }

    //-----------------//
    // visit StaffNode //
    //-----------------//
    public boolean visit (StaffNode staffNode)
    {
        return true;
    }

    //--------------//
    // visit System //
    //--------------//
    public boolean visit (System system)
    {
        return true;
    }

    //---------------------//
    // visit TimeSignature //
    //---------------------//
    public boolean visit (TimeSignature timeSignature)
    {
        return true;
    }

    /**
     * Although a Sheet is not part of the Score hierarchy, this visitor has
     * been specifically extended to render all physical info of a sheet
     *
     * @param sheet the sheet to render initial elements
     */
    //-------------//
    // visit Sheet //
    //-------------//
    public boolean visit (Sheet sheet)
    {
        // Use specific color
        g.setColor(Color.lightGray);

        Score score = sheet.getScore();

        if ((score != null) && (score.getSystems()
                                     .size() > 0)) {
            // Normal (full) rendering of the score
            score.accept(this);
        } else {
            // Render what we have got so far
            if (sheet.LinesAreDone()) {
                for (StaffInfo staff : sheet.getStaves()) {
                    staff.render(g, z);
                }
            }
        }

        if (sheet.BarsAreDone()) {
            for (SystemInfo system : sheet.getSystems()) {
                // Check that this system is visible
                Rectangle box = new Rectangle(
                    0,
                    system.getAreaTop(),
                    Integer.MAX_VALUE,
                    system.getAreaBottom() - system.getAreaTop() + 1);
                z.scale(box);

                if (box.intersects(g.getClipBounds())) {
                    g.setColor(Color.lightGray);

                    // Staff lines
                    for (StaffInfo staff : system.getStaves()) {
                        staff.render(g, z);
                    }

                    // Bar lines
                    for (Stick bar : system.getBars()) {
                        bar.renderLine(g, z);
                    }

                    g.setColor(Color.black);

                    // Stems
                    for (Glyph glyph : system.getGlyphs()) {
                        if (glyph.isStem()) {
                            Stick stick = (Stick) glyph;
                            stick.renderLine(g, z);
                        }
                    }

                    // Ledgers
                    for (Ledger ledger : system.getLedgers()) {
                        ledger.render(g, z);
                    }

                    // Endings
                    for (Ending ending : system.getEndings()) {
                        ending.render(g, z);
                    }
                }
            }
        } else {
            // Horizontals
            if (sheet.HorizontalsAreDone()) {
                // Ledgers
                for (Ledger ledger : sheet.getHorizontals()
                                          .getLedgers()) {
                    ledger.render(g, z);
                }

                // Endings
                for (Ending ending : sheet.getHorizontals()
                                          .getEndings()) {
                    ending.render(g, z);
                }
            }
        }

        return true;
    }
}
