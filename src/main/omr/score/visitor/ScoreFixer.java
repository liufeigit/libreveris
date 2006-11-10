//----------------------------------------------------------------------------//
//                                                                            //
//                            S c o r e F i x e r                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score.visitor;

import omr.score.Barline;
import omr.score.Beam;
import omr.score.Chord;
import omr.score.Clef;
import omr.score.KeySignature;
import omr.score.Measure;
import omr.score.MusicNode;
import omr.score.Score;
import static omr.score.ScoreConstants.*;
import omr.score.ScorePoint;
import omr.score.Slur;
import omr.score.Staff;
import omr.score.StaffNode;
import omr.score.System;
import omr.score.TimeSignature;

import omr.util.Dumper;
import omr.util.Logger;

import java.awt.Point;

/**
 * Class <code>ScoreFixer</code> visits the score hierarchy to fix
 * internal data.
 * Run computations on the tree of score, systems, etc, so that all display
 * data, such as origins and widths are available for display use.
 *
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScoreFixer
    implements Visitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreFixer.class);

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ScoreFixer object.
     */
    public ScoreFixer ()
    {
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

    //------------//
    // visit Chord //
    //------------//
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
        // Fix the staff reference
        measure.fixStaff();

        // First/Last measure ids
        Staff staff = measure.getStaff();
        staff.incrementLastMeasureId();
        measure.setId(staff.getLastMeasureId());

        if (logger.isFineEnabled()) {
            Dumper.dump(measure, "Computed");
        }

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
        if (logger.isFineEnabled()) {
            logger.info("Computing score ...");
        }

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
        // Display origin for the staff
        System system = staff.getSystem();
        Point  sysorg = system.getOrigin();
        staff.setDisplayOrigin(
            new ScorePoint(
                sysorg.x,
                sysorg.y +
                (staff.getTopLeft().y - staff.getSystem().getTopLeft().y)));

        // First/Last measure ids
        staff.setFirstMeasureId(system.getFirstMeasureId());
        staff.setLastMeasureId(system.getFirstMeasureId());

        if (logger.isFineEnabled()) {
            Dumper.dump(staff, "Computed");
        }

        return true;
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
        // Is there a Previous System ?
        System     prevSystem = (System) system.getPreviousSibling();

        ScorePoint origin = new ScorePoint();

        if (prevSystem == null) {
            // Very first system in the score
            origin.move(SCORE_INIT_X, SCORE_INIT_Y);
            system.setFirstMeasureId(0);
        } else {
            // Not the first system
            origin.setLocation(prevSystem.getOrigin());
            origin.translate(
                prevSystem.getDimension().width - 1 + INTER_SYSTEM,
                0);
            system.setFirstMeasureId(prevSystem.getLastMeasureId());
        }

        system.setOrigin(origin);

        if (logger.isFineEnabled()) {
            Dumper.dump(system, "Computed");
        }

        return true;
    }

    //---------------------//
    // visit TimeSignature //
    //---------------------//
    public boolean visit (TimeSignature timeSignature)
    {
        return true;
    }
}
