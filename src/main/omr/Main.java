//----------------------------------------------------------------------------//
//                                                                            //
//                                  M a i n                                   //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.score.Score;
import omr.score.ScoreManager;
import omr.score.visitor.ScoreExporter;

import omr.sheet.Sheet;
import omr.sheet.SheetManager;

import omr.ui.Jui;
import omr.ui.util.UILookAndFeel;

import omr.util.Clock;
import omr.util.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.swing.*;

/**
 * Class <code>Main</code> is the main class for OMR application. It deals with
 * the main routine and its command line parameters.  It launches the User
 * Interface, unless a batch mode is selected.
 *
 * <p> The command line parameters can be (order not relevant) : <dl>
 *
 * <dt> <b>-help</b> </dt> <dd> to print a quick usage help and leave the
 * application. </dd>
 *
 * <dt> <b>-batch</b> </dt> <dd> to run in batch mode, with no user
 * interface. </dd>
 *
 * <dt> <b>-write</b> </dt> <dd> to specify that the resulting score has to be
 * written down once the specified step has been reached. This feature is
 * available in batch mode only. </dd>
 *
 * <dt> <b>-save SAVEPATH</b> </dt> <dd> to specify the directory where score
 * output files are saved. If not specified, files are simply written to the
 * 'save' sub-directory of the application. </dd>
 *
 * <dt> <b>-sheet (SHEETNAME | &#64;SHEETLIST)+</b> </dt> <dd> to specify some
 * sheets to be read, either by naming the image file or by referencing (flagged
 * by a &#64; sign) a file that lists image files (or even other files list
 * recursively). A list file is a simple text file, with one image file name per
 * line.</dd>
 *
 * <dt> <b>-score (SCORENAME | &#64;SCORELIST)+</b> </dt> <dd> to specify some
 * scores to be read, using the same mechanism than sheets. These score files
 * contain binary data in saved during a previous run.</dd>
 *
 * <dt> <b>-step STEPNAME</b> </dt> <dd> to run till the specified
 * step. 'STEPNAME' can be any one of the step names (the case is irrelevant) as
 * defined in the {@link omr.sheet.Sheet} class.
 *
 * </dd> </dl>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Main
{
    //~ Static fields/initializers ---------------------------------------------

    static {
        // Time stamps
        Clock.resetTime();
    }

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Main.class);

    /** Installation container and folder */
    private static File container;

    /** Installation container and folder */
    private static File homeFolder;

    /** Singleton */
    private static Main INSTANCE;

    /** Specific folder name for icons */
    public static final String ICONS_NAME = "icons";

    //~ Instance fields --------------------------------------------------------

    /** Build reference of the application as displayed to the user */
    private final String toolBuild;

    /** Name of the application as displayed to the user */
    private final String toolName;

    /** Version of the application as displayed to the user */
    private final String toolVersion;

    /** Master View */
    private Jui jui;

    /** List of score file names to process */
    private List<String> scoreNames = new ArrayList<String>();

    /** List of sheet file names to process */
    private List<String> sheetNames = new ArrayList<String>();

    /** Target step */
    private Step targetStep;

    /** Batch mode if any */
    private boolean batchMode = false;

    /** Request to write score if any */
    private boolean writeScore = false;

    //~ Constructors -----------------------------------------------------------

    //------//
    // Main //
    //------//
    private Main (String[] args,
                  Class    caller)
    {
        // Locale  to be used in the whole application ?
        checkLocale();

        // Tool name
        final Package thisPackage = Main.class.getPackage();
        final String  name = thisPackage.getSpecificationTitle();

        if (name != null) {
            toolName = name;
            constants.toolName.setValue(name);
        } else {
            toolName = constants.toolName.getValue();
        }

        // Tool version
        final String version = thisPackage.getSpecificationVersion();

        if (version != null) {
            toolVersion = version;
            constants.toolVersion.setValue(version);
        } else {
            toolVersion = constants.toolVersion.getValue();
        }

        // Tool build
        toolBuild = thisPackage.getImplementationVersion();

        // Remember installation home
        container = new File(
            caller.getProtectionDomain().getCodeSource().getLocation().getFile());

        // Home Folder
        // .../build/classes
        // .../dist/audiveris.jar
        homeFolder = container.getParentFile()
                              .getParentFile();
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // getConfigFolder //
    //-----------------//
    /**
     * Report the folder where config parameters are stored
     *
     * @return the directory for configuration files
     */
    public static File getConfigFolder ()
    {
        return new File(getHomeFolder(), "config");
    }

    //----------------//
    // getIconsFolder //
    //----------------//
    /**
     * Report the folder where custom-defined icons are stored
     *
     * @return the directory for icon files
     */
    public static File getIconsFolder ()
    {
        return new File(getHomeFolder(), ICONS_NAME);
    }

    //--------//
    // getJui //
    //--------//
    /**
     * Points to the single instance of the User Interface, if any.
     *
     * @return Jui instance, which may be null
     */
    public static Jui getJui ()
    {
        if (INSTANCE == null) {
            return null;
        } else {
            return INSTANCE.jui;
        }
    }

    //-----------------//
    // getOutputFolder //
    //-----------------//
    /**
     * Report the folder defined for output/saved files
     *
     * @return the directory for output
     */
    public static String getOutputFolder ()
    {
        String saveDir = constants.savePath.getValue();

        if (saveDir.equals("")) {
            // Use default save directory
            return getHomeFolder() + "/save";
        } else {
            // Make sure that it ends with proper separator
            if (!(saveDir.endsWith("\\") || saveDir.endsWith("/"))) {
                saveDir = saveDir + "/";
            }

            return saveDir;
        }
    }

    //--------------//
    // getToolBuild //
    //--------------//
    /**
     * Report the build reference of the application as displayed to the user
     *
     * @return Build reference of the application
     */
    public static String getToolBuild ()
    {
        return INSTANCE.toolBuild;
    }

    //-------------//
    // getToolName //
    //-------------//
    /**
     * Report the name of the application as displayed to the user
     *
     * @return Name of the application
     */
    public static String getToolName ()
    {
        return INSTANCE.toolName;
    }

    //----------------//
    // getToolVersion //
    //----------------//
    /**
     * Report the version of the application as displayed to the user
     *
     * @return version of the application
     */
    public static String getToolVersion ()
    {
        return INSTANCE.toolVersion;
    }

    //----------------//
    // getTrainFolder //
    //----------------//
    /**
     * Report the folder defined for training files
     *
     * @return the directory for training material
     */
    public static File getTrainFolder ()
    {
        return new File(getHomeFolder(), "train");
    }

    //------//
    // main //
    //------//
    /**
     * Specific starting method for the application.
     *
     * @param args the command line parameters
     * @param caller the precise class of the caller
     *
     * @see omr.Main the possible command line parameters
     */
    public static void main (String[] args,
                             Class    caller)
    {
        // Problem, from Emacs all args are passed in one string sequence.  We
        // recognize this by detecting a single argument starting with '-'
        if ((args.length == 1) && (args[0].startsWith("-"))) {
            // Redispatch the real args
            StringTokenizer st = new StringTokenizer(args[0]);
            int             argNb = 0;

            // First just count the number of real arguments
            while (st.hasMoreTokens()) {
                argNb++;
                st.nextToken();
            }

            String[] newArgs = new String[argNb];

            // Second copy all real arguments into newly
            // allocated array
            argNb = 0;
            st = new StringTokenizer(args[0]);

            while (st.hasMoreTokens()) {
                newArgs[argNb++] = st.nextToken();
            }

            // Fake the args
            args = newArgs;
        }

        // Launch the processing
        INSTANCE = new Main(args, caller);

        if (logger.isFineEnabled()) {
            logger.fine("homeFolder=" + homeFolder);
            logger.fine("container=" + container);
            logger.fine("isDirectory=" + container.isDirectory());
        }

        try {
            INSTANCE.process(args);
        } catch (Main.StopRequired ex) {
            logger.info("Exiting.");
        }
    }

    //---------------//
    // getHomeFolder //
    //---------------//
    private static File getHomeFolder ()
    {
        return homeFolder;
    }

    //--------//
    // addRef //
    //--------//
    private void addRef (String       ref,
                         List<String> list)
    {
        // The ref may be a plain file name or the name of a pack that lists
        // ref(s). This is signalled by a starting '@' character in ref
        if (ref.startsWith("@")) {
            // File with other refs inside
            String pack = ref.substring(1);

            try {
                BufferedReader br = new BufferedReader(new FileReader(pack));
                String         newRef;

                try {
                    while ((newRef = br.readLine()) != null) {
                        addRef(newRef.trim(), list);
                    }

                    br.close();
                } catch (IOException ex) {
                    logger.warning(
                        "IO error while reading file '" + pack + "'");
                }
            } catch (FileNotFoundException ex) {
                logger.warning("Cannot find file '" + pack + "'");
            }
        } else
        // Plain file name
        if (ref.length() > 0) {
            list.add(ref);
        }
    }

    //--------//
    // browse //
    //--------//
    private void browse ()
    {
        // Browse desired sheets
        for (String name : sheetNames) {
            File file = new File(name);

            // We do not register the sheet target, since there may be several
            // in a row.  But we perform all steps through the desired step
            targetStep.perform(null, file);

            // Batch part?
            if (batchMode) {
                // Do we have to write down the score?
                if (writeScore) {
                    ScoreManager.getInstance()
                                .exportAll();
                }

                // Dispose allocated stuff
                SheetManager.getInstance()
                            .closeAll();
                ScoreManager.getInstance()
                            .closeAll();
            }
        }

        // Browse desired scores
        for (String name : scoreNames) {
            Score score = ScoreManager.getInstance()
                                      .load(new File(name));

            if (!batchMode) {
                Main.getJui().scoreController.setScoreView(score);
            }
        }
    }

    //-------------//
    // checkLocale //
    //-------------//
    private void checkLocale ()
    {
        final String country = constants.localeCountry.getValue();

        if (!country.equals("")) {
            for (Locale locale : Locale.getAvailableLocales()) {
                if (locale.getCountry()
                          .equals(country)) {
                    Locale.setDefault(locale);

                    return;
                }
            }

            logger.info("Cannot set locale country to " + country);
        }
    }

    //----------------//
    // parseArguments //
    //----------------//
    private void parseArguments (final String[] args)
        throws StopRequired
    {
        // Status of the finite state machine
        final int STEP = 0;
        final int SHEET = 1;
        final int SCORE = 2;
        final int SAVE = 3;
        boolean   paramNeeded = false; // Are we expecting a param?
        int       status = SHEET; // By default
        String    currentCommand = null;

        // Parse all arguments from command line
        for (int i = 0; i < args.length; i++) {
            String token = args[i];

            if (token.startsWith("-")) {
                // This is a command
                // Check that we were not expecting param(s)
                if (paramNeeded) {
                    printCommandLine(args);
                    stopUsage(
                        "Found no parameter after command '" + currentCommand +
                        "'");
                }

                if (token.equalsIgnoreCase("-help")) {
                    stopUsage(null);
                } else if (token.equalsIgnoreCase("-batch")) {
                    batchMode = true;
                    paramNeeded = false;
                } else if (token.equalsIgnoreCase("-write")) {
                    writeScore = true;
                    paramNeeded = false;
                } else if (token.equalsIgnoreCase("-step")) {
                    status = STEP;
                    paramNeeded = true;
                } else if (token.equalsIgnoreCase("-sheet")) {
                    status = SHEET;
                    paramNeeded = true;
                } else if (token.equalsIgnoreCase("-score")) {
                    status = SCORE;
                    paramNeeded = true;
                } else if (token.equalsIgnoreCase("-save")) {
                    status = SAVE;
                    paramNeeded = true;
                } else {
                    printCommandLine(args);
                    stopUsage("Unknown command '" + token + "'");
                }

                // Remember the current command
                currentCommand = token;
            } else {
                // This is a parameter
                switch (status) {
                case STEP :
                    // Read a step name
                    targetStep = null;

                    for (Step step : Sheet.getSteps()) {
                        if (token.equalsIgnoreCase(step.toString())) {
                            targetStep = step;

                            break;
                        }
                    }

                    if (targetStep == null) {
                        printCommandLine(args);
                        stopUsage(
                            "Step name expected, found '" + token +
                            "' instead");
                    }

                    // By default, sheets are now expected
                    status = SHEET;
                    paramNeeded = false;

                    break;

                case SHEET :
                    addRef(token, sheetNames);
                    paramNeeded = false;

                    break;

                case SCORE :
                    addRef(token, scoreNames);
                    paramNeeded = false;

                    break;

                case SAVE :

                    // Make sure that it ends with proper separator
                    if (!(token.endsWith("\\") || token.endsWith("/"))) {
                        token = token + "/";
                    }

                    constants.savePath.setValue(token);

                    // By default, sheets are now expected
                    status = SHEET;
                    paramNeeded = false;

                    break;
                }
            }
        }

        // Additional error checking
        if (paramNeeded) {
            printCommandLine(args);
            stopUsage(
                "Expecting a parameter after command '" + currentCommand + "'");
        }

        // Results
        if (logger.isFineEnabled()) {
            logger.fine("batchMode=" + batchMode);
            logger.fine("writeScore=" + writeScore);
            logger.fine("savePath=" + constants.savePath.getValue());
            logger.fine("targetStep=" + targetStep);
            logger.fine("sheetNames=" + sheetNames);
            logger.fine("scoreNames=" + scoreNames);
        }
    }

    //------------------//
    // printCommandLine //
    //------------------//
    private void printCommandLine (String[] args)
    {
        System.out.println("\nCommandParameters:");

        for (String arg : args) {
            System.out.print(" " + arg);
        }

        System.out.println();
    }

    //---------//
    // process //
    //---------//
    private void process (String[] args)
        throws StopRequired
    {
        // First parse the provided arguments if any
        parseArguments(args);

        // Interactive or Batch mode ?
        if (!batchMode) {
            logger.fine("Interactive processing");

            // UI Look and Feel
            UILookAndFeel.setUI(null);

            // Make sure we have nice window decorations.
            JFrame.setDefaultLookAndFeelDecorated(true);

            // Launch the GUI
            jui = new Jui();

            // Do we have sheet or score actions specified?
            if ((sheetNames.size() > 0) || (scoreNames.size() > 0)) {
                Worker worker = new Worker();
                worker.setName(getClass().getName());
                // Make sure the Gui gets priority
                worker.setPriority(Thread.MIN_PRIORITY);
                worker.start();
            }

            // Background task : JaxbContext
            ScoreExporter.preloadJaxbContext();
        } else {
            logger.info("Batch processing");
            browse();
        }
    }

    //-----------//
    // stopUsage //
    //-----------//
    private void stopUsage (String msg)
        throws StopRequired
    {
        // Print message if any
        if (msg != null) {
            logger.warning(msg);
        }

        StringBuffer buf = new StringBuffer(1024);

        // Print standard command line syntax
        buf.append("usage: java ")
           .append(getToolName())
           .append(" [-help]")
           .append(" [-batch]")
           .append(" [-write]")
           .append(" [-save SAVEPATH]")
           .append(" [-step STEPNAME]")
           .append(" [-sheet (SHEETNAME|@SHEETLIST)+]")
           .append(" [-score (SCORENAME|@SCORELIST)+]");

        // Print all allowed step names
        buf.append("\n      Known step names are in order")
           .append(" (non case-sensitive) :");

        for (Step step : Sheet.getSteps()) {
            buf.append(
                String.format(
                    "%n%-17s : %s",
                    step.toString().toUpperCase(),
                    step.getDescription()));
        }

        logger.info(buf.toString());

        // Stop application immediately
        throw new StopRequired();
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        /** Selection of locale country code (2 letters), or empty */
        Constant.String localeCountry = new Constant.String(
            "FR",
            "Locale country to be used in the whole application (US, FR, ...)");

        /** Directory for saved files, defaulted to 'save' audiveris subdir */
        Constant.String savePath = new Constant.String(
            "",
            "Directory for saved files, defaulted to 'save' audiveris subdir");

        /** Utility constant */
        Constant.String toolName = new Constant.String(
            "Audiveris",
            "* DO NOT EDIT * - Name of this application");

        /** Utility constant */
        Constant.String toolVersion = new Constant.String(
            "",
            "* DO NOT EDIT * - Version of this application");
    }

    //--------------//
    // StopRequired //
    //--------------//
    private static class StopRequired
        extends Exception
    {
    }

    //--------//
    // Worker //
    //--------//
    private class Worker
        extends Thread
    {
        //-----//
        // run //
        //-----//
        @Override
        public void run ()
        {
            browse();
        }
    }
}
