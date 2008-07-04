//----------------------------------------------------------------------------//
//                                                                            //
//                             S c o r e V i e w                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.score.Score;
import omr.score.ScoreSheetBridge;
import omr.score.common.PagePoint;
import omr.score.common.ScorePoint;
import omr.score.common.ScoreRectangle;
import omr.score.common.SystemPoint;
import omr.score.common.UnitDimension;
import omr.score.entity.Measure;
import omr.score.entity.Slot;
import omr.score.entity.System;
import static omr.score.ui.ScoreConstants.*;
import omr.score.visitor.ScorePainter;

import omr.selection.Selection;
import omr.selection.SelectionHint;
import omr.selection.SelectionTag;

import omr.sheet.Sheet;

import omr.ui.PixelCount;
import omr.ui.SheetAssembly;
import omr.ui.util.Panel;
import omr.ui.view.Rubber;
import omr.ui.view.RubberZoomedPanel;
import omr.ui.view.ScrollView;
import omr.ui.view.Zoom;
import omr.ui.view.ZoomedPanel;

import omr.util.Logger;
import omr.util.TreeNode;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.*;

import javax.swing.*;

/**
 * Class <code>ScoreView</code> encapsulates the horizontal display of all the
 * score systems which may encompass several pages & systems. It knows about the
 * display zoom ratio of the score.
 *
 * <dl>
 * <dt><b>Selection Inputs:</b></dt><ul>
 * <li>SCORE Location
 * </ul>
 * </dl>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ScoreView
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreView.class);

    //~ Instance fields --------------------------------------------------------

    /** The related score */
    private final Score score;

    /** Display zoom */
    private final Zoom zoom = new Zoom(0.5d);

    /** Mouse rubber */
    private final Rubber rubber = new Rubber(zoom);

    /** The displayed view */
    private final MyPanel view = new MyPanel(zoom, rubber);

    /** The scroll pane */
    private final ScrollView scrollPane = new ScrollView(view);

    /** The info zone */
    private final JLabel info = new JLabel("Score");

    /** The scroll pane + info zone */
    private final Panel compoundPanel = new Panel();

    /** Related popup menu */
    private final ScoreMenu scoreMenu;

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // ScoreView //
    //-----------//
    /**
     * Create the view dedicated to the display of the provided score.
     *
     * @param score the score to display
     */
    public ScoreView (Score score)
    {
        if (logger.isFineEnabled()) {
            logger.fine("new ScoreView on " + score);
        }

        // Explicitly register the display to Score Selection
        Sheet     sheet = score.getSheet();
        Selection locationSelection = sheet.getSelection(
            SelectionTag.SCORE_RECTANGLE);
        view.setLocationSelection(locationSelection);
        locationSelection.addObserver(view);

        // Layout
        info.setHorizontalAlignment(SwingConstants.LEFT);
        compoundPanel.setLayout(new BorderLayout());
        compoundPanel.add(scrollPane.getComponent(), BorderLayout.CENTER);
        compoundPanel.add(info, BorderLayout.SOUTH);

        // Cross referencing between score and its view
        this.score = score;
        score.setView(this);

        // Popup
        scoreMenu = new ScoreMenu(score);

        // Compute origin of all members
        computeModelSize();

        // Bridge companion between Score and Sheet locations both ways
        new ScoreSheetBridge(score);

        // Force selection update
        Selection pixelSelection = sheet.getSelection(
            SelectionTag.SHEET_RECTANGLE);
        pixelSelection.reNotifyObservers(null);
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Report the encapsulated UI component
     *
     * @return the UI component
     */
    public Component getComponent ()
    {
        return compoundPanel;
    }

    //----------//
    // getScore //
    //----------//
    /**
     * Report the score this view is dedicated to
     *
     * @return the related score
     */
    public Score getScore ()
    {
        return score;
    }

    //-------//
    // close //
    //-------//
    /**
     * Close the score display, by removing it from the enclosing tabbed pane.
     */
    public void close ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("Closing scoreView");
        }

        Sheet sheet = score.getSheet();

        if (sheet != null) {
            SheetAssembly assembly = sheet.getAssembly();

            if (assembly != null) {
                assembly.closeScoreView();
            }
        }
    }

    //------------------//
    // computeModelSize //
    //------------------//
    /**
     * Run computations on the tree of score, systems, etc, so that all display
     * data, such as origins and widths are available for display use.
     */
    public void computeModelSize ()
    {
        ScoreRectangle scoreContour = null;

        for (TreeNode node : score.getSystems()) {
            System         system = (System) node;
            ScoreRectangle absSystemContour = system.toScoreRectangle(
                system.getContour());

            if (scoreContour == null) {
                scoreContour = absSystemContour;
            } else {
                Rectangle r = scoreContour.union(absSystemContour);
                scoreContour = new ScoreRectangle(r.x, r.y, r.width, r.height);
            }
        }

        view.setModelSize(
            new Dimension(scoreContour.width, scoreContour.height));
        zoom.fireStateChanged();
    }

    //-----------//
    // highLight //
    //-----------//
    /**
     * Highlight the corresponding slot within the score display, using the
     * values of measure and slot.
     * @param measure the measure that contains the highlighted slot
     * @param slot the slot to highlight
     */
    public void highLight (final Measure measure,
                           final Slot    slot)
    {
        SwingUtilities.invokeLater(
            new Runnable() {
                    public void run ()
                    {
                        view.highLight(measure, slot);
                    }
                });
    }

    //---------//
    // repaint //
    //---------//
    public void repaint ()
    {
        scrollPane.getComponent()
                  .repaint();
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report a readable description
     *
     * @return a string based on the score XML file name
     */
    @Override
    public String toString ()
    {
        return "{ScoreView " + score.getRadix() + "}";
    }

    //-----------//
    // tellPoint //
    //-----------//
    private void tellPoint (ScorePoint  scrPt,
                            PagePoint   pagPt,
                            SystemPoint sysPt)
    {
        StringBuilder sb = new StringBuilder();

        //        if (scrPt != null) {
        //            sb.append("ScorePoint")
        //              .append(" X:")
        //              .append(scrPt.x)
        //              .append(" Y:")
        //              .append(scrPt.y)
        //              .append(" ");
        //        }
        if (pagPt != null) {
            sb.append("PagePoint")
              .append(" X:")
              .append(pagPt.x)
              .append(" Y:")
              .append(pagPt.y)
              .append(" ");
        }

        if (sysPt != null) {
            sb.append("SystemPoint")
              .append(" X:")
              .append(sysPt.x)
              .append(" Y:")
              .append(sysPt.y)
              .append(" ");
        }

        info.setText(sb.toString());
    }

    //~ Inner Classes ----------------------------------------------------------

    //---------//
    // MyPanel //
    //---------//
    public class MyPanel
        extends RubberZoomedPanel
    {
        //~ Instance fields ----------------------------------------------------

        // Currently highlighted slot & measure
        private Slot    highlightedSlot;
        private Measure highlightedMeasure;

        //~ Constructors -------------------------------------------------------

        //---------//
        // MyPanel //
        //---------//
        MyPanel (Zoom   zoom,
                 Rubber rubber)
        {
            super(zoom, rubber);
            setName("ScoreView-MyPanel");
            rubber.setMouseMonitor(this);

            // Initialize drawing colors, border, opacity.
            setBackground(new Color(255, 255, 220));
            setForeground(Color.black);
        }

        //~ Methods ------------------------------------------------------------

        //-----------------//
        // contextSelected //
        //-----------------//
        @Override
        public void contextSelected (MouseEvent e,
                                     Point      pt)
        {
            super.pointSelected(e, pt);

            // Let the user disable this popup feature if so desired
            if (!constants.popupEnabled.getValue()) {
                return;
            }

            // Context parameters
            ScorePoint scrPt = new ScorePoint(pt.x, pt.y);

            // Update the popup menu according to selected scores
            scoreMenu.updateMenu(scrPt);

            // Show the popup menu
            scoreMenu.getPopup()
                     .show(this, e.getX() + 20, e.getY() + 30);
        }

        //-----------//
        // highLight //
        //-----------//
        public void highLight (Measure measure,
                               Slot    slot)
        {
            this.highlightedMeasure = measure;
            this.highlightedSlot = slot;

            int margin = constants.measureMargin.getValue();

            // Safer
            if ((measure == null) || (slot == null)) {
                repaint(); // To erase previous highlight

                return;
            }

            UnitDimension dimension = measure.getSystem()
                                             .getDimension();
            Point         origin = measure.getDisplayOrigin();
            ZoomedPanel   zp = (ZoomedPanel) this;

            // If the current measure is at the beginning of a system,
            // make the most of this (new) system as visible as possible
            if (measure.getPreviousSibling() == null) {
                Rectangle rect = new Rectangle(
                    origin.x,
                    origin.y,
                    dimension.width,
                    dimension.height + STAFF_HEIGHT);
                zp.showFocusLocation(rect);
            }

            // Make the measure rectangle visible
            Rectangle rect = new Rectangle(
                (origin.x + measure.getLeftX()) - margin,
                origin.y - margin,
                measure.getWidth() + (2 * margin),
                dimension.height + STAFF_HEIGHT + (2 * margin));
            zp.showFocusLocation(rect);
        }

        //--------//
        // render //
        //--------//
        @Override
        public void render (Graphics g)
        {
            ScorePainter painter = new ScorePainter(g, zoom);
            score.accept(painter);

            if (highlightedSlot != null) {
                painter.drawSlot(
                    true,
                    highlightedMeasure,
                    highlightedSlot,
                    Color.MAGENTA);
            }
        }

        //--------//
        // update //
        //--------//
        /**
         * Call-back (part of Observer interface) triggered when location has
         * been modified
         *
         * @param selection the notified Selection
         * @param hint potential notification hint
         */
        @Override
        public void update (Selection     selection,
                            SelectionHint hint)
        {
            if (logger.isFineEnabled()) {
                logger.fine("ScoreView: Score selection updated " + selection);
            }

            // Show location in display (default dehavior)
            super.update(selection, hint);

            // Display location coordinates
            if (selection == locationSelection) {
                Rectangle rect = (Rectangle) selection.getEntity();

                if (rect != null) {
                    ScorePoint  scrPt = new ScorePoint(rect.x, rect.y);
                    System      system = score.scoreLocateSystem(scrPt);
                    PagePoint   pagPt = system.toPagePoint(scrPt);
                    SystemPoint sysPt = system.toSystemPoint(pagPt);
                    tellPoint(scrPt, pagPt, sysPt);
                } else {
                    tellPoint(null, null, null);
                }
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        PixelCount       measureMargin = new PixelCount(
            10,
            "Number of pixels as margin when highlighting a measure");
        Constant.Boolean popupEnabled = new Constant.Boolean(
            true,
            "Should we allow popup menu in score view?");
    }
}
