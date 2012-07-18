//----------------------------------------------------------------------------//
//                                                                            //
//                     T e x t B o r d e r P a t t e r n                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.pattern;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;
import omr.text.TextLine;
import omr.text.TextBuilder;
import omr.text.TextBlob;

import omr.grid.LineInfo;
import omr.grid.StaffInfo;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.BrokenLine;
import omr.util.HorizontalSide;
import omr.util.VerticalSide;

import java.awt.Polygon;
import java.util.Arrays;
import java.util.List;

/**
 * Class {@code TextBorderPattern} directly uses OCR on specific
 * regions around staves of a given system to retrieve text-shaped
 * glyphs.
 *
 * <ol>
 * <li>We define near-rectangular zones above and below systems, and on left
 * and right sides of the system.</li>
 * <li>For each zone, all contained glyphs are filtered and gathered into blobs
 * (each blob being likely to contain one sentence).</li>
 * <li>NOTA: By-passing the neural network,each blob is directly handed to the
 * OCR engine.</li>
 * <li>Suitable OcrLines returned by the OCR engine are then used to assign
 * the TEXT shape to the corresponding glyphs.</li>
 * </ol>
 *
 * <p>TODO: Rather than detecting text glyphs candidates and then calling OCR,
 * we should redesign this class to let Tesseract 3 analyse the layout of each
 * border zone by itself (using MULTI_BLOCK mode), and simply build the text
 * items based on Tesseract results.</p>
 *
 * @author Hervé Bitteur
 */
public class TextBorderPattern
        extends AbstractBlobPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
            TextBorderPattern.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** The system contour box (just the union of staves) */
    private final PixelRectangle systemBox;

    // Scale dependent constants
    private final int maxFontSize;

    //~ Constructors -----------------------------------------------------------
    //
    //-------------------//
    // TextBorderPattern //
    //-------------------//
    /**
     * Creates a new TextBorderPattern object.
     *
     * @param system the containing system
     */
    public TextBorderPattern (SystemInfo system)
    {
        super("TextBorder", system);

        maxFontSize = scale.toPixels(constants.maxFontSize);

        // System contour box
        systemBox = new PixelRectangle(
                system.getLeft(),
                system.getTop(),
                system.getWidth(),
                system.getBottom() - system.getTop());
    }

    //~ Methods ----------------------------------------------------------------
    //
    //--------------//
    // buildRegions //
    //--------------//
    @Override
    protected List<? extends Region> buildRegions ()
    {
        Sheet sheet = system.getSheet();
        int staffMarginAbove = scale.toPixels(
                constants.staffMarginAbove);
        int staffMarginBelow = scale.toPixels(
                constants.staffMarginBelow);

        final int left = system.getLeft();
        final int right = system.getRight();
        BrokenLine topLine = system.getBoundary().getLimit(VerticalSide.TOP);
        BrokenLine botLine = system.getBoundary().getLimit(VerticalSide.BOTTOM);
        List<StaffInfo> staves = system.getStaves();

        StaffInfo firstStaff = staves.get(0);
        LineInfo firstLine = firstStaff.getFirstLine();
        PixelPoint nw = new PixelPoint(0, firstLine.yAt(0));
        PixelPoint ne = new PixelPoint(
                sheet.getWidth(),
                firstLine.yAt(sheet.getWidth()));
        // Move up a bit
        nw.y -= staffMarginAbove;
        ne.y -= staffMarginAbove;

        StaffInfo lastStaff = staves.get(staves.size() - 1);
        LineInfo lastLine = lastStaff.getLastLine();
        PixelPoint sw = new PixelPoint(0, lastLine.yAt(0));
        PixelPoint se = new PixelPoint(
                sheet.getWidth(),
                lastLine.yAt(sheet.getWidth()));
        // Move down a bit
        sw.y += staffMarginBelow;
        se.y += staffMarginBelow;

        return Arrays.
                asList(
                new MyRegion("North", buildPolygon(topLine.getPoints(), ne, nw)),
                new MyRegion("South", buildPolygon(botLine.getPoints(), se, sw)),
                new MyRegion(
                "West",
                buildPolygon(
                nw,
                new PixelPoint(left, firstLine.yAt(left)),
                new PixelPoint(left, lastLine.yAt(left)),
                sw)),
                new MyRegion(
                "East",
                buildPolygon(
                new PixelPoint(right, firstLine.yAt(right)),
                ne,
                se,
                new PixelPoint(right, lastLine.yAt(right)))));
    }

    //-----------//
    // borderOCR //
    //-----------//
    private TextLine borderOCR (Glyph compound,
                               String language)
    {
        List<TextLine> lines = compound.retrieveOcrLines(language);

        // Debug
        if (logger.isFineEnabled()) {
            int i = 0;

            for (TextLine ocrLine : lines) {
                i++;

                String value = ocrLine.getValue();
                float fontSize = ocrLine.getFirstWord().getFontInfo().pointsize;
                PixelRectangle box = ocrLine.getBounds();
                logger.fine("ocrLine:{0} {1} {2} w:{3} h:{4} aspect:{5}",
                            i, box, ocrLine.toString(),
                            box.width / (fontSize * value.length()),
                            box.height / fontSize,
                            ((float) box.height * value.length()) / box.width);
            }
        }

        // Tests on OCR results
        if (lines.isEmpty()) {
            logger.fine("No line found");

            return null;
        }

        if (lines.size() > 1) {
            logger.fine("More than 1 line found");

            return null;
        }

        TextLine ocrLine = lines.get(0);

        String value = ocrLine.getValue();
        float fontSize = ocrLine.getFirstWord().getFontInfo().pointsize;
        PixelRectangle box = ocrLine.getBounds();

        if (box.height == 0) {
            logger.fine("OCR found nothing");

            return null;
        }

        if (fontSize > maxFontSize) {
            logger.fine("Font size {0} exceeds maximum {1}",
                        fontSize, maxFontSize);

            return null;
        }

        final float aspect = ((float) box.height * value.length()) / box.width;
        final double minAspect = constants.minAspect.getValue();

        if (aspect < minAspect) {
            logger.fine("Char aspect {0} lower than minimum {1}",
                        aspect, minAspect);

            return null;
        }

        final double maxAspect = constants.maxAspect.getValue();

        if (aspect > maxAspect) {
            logger.fine("Char aspect {0} exceeds maximum {1}",
                        aspect, maxAspect);

            return null;
        }

        // All tests are OK
        return ocrLine;
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction maxFontSize = new Scale.Fraction(
                3.0,
                "Maximum value for text font size");

        Constant.Ratio minAspect = new Constant.Ratio(
                0.5,
                "Minimum aspect of chars (height / width)");

        Constant.Ratio maxAspect = new Constant.Ratio(
                3.0,
                "Maximum aspect of chars (height / width)");

        Scale.Fraction staffMarginAbove = new Scale.Fraction(
                1.0,
                "Minimum distance above staff");

        Scale.Fraction staffMarginBelow = new Scale.Fraction(
                3.0,
                "Minimum distance below staff");

        //
        Evaluation.Grade maxGrade = new Evaluation.Grade(
                80.0,
                "Maximum grade for calling known glyphs into question");
    }

    //----------//
    // MyRegion //
    //----------//
    private class MyRegion
            extends Region
    {
        //~ Constructors -------------------------------------------------------

        public MyRegion (String name,
                         Polygon polygon)
        {
            super(name, polygon);
        }

        //~ Methods ------------------------------------------------------------
        //-----------//
        // checkBlob //
        //-----------//
        @Override
        protected boolean checkBlob (TextBlob blob,
                                     Glyph compound)
        {
            // Call the OCR?
            if (TextBuilder.getOcr().isAvailable()) {
                TextBuilder textBuilder = system.getTextBuilder();
                TextLine ocrLine = null;

                if (compound.isTransient()) {
                    compound = system.registerGlyph(compound);
                }

                if (compound.getTextValue() == null) {
                    String language = system.getScoreSystem().getScore().
                            getLanguage();

                    ocrLine = borderOCR(compound, language);
                    logger.fine("OCR on {0} {1}", blob, ocrLine);

                    if (ocrLine == null) {
                        return false;
                    }
                } else {
                    ///////////////////////////////////////////////////////////////////////////////////////////////ocrLine = compound.getTextLine();
                }

                if (!textBuilder.isValid(compound, ocrLine)) {
                    logger.fine("Invalid blob {0} {1}", ocrLine, compound);

                    return false;
                }

                // OK
                compound = system.addGlyph(compound);
                system.computeGlyphFeatures(compound);
                compound.setShape(Shape.TEXT, Evaluation.ALGORITHM);
                logger.fine("Border {0} => \"{1}\"",
                            compound.idString(), ocrLine.getValue());

                return true;
            } else {
                return true; // By default (with no OCR call...)
            }
        }

        //----------------//
        // checkCandidate //
        //----------------//
        @Override
        protected boolean checkCandidate (Glyph glyph)
        {
            // Common checks
            if (!super.checkCandidate(glyph)) {
                return false;
            }
            
            if (glyph.isStem()) {
                return false;
            }

            // We remove candidates that are stuck to a stem that goes into a
            // staff because these glyphs are not likely to be text items
            for (HorizontalSide side : HorizontalSide.values()) {
                Glyph stem = glyph.getStem(side);
                
                if (stem != null) { //&& stem.getBounds().intersects(systemBox)) {
                    return false;
                }
            }
            
            return true;
        }
    }
}
