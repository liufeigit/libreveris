//----------------------------------------------------------------------------//
//                                                                            //
//                         S y m b o l s E d i t o r                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.ui;

import omr.glyph.Glyph;
import omr.glyph.GlyphEvaluator;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphNetwork;
import omr.glyph.GlyphSection;
import omr.glyph.Glyphs;

import omr.lag.Sections;
import omr.lag.ui.RunBoard;
import omr.lag.ui.ScrollLagView;
import omr.lag.ui.SectionBoard;

import omr.log.Logger;

import omr.selection.GlyphEvent;
import omr.selection.GlyphSetEvent;
import omr.selection.MouseMovement;
import omr.selection.SectionSetEvent;
import omr.selection.SelectionHint;
import omr.selection.SheetLocationEvent;
import omr.selection.UserEvent;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.ui.PixelBoard;
import omr.sheet.ui.SheetPainter;

import omr.step.Step;

import omr.ui.BoardsPane;

import omr.util.Implement;

import java.awt.*;
import java.awt.event.*;
import java.util.Set;

/**
 * Class <code>SymbolsEditor</code> defines a UI pane from which all symbol
 * processing actions can be launched and their results checked.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SymbolsEditor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SymbolsEditor.class);

    /** Color for hiding unknown glyphs when filter is ON */
    public static final Color hiddenColor = Color.white;

    //~ Instance fields --------------------------------------------------------

    /** Related instance of symbols builder */
    private final SymbolsController symbolsBuilder;

    /** Related sheet */
    private final Sheet sheet;

    /** Evaluator to check for NOISE glyphs */
    private final GlyphEvaluator evaluator = GlyphNetwork.getInstance();

    /** Related Lag view */
    private final GlyphLagView view;

    /** Popup menu related to glyph selection */
    private GlyphMenu glyphMenu;

    /** The entity used for display focus */
    private ShapeFocusBoard focus;

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // SymbolsEditor //
    //---------------//
    /**
     * Create a view in the sheet assembly tabs, dedicated to the display and
     * handling of glyphs
     *
     * @param sheet the sheet whose glyphs are considered
     * @param symbolsController the symbols controller for this sheet
     */
    public SymbolsEditor (Sheet             sheet,
                          SymbolsController symbolsController)
    {
        this.sheet = sheet;
        this.symbolsBuilder = symbolsController;

        GlyphLag lag = symbolsController.getLag();

        view = new MyView(lag);
        view.setLocationService(
            sheet.getSelectionService(),
            SheetLocationEvent.class);

        focus = new ShapeFocusBoard(
            sheet,
            symbolsController,
            new ActionListener() {
                    @Implement(ActionListener.class)
                    public void actionPerformed (ActionEvent e)
                    {
                        view.colorizeAllGlyphs();
                    }
                });

        glyphMenu = new GlyphMenu(symbolsController, evaluator, focus, lag);

        final String  unit = sheet.getRadix() + ":SymbolsEditor";

        BoardsPane    boardsPane = new BoardsPane(
            sheet,
            view,
            new PixelBoard(unit, sheet),
            new RunBoard(unit, lag),
            new SectionBoard(
                unit,
                symbolsController.getLag().getLastVertexId(),
                lag),
            new SymbolGlyphBoard(
                unit + "-SymbolGlyphBoard",
                symbolsController,
                0),
            focus,
            new EvaluationBoard(
                unit + "-Evaluation-ActiveBoard",
                symbolsController,
                sheet,
                view));

        // Create a hosting pane for the view
        ScrollLagView slv = new ScrollLagView(view);
        sheet.getAssembly()
             .addViewTab(Step.SYMBOLS, slv, boardsPane);
    }

    //~ Methods ----------------------------------------------------------------

    //---------//
    // refresh //
    //---------//
    /**
     * Refresh the UI display (reset the model values of all spinners, updateMenu
     * the colors of the glyphs)
     */
    public void refresh ()
    {
        view.refresh();
    }

    //~ Inner Classes ----------------------------------------------------------

    //--------//
    // MyView //
    //--------//
    private class MyView
        extends GlyphLagView
    {
        //~ Constructors -------------------------------------------------------

        private MyView (GlyphLag lag)
        {
            super(lag, null, null, symbolsBuilder, null);
            setName("SymbolsEditor-MyView");

            // Use light gray color for past successful entities
            int viewIndex = lag.viewIndexOf(this);
            sheet.colorize(lag, viewIndex, Color.lightGray);
        }

        //~ Methods ------------------------------------------------------------

        //-----------------------//
        // asyncDeassignGlyphs //
        //-----------------------//
        @Override
        public void asyncDeassignGlyphSet (Set<Glyph> glyphs)
        {
            super.asyncDeassignGlyphSet(glyphs);
        }

        //-------------------//
        // colorizeAllGlyphs //
        //-------------------//
        /**
         * Colorize all the glyphs of the sheet
         */
        @Override
        public void colorizeAllGlyphs ()
        {
            int viewIndex = lag.viewIndexOf(this);

            for (Glyph glyph : sheet.getActiveGlyphs()) {
                colorizeGlyph(viewIndex, glyph);
            }

            repaint();
        }

        //---------------//
        // colorizeGlyph //
        //---------------//
        @Override
        public void colorizeGlyph (int   viewIndex,
                                   Glyph glyph)
        {
            colorizeGlyph(
                viewIndex,
                glyph,
                focus.isDisplayed(glyph) ? glyph.getColor() : hiddenColor);
        }

        //-----------------//
        // contextSelected //
        //-----------------//
        @Override
        public void contextSelected (Point         pt,
                                     MouseMovement movement)
        {
            if (!ViewParameters.getInstance()
                               .isSectionSelectionEnabled()) {
                // Retrieve the selected glyphs
                Set<Glyph> glyphs = sheet.getVerticalLag()
                                         .getSelectedGlyphSet();

                // To display point information
                if ((glyphs == null) || glyphs.isEmpty()) {
                    pointSelected(pt, movement); // This may change glyph selection
                    glyphs = sheet.getVerticalLag()
                                  .getSelectedGlyphSet();
                }

                if ((glyphs != null) && !glyphs.isEmpty()) {
                    // Update the popup menu according to selected glyphs
                    glyphMenu.updateMenu();

                    // Show the popup menu
                    glyphMenu.getPopup()
                             .show(
                        this,
                        getZoom().scaled(pt.x),
                        getZoom().scaled(pt.y));
                } else {
                    // Popup with no glyph selected ?
                }
            } else {
                // Retrieve the selected sections
                Set<GlyphSection> sections = sheet.getVerticalLag()
                                                  .getSelectedSectionSet();
                // Update the popup menu according to selected sections
                glyphMenu.updateMenu();

                if ((sections != null) && !sections.isEmpty()) {
                    // Show the popup menu
                    glyphMenu.getPopup()
                             .show(
                        this,
                        getZoom().scaled(pt.x),
                        getZoom().scaled(pt.y));
                }
            }
        }

        //---------//
        // onEvent //
        //---------//
        /**
         * On reception of GLYPH_SET or SECTION_SET information, we build a
         * transient compound glyph which is then dispatched.
         *
         * Such glyph is always generated (a null glyph if the set is null or
         * empty, a simple glyph if the set contains just one glyph, and a true
         * compound glyph when the set contains several glyphs)
         *
         * @param event the notified event
         */
        @Override
        public void onEvent (UserEvent event)
        {
            ///logger.info("*** " + getName() + " " + event);
            try {
                // Ignore RELEASING
                if (event.movement == MouseMovement.RELEASING) {
                    return;
                }

                // Default lag view behavior, including specifics
                super.onEvent(event);

                if (event instanceof GlyphSetEvent) { // GlyphSet => Compound
                    handleEvent((GlyphSetEvent) event);
                } else if (event instanceof SectionSetEvent) { // SectionSet => Compound
                    handleEvent((SectionSetEvent) event);
                }
            } catch (Exception ex) {
                logger.warning(getClass().getName() + " onEvent error", ex);
            }
        }

        //-------------//
        // renderItems //
        //-------------//
        @Override
        protected void renderItems (Graphics g)
        {
            // Render all sheet physical info known so far
            sheet.accept(new SheetPainter(g, false));

            // Normal display of selected items
            super.renderItems(g);
        }

        //-------------//
        // handleEvent //
        //-------------//
        /**
         * Interest in GlyphSet => Compound
         * @param glyphSetEvent
         */
        private void handleEvent (GlyphSetEvent glyphSetEvent)
        {
            if (ViewParameters.getInstance()
                              .isSectionSelectionEnabled()) {
                return;
            }

            MouseMovement movement = glyphSetEvent.movement;
            Set<Glyph>    glyphs = glyphSetEvent.getData();
            Glyph         compound = null;

            if (glyphs != null) {
                if (glyphs.size() > 1) {
                    try {
                        SystemInfo system = sheet.getSystemOf(glyphs);

                        if (system != null) {
                            compound = system.buildTransientCompound(glyphs);
                        }
                    } catch (IllegalArgumentException ex) {
                        // All glyphs do not belong to the same system
                        // No compound is allowed and displayed
                        logger.warning(
                            "Glyphs from different systems " +
                            Glyphs.toString(glyphs));
                    }
                } else if (glyphs.size() == 1) {
                    compound = glyphs.iterator()
                                     .next();
                }
            }

            publish(
                new GlyphEvent(
                    this,
                    SelectionHint.GLYPH_TRANSIENT,
                    movement,
                    compound));
        }

        //-------------//
        // handleEvent //
        //-------------//
        /**
         *  Interest in SectionSetEvent => transient Glyph
         * @param sectionSetEvent
         */
        @SuppressWarnings("unchecked")
        private void handleEvent (SectionSetEvent sectionSetEvent)
        {
            if (!ViewParameters.getInstance()
                               .isSectionSelectionEnabled()) {
                return;
            }

            Set<GlyphSection> sections = sectionSetEvent.getData();

            if ((sections == null) || sections.isEmpty()) {
                return;
            }

            MouseMovement movement = sectionSetEvent.movement;
            Glyph         compound = null;

            try {
                SystemInfo system = sheet.getSystemOfSections(sections);

                if (system != null) {
                    compound = system.buildTransientGlyph(sections);
                }

                publish(
                    new GlyphEvent(
                        this,
                        SelectionHint.GLYPH_TRANSIENT,
                        movement,
                        compound));

                publish(
                    new GlyphSetEvent(
                        this,
                        SelectionHint.GLYPH_TRANSIENT,
                        movement,
                        Glyphs.set(compound)));
            } catch (IllegalArgumentException ex) {
                // All sections do not belong to the same system
                // No compound is allowed and displayed
                logger.warning(
                    "Sections from different systems " +
                    Sections.toString(sections));
            }
        }
    }
}
