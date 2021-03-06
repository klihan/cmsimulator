package fiitstu.gulis.cmsimulator.database;

import android.content.res.AssetManager;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Environment;
import android.provider.ContactsContract;
import android.sax.RootElement;
import android.support.v4.util.LongSparseArray;
import android.util.Log;

import android.util.Pair;
import fiitstu.gulis.cmsimulator.BuildConfig;
import fiitstu.gulis.cmsimulator.models.ChessGame;
import fiitstu.gulis.cmsimulator.models.tasks.automata_type;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import fiitstu.gulis.cmsimulator.R;
import fiitstu.gulis.cmsimulator.activities.GrammarActivity;
import fiitstu.gulis.cmsimulator.app.CMSimulator;
import fiitstu.gulis.cmsimulator.elements.*;

import java.io.*;
import java.sql.Time;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import fiitstu.gulis.cmsimulator.activities.MainActivity;

/**
 * A class that allows serializing and deserializing data from XML and reading and
 * writing the XML to files.
 * <p>
 * Created by Martin on 7. 3. 2017.
 */
public class FileHandler {

    /**
     * Contains the file names of the built-in example machines
     */
    public static class Examples {

        public static final String DFSA = "FSA.cms";
        public static final String DFSA_AN = "FSA_a^n.cms";
        public static final String NFSA = "NFSA.cms";
        public static final String DPDA = "PDA.cms";
        public static final String DPDA_ANBN = "PDA_a^nb^n.cms";
        public static final String NPDA = "NPDA.cms";
        public static final String DLBA = "LBA.cms";
        public static final String NLBA = "NLBA.cms";
        public static final String DTM = "TM.cms";
        public static final String NTM = "NTM.cms";
    }

    /**
     * Contains the file names of the built-in example grammars
     */
    public static class GrammarExamples {

        public static final String GREG_AN = "Greg_a^n.cmsg";
        public static final String GREG_3KPlus1 = "Greg_3k+1.cmsg";
        public static final String GREG_End01Or10 = "Greg_End01Or10.cmsg";
        public static final String LCF_ANBN = "Lcf_a^nb^n.cmsg";
        public static final String LCF_ANBN_CNF = "Lcf_a^nb^n_cnf.cmsg";
        public static final String LCF_WWR = "Lcf_ww^R.cmsg";
        public static final String LCS_ANBNCN = "Lcs_a^nb^nc^n.cmsg";
    }

    //CMS and CMST are generally treated identically, but they have different extensions
    //so the users can easily tell which saved files are tasks and which are plain automatons
    public enum Format {
        /**
         * CMSimulator automaton
         */
        CMS,

        /**
         * CMSimulator automaton with task
         */
        CMST,

        /**
         * CMSimulator game
         */
        CMSC,

        /**
         * JFLAP automaton
         */
        JFF,

        /**
         * CMSimulator grammar
         */
        CMSG;

        public String getExtension() {
            return "." + name().toLowerCase();
        }

        public static Format fromExtension(String extension) {
            switch (extension) {
                case "jff":
                case ".jff":
                    return JFF;
                case "cms":
                case ".cms":
                    return CMS;
                case "cmst":
                case ".cmst":
                    return CMST;
                case "cmsg":
                case ".cmsg":
                    return CMSG;
                case "cmsc":
                case ".cmsc":
                    return CMSC;
                default:
                    return null;
            }
        }

    }

    /**
     * A simple class to represent version of the file
     */
    private class Version {

        //might as well be public, since they are final and the class itself is private
        public final int major;
        public final int minor;

        /**
         * Creates a Version object representing the version of the given root element
         *
         * @param rootElement the root element of the file whose version you want to obtain
         */
        Version(Element rootElement) {
            String versionString = rootElement.getAttribute(VERSION);
            if (versionString.equals("")) {
                major = 1;
                minor = 0;
            } else {
                String[] versionNumbers = versionString.split("\\.");
                major = Integer.parseInt(versionNumbers[0]);
                minor = versionNumbers.length > 1 ? Integer.parseInt(versionNumbers[1]) : 0;
            }
        }

    }

    //log tag
    private static final String TAG = FileHandler.class.getName();

    //path
    public static final String PATH = Environment.getExternalStorageDirectory() + "/" + CMSimulator.getContext().getResources().getString(R.string.default_folder);

    /**
     * Default (latest) version of the .cms file format
     * 1.0 - the original version (number assigned retro-actively)
     * 2.0 - the version used by CMSimulator 3
     */
    private static final String CURRENT_CMS_VERSION = "2.1";

    //xml tag names
    private static final String ROOT = "structure";
    private static final String VERSION = "version";
    private static final String TYPE = "type";
    private static final String AUTOMATON = "automaton";

    private static final String STATE = "state";
    private static final String ID = "id";
    private static final String NAME = "name";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String INITIAL = "initial";
    private static final String FINAL = "final";
    private static final String AUTOMATA_TYPE = "automataType";
    private static final String VALUE = "value";

    private static final String TRANSITION = "transition";
    private static final String FROM = "from";
    private static final String TO = "to";
    private static final String READ = "read";
    private static final String POP = "pop";
    private static final String PUSH = "push";
    private static final String WRITE = "write";
    private static final String MOVE = "move";

    private static final String INPUT_ALPHABET = "input_alphabet";
    private static final String STACK_ALPHABET = "stack_alphabet";
    private static final String TAPE = "tape";
    private static final String SYMBOL = "symbol";
    private static final String PROPERTIES = "properties";
    private static final String STATES = "states";
    private static final String TRANSITIONS = "transitions";

    private final static String TEST_SCENARIOS = "test_scenarios";
    private final static String NEGATIVE_SCENARIOS = "negative_scenarios";
    private final static String TEST_SCENARIO = "test_scenario";
    private final static String INPUT_WORD = "input_word";
    private final static String OUTPUT_WORD = "output_word";

    private static final String TASK = "task";
    private static final String TITLE = "title";
    private static final String TEXT = "text";
    private static final String TIME = "time";
    private static final String PUBLIC_INPUTS = "public_inputs";
    private static final String MAX_STEPS = "max_steps";
    private static final String TASK_RESULT_VERSION = "result_version";

    private static final String GRAMMAR = "grammar";
    private static final String RULE = "rule";
    private static final String LEFT = "left";
    private static final String RIGHT = "right";
    private static final String PRODUCTION = "production";

    // game attributes
    private static final String CHESS_GAME = "chess_game";
    private static final String START_FIELD = "start_field";
    private static final String FINISH_FIELD = "finish_field";
    private static final String PATH_FIELDS = "path_fields";
    private static final String FIELD = "field";
    private static final String CHESS_FIELD_SIZE = "chess_field_size";
    private static final String MAX_STATE_COUNT = "max_state_count";
    private static final String COUNT = "count";

    //variables
    private Long emptyInputSymbolId;
    private Long startStackSymbolId;

    private Document document;
    private Format format;

    public FileHandler(Format format) {
        this.format = format;
        Log.i(TAG, "new instance created");
    }

    /**
     * Opens a file and builds parse tree
     *
     * @param filePath the path to the file
     * @throws FileFormatException when something goes wrong during the parse
     * @throws IOException         if the file cannot be read
     */
    public void loadFile(String filePath) throws FileFormatException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        //initialize file
        File inputFile = new File(filePath);
        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();
            document = db.parse(inputFile);
        } catch (ParserConfigurationException | SAXException e) {
            throw new FileFormatException(e);
        }
        Log.v(TAG, "file loaded");
    }

    /**
     * Opens a file from assets and builds parse tree
     *
     * @param assetManager the AssetManager that will be used to load the asset
     * @param filename     the name of the asset file
     * @throws FileFormatException when something goes wrong during the parse
     * @throws IOException         if the file cannot be read
     */
    public void loadAsset(AssetManager assetManager, String filename) throws IOException, FileFormatException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        //initialize file
        InputStream inputStream = assetManager.open(filename);
        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();
            document = db.parse(inputStream);
        } catch (ParserConfigurationException | SAXException e) {
            throw new FileFormatException(e);
        }
        Log.v(TAG, "file asset loaded");
    }

    /**
     * Reads data from the open file and populates the DataSource
     *
     * @param dataSource the DataSource to be populated
     * @throws FileFormatException if the open file contains invalid data
     */
    public void getData(DataSource dataSource) throws FileFormatException {
        if (format == Format.CMS || format == Format.CMST) {
            getDataCMS(dataSource);
        } else if (format == Format.CMSG) {
            getDataCMSG(dataSource);
        } else if (format == Format.CMSC) {
            getDataCMSC(dataSource);
        } else {
            getDataJFF(dataSource);
        }
    }

    public ChessGame getChessGame() {
        Pair<Integer, Integer> startField;
        Pair<Integer, Integer> finishField;
        List<Pair<Integer, Integer>> pathList = new ArrayList<>();
        Pair<Integer, Integer> fieldSize;
        int imaxStateCount;
        automata_type automata_type;

        // PATH
        NodeList nodeList = document.getElementsByTagName(PATH_FIELDS);
        Node node = nodeList.item(0);
        NodeList fields = node.getChildNodes();
        final int fieldsSize = fields.getLength();
        for (int i = 0; i < fieldsSize; i++) {
            Node field = fields.item(i);
            NamedNodeMap namedNodeMap = field.getAttributes();
            if (namedNodeMap == null)
                continue;
            Node x = namedNodeMap.getNamedItem(X);
            String s_x = x.getNodeValue();

            Node y = namedNodeMap.getNamedItem(Y);
            String s_y = y.getNodeValue();

            Pair<Integer, Integer> currentField = new Pair<>(Integer.parseInt(s_x), Integer.parseInt(s_y));
            pathList.add(currentField);
        }

        // START FIELD
        NodeList startList = document.getElementsByTagName(START_FIELD);
        Node startNode = startList.item(0);
        NamedNodeMap startAttr = startNode.getAttributes();

        Node startX = startAttr.getNamedItem(X);
        Node startY = startAttr.getNamedItem(Y);

        String startXS = startX.getNodeValue();
        String startYS = startY.getNodeValue();

        startField = new Pair<>(Integer.parseInt(startXS), Integer.parseInt(startYS));

        // FINISH FIELD
        NodeList finishList = document.getElementsByTagName(FINISH_FIELD);
        Node finishNode = finishList.item(0);
        NamedNodeMap finishAttr = finishNode.getAttributes();

        Node finishX = finishAttr.getNamedItem(X);
        Node finishY = finishAttr.getNamedItem(Y);

        String finishXS = finishX.getNodeValue();
        String finishYS = finishY.getNodeValue();

        finishField = new Pair<>(Integer.parseInt(finishXS), Integer.parseInt(finishYS));

        // CHESS FIELD SIZE
        NodeList chessFieldList = document.getElementsByTagName(CHESS_FIELD_SIZE);
        Node chessFieldNode = chessFieldList.item(0);
        NamedNodeMap chessFieldAttr = chessFieldNode.getAttributes();

        Node chessFieldX = chessFieldAttr.getNamedItem(X);
        Node chessFieldY = chessFieldAttr.getNamedItem(Y);

        String chessFieldXS = chessFieldX.getNodeValue();
        String chessFieldYS = chessFieldY.getNodeValue();

        fieldSize = new Pair<>(Integer.parseInt(chessFieldXS), Integer.parseInt(chessFieldYS));

        // MAX STATE COUNT
        NodeList maxStateList = document.getElementsByTagName(MAX_STATE_COUNT);
        Node maxStateNode = maxStateList.item(0);
        NamedNodeMap maxStateAttr = maxStateNode.getAttributes();

        Node maxStateCount = maxStateAttr.getNamedItem(COUNT);

        String maxStateCountS = maxStateCount.getNodeValue();
        imaxStateCount = Integer.parseInt(maxStateCountS);

        // AUTOMATA TYPE
        NodeList automataTypeNodeList = document.getElementsByTagName(AUTOMATA_TYPE);
        Node automataTypeNode = automataTypeNodeList.item(0);
        NamedNodeMap automataTypeNamedNodeMap = automataTypeNode.getAttributes();

        Node valueNode = automataTypeNamedNodeMap.getNamedItem(VALUE);
        String automataTypeString = valueNode.getNodeValue();

        if (automataTypeString.equals(fiitstu.gulis.cmsimulator.models.tasks.automata_type.FINITE_AUTOMATA.getApiKey()))
            automata_type = fiitstu.gulis.cmsimulator.models.tasks.automata_type.FINITE_AUTOMATA;
        else
            automata_type = fiitstu.gulis.cmsimulator.models.tasks.automata_type.PUSHDOWN_AUTOMATA;

        // CREATE CHESS GAME
        ChessGame chessGame = new ChessGame(startField, finishField, pathList, fieldSize, imaxStateCount, automata_type);
        return chessGame;
    }

    private void getDataCMSC(DataSource dataSource) {
        Log.v(TAG, "getDataCMSC method started");

        dataSource.open();
        dataSource.globalDrop();

        // PATH_FIELDS

        {
            List<Pair<Integer, Integer>> pathList = new ArrayList<>();
            NodeList nodeList = document.getElementsByTagName(PATH_FIELDS);
            Node node = nodeList.item(0);
            NodeList fields = node.getChildNodes();
            final int fieldsSize = fields.getLength();
            for (int i = 0; i < fieldsSize; i++) {
                Node field = fields.item(i);
                NamedNodeMap namedNodeMap = field.getAttributes();
                if (namedNodeMap == null)
                    continue;
                Node x = namedNodeMap.getNamedItem(X);
                String s_x = x.getNodeValue();

                Node y = namedNodeMap.getNamedItem(Y);
                String s_y = y.getNodeValue();

                Pair<Integer, Integer> currentField = new Pair<>(Integer.parseInt(s_x), Integer.parseInt(s_y));
                dataSource.addPathField(currentField);
                pathList.add(currentField);
            }
        }

        // START FIELD
        {
            NodeList startList = document.getElementsByTagName(START_FIELD);
            Node startNode = startList.item(0);
            NamedNodeMap startAttr = startNode.getAttributes();

            Node startX = startAttr.getNamedItem(X);
            Node startY = startAttr.getNamedItem(Y);

            String startXS = startX.getNodeValue();
            String startYS = startY.getNodeValue();

            Pair<Integer, Integer> startField = new Pair<>(Integer.parseInt(startXS), Integer.parseInt(startYS));
            dataSource.setStartField(startField);
        }

        // FINISH FIELD
        {
            NodeList finishList = document.getElementsByTagName(FINISH_FIELD);
            Node finishNode = finishList.item(0);
            NamedNodeMap finishAttr = finishNode.getAttributes();

            Node finishX = finishAttr.getNamedItem(X);
            Node finishY = finishAttr.getNamedItem(Y);

            String finishXS = finishX.getNodeValue();
            String finishYS = finishY.getNodeValue();

            Pair<Integer, Integer> finishField = new Pair<>(Integer.parseInt(finishXS), Integer.parseInt(finishYS));
            dataSource.setFinishField(finishField);
        }

        // CHESS FIELD SIZE
        {
            NodeList chessFieldList = document.getElementsByTagName(CHESS_FIELD_SIZE);
            Node chessFieldNode = chessFieldList.item(0);
            NamedNodeMap chessFieldAttr = chessFieldNode.getAttributes();

            Node chessFieldX = chessFieldAttr.getNamedItem(X);
            Node chessFieldY = chessFieldAttr.getNamedItem(Y);

            String chessFieldXS = chessFieldX.getNodeValue();
            String chessFieldYS = chessFieldY.getNodeValue();

            Pair<Integer, Integer> chessFieldField = new Pair<>(Integer.parseInt(chessFieldXS), Integer.parseInt(chessFieldYS));
            dataSource.setFieldSize(chessFieldField);
        }

        // MAX STATE COUNT
        {
            NodeList maxStateList = document.getElementsByTagName(MAX_STATE_COUNT);
            Node maxStateNode = maxStateList.item(0);
            NamedNodeMap maxStateAttr = maxStateNode.getAttributes();

            Node maxStateCount = maxStateAttr.getNamedItem(COUNT);

            String maxStateCountS = maxStateCount.getNodeValue();

            int intMaxStateField = Integer.parseInt(maxStateCountS);


            dataSource.setMaxStates(intMaxStateField);
        }

        // INPUT SYMBOLS
        {
            List<Symbol> listOfSymbols = ChessGame.getMovementSymbolList();
            for (Symbol s : listOfSymbols) {
                dataSource.addInputSymbol(s.getValue(), 0);
            }
            Symbol empty = dataSource.addInputSymbol(Symbol.EMPTY_SYMBOL, Symbol.EMPTY);
            emptyInputSymbolId = empty.getId();
        }

        // AUTOMATA_TYPE
        automata_type autotype = null;
        {
            NodeList automataTypeNodeList = document.getElementsByTagName(AUTOMATA_TYPE);
            Node automataTypeNode = automataTypeNodeList.item(0);
            NamedNodeMap map = automataTypeNode.getAttributes();
            final int size = map.getLength();
            for (int i = 0; i < size; i++) {
                Node type = map.getNamedItem(VALUE);
                String s_type = type.getNodeValue();

                switch (s_type) {
                    case "pushdown_automata":
                        autotype = automata_type.PUSHDOWN_AUTOMATA;
                        break;
                    default:
                    case "finite_automata":
                        autotype = automata_type.FINITE_AUTOMATA;
                        break;
                }
            }
        }


        // STATES
        List<State> stateList = new ArrayList<>();
        LongSparseArray<Long> stateIdMap = new LongSparseArray<>();
        LongSparseArray<State> stateMap = new LongSparseArray<>();
        {
            NodeList statesNodeList = document.getElementsByTagName(STATES);
            Node StateNode = statesNodeList.item(0);
            NodeList states = null;
            try {
                states = StateNode.getChildNodes();
            } catch (Exception e) {
                e.printStackTrace();
                dataSource.close();
                return;
            }
            final int statesSize = states.getLength();
            for (int i = 0; i < statesSize; i++) {
                Node field = states.item(i);
                NamedNodeMap namedNodeMap = field.getAttributes();
                if (namedNodeMap == null)
                    continue;
                Node id = namedNodeMap.getNamedItem(ID);
                String s_id = id.getNodeValue();

                Node x = namedNodeMap.getNamedItem(X);
                String s_x = x.getNodeValue();

                Node y = namedNodeMap.getNamedItem(Y);
                String s_y = y.getNodeValue();

                Node initial = namedNodeMap.getNamedItem(INITIAL);
                String s_initial = initial.getNodeValue();

                Node finalState = namedNodeMap.getNamedItem(FINAL);
                String s_final = finalState.getNodeValue();

                Node name = namedNodeMap.getNamedItem(NAME);
                String s_name = name.getNodeValue();

                State state = dataSource.addState(s_name, Integer.parseInt(s_x), Integer.parseInt(s_y), Boolean.parseBoolean(s_initial), Boolean.parseBoolean(s_final));
                stateIdMap.append(Long.parseLong(s_id), state.getId());
                stateMap.append(state.getId(), state);
                stateList.add(state);
            }
        }

        // STACK ALPHABET
        LongSparseArray<Symbol> alphabetMap = new LongSparseArray<>();
        {
            if (autotype == automata_type.PUSHDOWN_AUTOMATA) {
                NodeList stackAlphabetNodeList = document.getElementsByTagName(STACK_ALPHABET);
                Node stackAlphabetNode = stackAlphabetNodeList.item(0);
                NodeList symbols = stackAlphabetNode.getChildNodes();
                final int symbolsSize = symbols.getLength();
                for (int i = 0; i < symbolsSize; i++) {
                    Node symbol = symbols.item(i);
                    NamedNodeMap symbolAttrs = symbol.getAttributes();
                    if (symbolAttrs == null)
                        continue;
                    Node value = symbolAttrs.getNamedItem(VALUE);
                    String s_value = value.getNodeValue();

                    Node property = symbolAttrs.getNamedItem(PROPERTIES);
                    String s_property = property.getNodeValue();

                    Symbol s = dataSource.addStackSymbol(s_value, Integer.parseInt(s_property));
                    alphabetMap.append(s.getId(), s);
                }
            }


            // TRANSITION
            {
                List<Transition> transitionList = new ArrayList<>();
                NodeList statesNodeList = document.getElementsByTagName(TRANSITIONS);
                Node StateNode = statesNodeList.item(0);
                NodeList states = null;
                try {
                    states = StateNode.getChildNodes();
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                final int statesSize = states.getLength();
                for (int i = 0; i < statesSize; i++) {
                    Node field = states.item(i);
                    NamedNodeMap namedNodeMap = field.getAttributes();
                    if (namedNodeMap == null)
                        continue;
                    Node from = namedNodeMap.getNamedItem(FROM);
                    String s_from = from.getNodeValue();

                    Node to = namedNodeMap.getNamedItem(TO);
                    String s_to = to.getNodeValue();

                    Node read = namedNodeMap.getNamedItem(READ);
                    String s_read = read.getNodeValue();

                    long newFromId = stateIdMap.get(Long.parseLong(s_from));
                    long newToId = stateIdMap.get(Long.parseLong(s_to));
                    State fromState = stateMap.get(newFromId);
                    State toState = stateMap.get(newToId);
                    Symbol readSymbol = getSymbol(s_read, dataSource.getInputAlphabetFullExtract());

                    if (autotype == automata_type.PUSHDOWN_AUTOMATA) {
                        NodeList transitionNodeList = field.getChildNodes();
                        final int transitionListSize = transitionNodeList.getLength();

                        List<Symbol> popSymbols = new ArrayList<>();
                        List<Symbol> pushSymbols = new ArrayList<>();
                        for (int j = 0; j < transitionListSize; j++) {
                            Node symbolNode = transitionNodeList.item(j);
                            NodeList symbolsNodeList = symbolNode.getChildNodes();
                            final int symbolListSize = symbolsNodeList.getLength();
                            for (int k = 0; k < symbolListSize; k++) {
                                Node symbol = symbolsNodeList.item(k);
                                NamedNodeMap symbolAttribs = symbol.getAttributes();
                                if (symbolAttribs == null)
                                    continue;

                                Node id = symbolAttribs.getNamedItem(ID);
                                Node value = symbolAttribs.getNamedItem(VALUE);
                                Node properties = symbolAttribs.getNamedItem(PROPERTIES);

                                Long s_id = Long.parseLong(id.getNodeValue());
                                String s_value = value.getNodeValue();
                                int s_properties = Integer.parseInt(properties.getNodeValue());

                                Symbol s = alphabetMap.get(s_id);
                                String symbolsState = symbolNode.getNodeName();
                                if (symbolsState.equals(POP))
                                    popSymbols.add(s);
                                else if (symbolsState.equals(PUSH))
                                    pushSymbols.add(s);
                            }
                        }
                        PdaTransition t = (PdaTransition) dataSource.addPdaTransition(fromState, readSymbol, toState, emptyInputSymbolId, popSymbols, pushSymbols);
                        transitionList.add(t);
                    } else {
                        FsaTransition f = (FsaTransition) dataSource.addFsaTransition(fromState, readSymbol, toState, emptyInputSymbolId);
                        transitionList.add(f);
                    }
                }
            }
        }

        dataSource.close();
    }

    private State getState(int id, List<State> stateList) {
        for (State s : stateList) {
            if (s.getId() == id)
                return s;
        }

        return null;
    }

    private Symbol getSymbol(String value, List<Symbol> symbolList) {
        for (Symbol s : symbolList) {
            if (s.getValue().equals(value))
                return s;
        }

        return null;
    }

    /**
     * Returns a task stored in the file, or null if there is none
     *
     * @return a task stored in the file, or null if there is none
     */
    public Task getTask() {
        if (format == Format.JFF) {
            //JFLAP does not support tasks
            return null;
        } else {
            Log.v(TAG, "getTask method started");
            Element rootElement = document.getDocumentElement();

            Element taskElement = (Element) rootElement.getElementsByTagName(TASK).item(0);
            if (taskElement == null) {
                return null;
            }

            String title = taskElement.getAttribute(TITLE);
            String text = taskElement.getAttribute(TEXT);
            int minutes = Integer.parseInt(taskElement.getAttribute(TIME));
            boolean publicInputs = Boolean.parseBoolean(taskElement.getAttribute(PUBLIC_INPUTS));
            int maxSteps = Integer.parseInt(taskElement.getAttribute(MAX_STEPS));

            Version version = new Version(rootElement);
            int resultVersion;
            if (version.major == 2 && version.minor == 0) {
                resultVersion = 0;
            } else {
                resultVersion = Integer.parseInt(taskElement.getAttribute(TASK_RESULT_VERSION));
            }

            final String sTime = String.format("00:%02d:00", minutes);
            final Time time = Time.valueOf(sTime);
            return new Task(title, text, time, publicInputs, maxSteps, resultVersion);
        }
    }

    public void writeGame(ChessGame game) {
        Element gameElement = document.createElement(CHESS_GAME);
        document.appendChild(gameElement);

        // PATH ADDING
        Element path = document.createElement(PATH_FIELDS);
        gameElement.appendChild(path);
        for (Pair<Integer, Integer> field : game.getPathFields()) {
            Element _field = document.createElement(FIELD);
            _field.setAttribute(X, field.first.toString());
            _field.setAttribute(Y, field.second.toString());
            path.appendChild(_field);
        }

        // STARTING FIELD
        {
            Element starting_field = document.createElement(START_FIELD);
            starting_field.setAttribute(X, game.getStartField().first.toString());
            starting_field.setAttribute(Y, game.getStartField().second.toString());
            gameElement.appendChild(starting_field);
        }

        // FINISH FIELD
        {
            Element finishing_field = document.createElement(FINISH_FIELD);
            finishing_field.setAttribute(X, game.getFinishField().first.toString());
            finishing_field.setAttribute(Y, game.getFinishField().second.toString());
            gameElement.appendChild(finishing_field);
        }

        // CHESS FIELD SIZE
        {
            Element chess_field_size = document.createElement(CHESS_FIELD_SIZE);
            chess_field_size.setAttribute(X, game.getFieldSize().first.toString());
            chess_field_size.setAttribute(Y, game.getFieldSize().second.toString());
            gameElement.appendChild(chess_field_size);
        }

        // MAX STATE COUNT
        {
            Element max_state_count = document.createElement(MAX_STATE_COUNT);
            max_state_count.setAttribute(COUNT, Integer.toString(game.getMaxStateCount()));
            gameElement.appendChild(max_state_count);
        }

        // AUTOMATA_TYPE
        {
            Element automataType = document.createElement(AUTOMATA_TYPE);
            automataType.setAttribute(VALUE, game.getAutomata_type().getApiKey());
            gameElement.appendChild(automataType);
        }

        // STATES
        {
            Element states = document.createElement(STATES);
            List<State> stateList = game.getListOfStates();

            for (State s : stateList) {
                Element state = document.createElement(STATE);
                state.setAttribute(ID, Long.toString(s.getId()));
                state.setAttribute(NAME, s.getValue());
                state.setAttribute(INITIAL, Boolean.toString(s.isInitialState()));
                state.setAttribute(FINAL, Boolean.toString(s.isFinalState()));
                state.setAttribute(X, Integer.toString(s.getPositionX()));
                state.setAttribute(Y, Integer.toString(s.getPositionY()));
                states.appendChild(state);
            }

            gameElement.appendChild(states);
        }

        // TRANSITIONS
        {
            Element transitions = document.createElement(TRANSITIONS);
            List<Transition> transitionList = game.getListOfTransitions();

            for (Transition t : transitionList) {
                Element transition = document.createElement(TRANSITION);
                if (game.getAutomata_type() == automata_type.PUSHDOWN_AUTOMATA) {
                    PdaTransition trans = (PdaTransition) t;

                    transition.setAttribute(ID, Long.toString(t.getId()));
                    transition.setAttribute(FROM, Long.toString(t.getFromState().getId()));
                    transition.setAttribute(TO, Long.toString(t.getToState().getId()));
                    transition.setAttribute(READ, t.getReadSymbol().getValue());
                    StringBuilder popString = new StringBuilder();

                    List<Symbol> listOfPopSymbols = trans.getPopSymbolList();
                    List<Symbol> listOfPushSymbols = trans.getPushSymbolList();

                    Element pop = document.createElement(POP);
                    for (Symbol s : listOfPopSymbols) {
                        Element popSymbol = document.createElement(SYMBOL);
                        popSymbol.setAttribute(ID, Long.toString(s.getId()));
                        popSymbol.setAttribute(VALUE, s.getValue());
                        popSymbol.setAttribute(PROPERTIES, Integer.toString(s.getProperties()));
                        pop.appendChild(popSymbol);
                    }
                    transition.appendChild(pop);

                    Element push = document.createElement(PUSH);
                    for (Symbol s : listOfPushSymbols) {
                        Element pushSymbol = document.createElement(SYMBOL);
                        pushSymbol.setAttribute(ID, Long.toString(s.getId()));
                        pushSymbol.setAttribute(VALUE, s.getValue());
                        pushSymbol.setAttribute(PROPERTIES, Integer.toString(s.getProperties()));
                        push.appendChild(pushSymbol);
                    }
                    transition.appendChild(push);
                } else if (game.getAutomata_type() == automata_type.FINITE_AUTOMATA) {
                    transition.setAttribute(ID, Long.toString(t.getId()));
                    transition.setAttribute(FROM, Long.toString(t.getFromState().getId()));
                    transition.setAttribute(TO, Long.toString(t.getToState().getId()));
                    transition.setAttribute(READ, t.getReadSymbol().getValue());
                }

                transitions.appendChild(transition);
            }
            gameElement.appendChild(transitions);
        }

        // STACK ALPHABET
        {
            Element stackAlphabet = document.createElement(STACK_ALPHABET);
            List<Symbol> stackAlphabetSymbols = game.getStackAlphabet();
            for (Symbol s : stackAlphabetSymbols) {
                Element symbol = document.createElement(SYMBOL);
                symbol.setAttribute(ID, Long.toString(s.getId()));
                symbol.setAttribute(VALUE, s.getValue());
                symbol.setAttribute(PROPERTIES, Integer.toString(s.getProperties()));

                stackAlphabet.appendChild(symbol);
            }
            gameElement.appendChild(stackAlphabet);
        }
    }


    /**
     * Writes a task into the document
     *
     * @param task the task to be written
     */
    public void writeTask(Task task) throws ParserConfigurationException {
        if (document == null) {
            createDoc();
        }

        Element rootElement = document.getDocumentElement();
        if (rootElement == null) {
            rootElement = document.createElement(ROOT);
            rootElement.setAttribute(VERSION, CURRENT_CMS_VERSION);
            document.appendChild(rootElement);
            Log.v(TAG, "rootElement created");
        }

        Element taskElement = document.createElement(TASK);
        taskElement.setAttribute(TITLE, task.getTitle());
        taskElement.setAttribute(TEXT, task.getText());
        taskElement.setAttribute(TIME, String.valueOf(task.getAvailable_time().getMinutes()));
        taskElement.setAttribute(PUBLIC_INPUTS, String.valueOf(task.getPublicInputs()));
        taskElement.setAttribute(MAX_STEPS, String.valueOf(task.getMaxSteps()));
        taskElement.setAttribute(TASK_RESULT_VERSION, String.valueOf(task.getResultVersion()));

        rootElement.appendChild(taskElement);
    }

    /**
     * Loads data from the DataSource into the document
     *
     * @param dataSource  the dataSource from which to read data
     * @param machineType the type of the machine
     * @throws ParserConfigurationException if the parser configuration failed
     */
    public void setData(DataSource dataSource, int machineType) throws ParserConfigurationException {
        List<Symbol> stackAlphabet = null;
        List<Symbol> inputAlphabet = dataSource.getInputAlphabetFullExtract();
        List<State> states = dataSource.getStateFullExtract();
        List<Transition> transitions = null;
        switch (machineType) {
            case MainActivity.FINITE_STATE_AUTOMATON:
                transitions = dataSource.getFsaTransitionFullExtract(inputAlphabet, states);
                break;
            case MainActivity.PUSHDOWN_AUTOMATON:
                stackAlphabet = dataSource.getStackAlphabetFullExtract();
                transitions = dataSource.getPdaTransitionFullExtract(inputAlphabet, stackAlphabet, states);
                break;
            case MainActivity.LINEAR_BOUNDED_AUTOMATON:
            case MainActivity.TURING_MACHINE:
                transitions = dataSource.getTmTransitionFullExtract(inputAlphabet, states);
                break;
        }

        setData(states, inputAlphabet, stackAlphabet, transitions, dataSource.getTapeFullExtract(inputAlphabet),
                dataSource.getTestFullExtract(false, inputAlphabet), dataSource.getTestFullExtract(true, inputAlphabet), machineType);
    }

    public void setData(List<State> states, List<Symbol> inputAlphabet, List<Symbol> stackAlphabet,
                        List<Transition> transitions, List<TapeElement> tapeElements,
                        List<TestScenario> positiveTests, List<TestScenario> negativeTests,
                        int machineType) throws ParserConfigurationException {
        if (document == null) {
            createDoc();
        }

        if (format == Format.CMS || format == Format.CMST) {
            setDataCMS(states, inputAlphabet, stackAlphabet, transitions, tapeElements,
                    positiveTests, negativeTests, machineType);
        } else {
            setDataJFF(states, inputAlphabet, stackAlphabet, transitions, machineType);
        }
    }

    public void setData(List<GrammarRule> grammarRules, List<String> tests) throws ParserConfigurationException {
        if (document == null)
            createDoc();
        if (format == Format.CMSG) {
            setDataCMSG(grammarRules, tests);
        } else {
            setDataJFF(grammarRules);
        }
    }

    public void setData(ChessGame game) throws ParserConfigurationException {
        if (document == null)
            createDoc();
        writeGame(game);
    }

    public void setData(List<GrammarRule> grammarRules) throws ParserConfigurationException {
        if (document == null) {
            createDoc();
        }
        if (format == Format.CMSG) {
            setDataCMSG(grammarRules);
        } else {
            setDataJFF(grammarRules);
        }
    }

    /**
     * Loads data from an XML String
     *
     * @param xmlDoc a string that contains an XML document to be parsed
     * @throws ParserConfigurationException if the parser configuration failed
     * @throws FileFormatException          if the file could not be parsed
     */
    public void loadFromString(String xmlDoc) throws ParserConfigurationException, FileFormatException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xmlDoc));
        try {
            document = db.parse(is);
        } catch (SAXException | IOException e) {
            throw new FileFormatException(e);
        }
    }

    /**
     * Saves the document into a String. The contents of the String are equivalent to those written into
     * a file by {@link #writeFile(String)}, but the formatting is optimized for minimum size
     *
     * @return a String representation of the generated XML document
     * @throws TransformerException if error happened while formatting the document
     */
    public String writeToString() throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(document), new StreamResult(outputStream));
        Log.i(TAG, "document transformed");

        try {
            return outputStream.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            //UTF-8 support is required by the standard, this should never happen
            Log.wtf(TAG, "The implementation does not support UTF-8", e);
            return null;
        }
    }

    /**
     * Saves the file. The data written into the file is equivalent to that returned by
     * {@link #writeToString()}, but the formatting is more friendly to human readers
     *
     * @param fileName the name of the file
     * @throws IOException          if error happened while saving the file
     * @throws TransformerException if error happened while formatting the file
     */
    public void writeFile(String fileName) throws IOException, TransformerException {
        final File directory = new File(PATH);
        directory.mkdirs();

        Log.v(TAG, PATH + "/" + fileName + format.getExtension());
        File outputFile = new File(directory, fileName + format.getExtension());
        outputFile.createNewFile();

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        transformer.transform(new DOMSource(document), new StreamResult(new FileOutputStream(outputFile)));
        Log.i(TAG, "document transformed");
    }

    public int getMachineType() throws FileFormatException {
        int machineType;

        Element rootElement = document.getDocumentElement();
        Version version = format == Format.JFF ? null : new Version(rootElement);

        String machineTypeString;

        if (version == null || version.major < 2) {
            //get typeNode and his value
            Node type = rootElement.getElementsByTagName(TYPE).item(0);
            if (type == null) {
                throw new FileFormatException(TYPE + " element not found");
            }
            machineTypeString = type.getFirstChild().getNodeValue();
        } else {
            Element automatonElement = (Element) rootElement.getElementsByTagName(AUTOMATON).item(0);
            machineTypeString = automatonElement.getAttribute(TYPE);
        }

        if (format == Format.CMS || format == Format.CMST) {
            switch (machineTypeString) {
                case "fsa":
                    machineType = MainActivity.FINITE_STATE_AUTOMATON;
                    Log.v(TAG, "fsa selected");
                    break;
                case "pda":
                    machineType = MainActivity.PUSHDOWN_AUTOMATON;
                    Log.v(TAG, "pda selected");
                    break;
                case "lba":
                    machineType = MainActivity.LINEAR_BOUNDED_AUTOMATON;
                    Log.v(TAG, "lba selected");
                    break;
                case "tm":
                    machineType = MainActivity.TURING_MACHINE;
                    Log.v(TAG, "tm selected");
                    break;
                default:
                    throw new FileFormatException(format.toString() + ": unknown machine type: " + machineTypeString);
            }
        } else {
            switch (machineTypeString) {
                case "fa":
                    machineType = MainActivity.FINITE_STATE_AUTOMATON;
                    Log.v(TAG, "fsa selected");
                    break;
                case "pda":
                    machineType = MainActivity.PUSHDOWN_AUTOMATON;
                    Log.v(TAG, "pda selected");
                    break;
                case "turing":
                    machineType = MainActivity.TURING_MACHINE;
                    Log.v(TAG, "tm selected");
                    break;
                default:
                    throw new FileFormatException(format.toString() + ": unknown machine type: " + machineTypeString);
            }
        }

        return machineType;
    }

    private void getDataCMS(DataSource dataSource) throws FileFormatException {
        Log.v(TAG, "getDataCMS method started");

        Element rootElement = document.getDocumentElement();

        Version version = new Version(rootElement);
        switch (version.major) {
            case 1:
                getDataCMSHelper(dataSource, rootElement, version);
                break;
            case 2:
                Element automatonElement = (Element) rootElement.getElementsByTagName(AUTOMATON).item(0);
                if (automatonElement == null) {
                    throw new FileFormatException("The file does not contain an automaton");
                }
                getDataCMSHelper(dataSource, automatonElement, version);
                break;
            default:
                throw new FileFormatException("Unknown CMS file format version");
        }
    }

    private void getDataCMSHelper(DataSource dataSource, Element automatonElement, Version version)
            throws FileFormatException {
        Log.v(TAG, "getDataCMSHelper method started");

        ////initialize inputAlphabetElements
        //map id->inputSymbol (id is from the file, may not be the same as the one generated by database)
        LongSparseArray<Symbol> inputAlphabetMap = new LongSparseArray<>();
        Element inputAlphabetElement = (Element) automatonElement.getElementsByTagName(INPUT_ALPHABET).item(0);
        if (inputAlphabetElement == null) {
            throw new FileFormatException(INPUT_ALPHABET + " element not found");
        }
        NodeList inputAlphabetNodeList = inputAlphabetElement.getElementsByTagName(SYMBOL);
        for (int i = 0; i < inputAlphabetNodeList.getLength(); i++) {
            Element symbolNode = (Element) inputAlphabetNodeList.item(i);
            long inputSymbolId = Long.parseLong(symbolNode.getAttribute(ID));
            String inputSymbolValue = symbolNode.getAttribute(NAME);

            //older versions did not store symbol properties (because empty symbol could never be changed)
            int inputSymbolProperties;
            if (version.major >= 2) {
                inputSymbolProperties = Integer.parseInt(symbolNode.getAttribute(PROPERTIES));
            } else {
                if (inputSymbolValue.equals("ε") || inputSymbolValue.equals("#")) {
                    inputSymbolProperties = Symbol.EMPTY;
                } else {
                    inputSymbolProperties = 0;
                }
            }
            Symbol symbol = dataSource.addInputSymbol(inputSymbolValue, inputSymbolProperties);
            inputAlphabetMap.put(inputSymbolId, symbol);
            if (i == 0) {
                emptyInputSymbolId = symbol.getId();
            }
            Log.v(TAG, "inputSymbol created");
        }

        int machineType = getMachineType();

        if (machineType == MainActivity.LINEAR_BOUNDED_AUTOMATON && version.major < 2) {
            dataSource.addInputSymbol("|<", Symbol.LEFT_BOUND);
            dataSource.addInputSymbol(">|", Symbol.RIGHT_BOUND);
        }

        ////initialize stackAlphabetElements
        //map id->stackSymbol (id is from the file, may not be the same as the one generated by database)
        LongSparseArray<Symbol> stackAlphabetMap = new LongSparseArray<>();
        if (machineType == MainActivity.PUSHDOWN_AUTOMATON) {
            Element stackAlphabetElement = (Element) automatonElement.getElementsByTagName(STACK_ALPHABET).item(0);
            if (stackAlphabetElement == null) {
                throw new FileFormatException(STACK_ALPHABET + " element not found");
            }
            NodeList stackAlphabetNodeList = stackAlphabetElement.getElementsByTagName(SYMBOL);
            for (int i = 0; i < stackAlphabetNodeList.getLength(); i++) {
                Element symbolNode = (Element) stackAlphabetNodeList.item(i);
                long stackSymbolId = Long.parseLong(symbolNode.getAttribute(ID));
                String stackSymbolValue = symbolNode.getAttribute(NAME);
                int stackSymbolProperties;
                if (version.major >= 2) {
                    stackSymbolProperties = Integer.parseInt(symbolNode.getAttribute(PROPERTIES));
                } else {
                    if (i == 0) {
                        stackSymbolProperties = Symbol.STACK_BOTTOM;
                    } else {
                        stackSymbolProperties = 0;
                    }
                }
                Symbol symbol = dataSource.addStackSymbol(stackSymbolValue, stackSymbolProperties);
                stackAlphabetMap.put(stackSymbolId, symbol);
                if (i == 0) {
                    startStackSymbolId = symbol.getId();
                }
            }
        }

        ////initialize stateElements
        //map id->state (id is from the file, may not be the same as the one generated by database)
        LongSparseArray<State> stateMap = new LongSparseArray<>();
        Element statesElement = (Element) automatonElement.getElementsByTagName(STATES).item(0);
        if (statesElement == null) {
            throw new FileFormatException(STATES + " element not found");
        }
        NodeList stateNodeList = statesElement.getElementsByTagName(STATE);
        for (int i = 0; i < stateNodeList.getLength(); i++) {
            Element stateNode = (Element) stateNodeList.item(i);
            long stateId = Long.parseLong(stateNode.getAttribute(ID));
            String stateValue = stateNode.getAttribute(NAME);

            Node xNode = stateNode.getElementsByTagName(X).item(0);
            int x = Integer.parseInt(xNode.getFirstChild().getNodeValue());
            Node yNode = stateNode.getElementsByTagName(Y).item(0);
            int y = Integer.parseInt(yNode.getFirstChild().getNodeValue());
            boolean initialS = stateNode.getElementsByTagName(INITIAL).getLength() > 0;
            boolean finalS = stateNode.getElementsByTagName(FINAL).getLength() > 0;

            State state = dataSource.addState(stateValue, x, y, initialS, finalS);
            stateMap.put(stateId, state);
            Log.v(TAG, "state created");
        }

        ////initialize transitionElements
        Element transitionsElement = (Element) automatonElement.getElementsByTagName(TRANSITIONS).item(0);
        if (transitionsElement == null) {
            throw new FileFormatException(TRANSITIONS + " element not found");
        }
        NodeList transitionNodeList = transitionsElement.getElementsByTagName(TRANSITION);
        for (int i = 0; i < transitionNodeList.getLength(); i++) {
            Element transitionNode = (Element) transitionNodeList.item(i);

            Node fromStateNode = transitionNode.getElementsByTagName(FROM).item(0);
            long fromStateId = Long.parseLong(fromStateNode.getFirstChild().getNodeValue());
            State fromState = stateMap.get(fromStateId);
            Log.v(TAG, "fromStateNode initialized");

            Node toStateNode = transitionNode.getElementsByTagName(TO).item(0);
            long toStateId = Long.parseLong(toStateNode.getFirstChild().getNodeValue());
            State toState = stateMap.get(toStateId);
            Log.v(TAG, "toStateNode initialized");

            Node readSymbolNode = transitionNode.getElementsByTagName(READ).item(0);
            long readSymbolId = Long.parseLong(readSymbolNode.getFirstChild().getNodeValue());
            Symbol readSymbol = inputAlphabetMap.get(readSymbolId);
            Log.v(TAG, "readSymbolNode initialized");

            switch (machineType) {
                case MainActivity.FINITE_STATE_AUTOMATON:
                    dataSource.addFsaTransition(fromState, readSymbol, toState, emptyInputSymbolId);
                    break;
                case MainActivity.PUSHDOWN_AUTOMATON:
                    NodeList popSymbolNodeList = transitionNode.getElementsByTagName(POP);
                    List<Symbol> popSymbolList = new ArrayList<>();
                    for (int j = 0; j < popSymbolNodeList.getLength(); j++) {
                        Node popSymbolNode = popSymbolNodeList.item(j);
                        long popSymbolId = Long.parseLong(popSymbolNode.getFirstChild().getNodeValue());
                        Symbol popSymbol = stackAlphabetMap.get(popSymbolId);
                        popSymbolList.add(popSymbol);
                        Log.v(TAG, "popSymbolNode initialized");
                    }
                    Log.v(TAG, "popSymbolNodeList initialized");

                    NodeList pushSymbolNodeList = transitionNode.getElementsByTagName(PUSH);
                    List<Symbol> pushSymbolList = new ArrayList<>();
                    for (int j = 0; j < pushSymbolNodeList.getLength(); j++) {
                        Node pushSymbolNode = pushSymbolNodeList.item(j);
                        long pushSymbolId = Long.parseLong(pushSymbolNode.getFirstChild().getNodeValue());
                        Symbol pushSymbol = stackAlphabetMap.get(pushSymbolId);
                        pushSymbolList.add(pushSymbol);
                        Log.v(TAG, "pushSymbolNode initialized");
                    }
                    Log.v(TAG, "pushSymbolNodeList initialized");

                    dataSource.addPdaTransition(fromState, readSymbol, toState, emptyInputSymbolId, popSymbolList, pushSymbolList);
                    break;
                case MainActivity.LINEAR_BOUNDED_AUTOMATON:
                case MainActivity.TURING_MACHINE:
                    Node writeSymbolNode = transitionNode.getElementsByTagName(WRITE).item(0);
                    long writeSymbolId = Long.parseLong(writeSymbolNode.getFirstChild().getNodeValue());
                    Symbol writeSymbol = inputAlphabetMap.get(writeSymbolId);
                    Log.v(TAG, "writeSymbolNode initialized");

                    Node directionNode = transitionNode.getElementsByTagName(MOVE).item(0);
                    TmTransition.Direction direction = TmTransition.Direction.fromString(directionNode.getFirstChild().getNodeValue());
                    Log.v(TAG, "directionNode initialized");

                    dataSource.addTmTransition(fromState, readSymbol, toState, writeSymbol, direction);
                    break;
            }
        }

        ////initialize tapeElements
        Element tapeElement = (Element) automatonElement.getElementsByTagName(TAPE).item(0);
        if (tapeElement == null) {
            throw new FileFormatException(TAPE + " element not found");
        }
        NodeList tapeNodeList = tapeElement.getElementsByTagName(SYMBOL);
        for (int i = 0; i < tapeNodeList.getLength(); i++) {
            Node tapeNode = tapeNodeList.item(i);
            long symbolId = Long.parseLong(tapeNode.getFirstChild().getNodeValue());
            Symbol symbol = inputAlphabetMap.get(symbolId);
            dataSource.addTapeElement(symbol, i);
        }
        Log.v(TAG, "tapeNodeList initialized");

        //initialize test scenarios
        if (version.major >= 2) {
            Element testScenariosElement = (Element) automatonElement.getElementsByTagName(TEST_SCENARIOS).item(0);
            if (testScenariosElement == null) {
                throw new FileFormatException(TEST_SCENARIOS + " element not found");
            }
            setTestScenarios(testScenariosElement, dataSource, false, inputAlphabetMap);

            Element negativeScenariosElement = (Element) automatonElement.getElementsByTagName(NEGATIVE_SCENARIOS).item(0);
            if (negativeScenariosElement == null) {
                throw new FileFormatException(TEST_SCENARIOS + " element not found");
            }
            setTestScenarios(negativeScenariosElement, dataSource, true, inputAlphabetMap);
        }
    }

    private void setTestScenarios(Element testScenariosElement, DataSource dataSource,
                                  boolean negative, LongSparseArray<Symbol> inputAlphabetMap) {
        NodeList testNodeList = testScenariosElement.getElementsByTagName(TEST_SCENARIO);
        for (int i = 0; i < testNodeList.getLength(); i++) {
            List<Symbol> inputWord = new ArrayList<>();
            List<Symbol> outputWord = null;

            Element testElement = (Element) testNodeList.item(i);
            Element inputWordElement = (Element) testElement.getElementsByTagName(INPUT_WORD).item(0);
            if (inputWordElement != null) {
                NodeList symbolNodeList = inputWordElement.getElementsByTagName(SYMBOL);
                for (int j = 0; j < symbolNodeList.getLength(); j++) {
                    Element symbolElement = (Element) symbolNodeList.item(j);
                    long symbolId = Long.parseLong(symbolElement.getAttribute(ID));
                    inputWord.add(inputAlphabetMap.get(symbolId));
                }
            }
            Element outputWordElement = (Element) testElement.getElementsByTagName(OUTPUT_WORD).item(0);
            if (outputWordElement != null) {
                outputWord = new ArrayList<>();

                NodeList symbolNodeList = outputWordElement.getElementsByTagName(SYMBOL);
                for (int j = 0; j < symbolNodeList.getLength(); j++) {
                    Element symbolElement = (Element) symbolNodeList.item(j);
                    long symbolId = Long.parseLong(symbolElement.getAttribute(ID));
                    outputWord.add(inputAlphabetMap.get(symbolId));
                }
            }

            dataSource.addOrUpdateTest(new TestScenario(inputWord, outputWord), negative);
        }
    }

    private void getDataJFF(DataSource dataSource) throws FileFormatException {
        Log.v(TAG, "getDataJFF method started");

        Element documentType = (Element) document.getElementsByTagName(TYPE).item(0);
        if (documentType.getTextContent().equals(GRAMMAR)) {
            NodeList nodeList = document.getElementsByTagName(PRODUCTION);
            dataSource.clearGrammarRuleTable();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                NodeList rules = node.getChildNodes();

                for (int j = 1; j < rules.getLength(); j += 4) {
                    if (rules.item(j) != null && rules.item(j).getNodeType() == Node.ELEMENT_NODE) {
                        String leftRule = rules.item(j).getTextContent();
                        String rightRule = rules.item(j + 2).getTextContent();
                        if (rightRule.equals("")) {
                            rightRule = "ε";
                        }
                        dataSource.addGrammarRule(leftRule, rightRule);
                        Log.v(TAG, "grammar rule created");
                    }
                }
            }
        } else {
            int machineType = getMachineType();

            //insert empty symbol
            Symbol emptySymbol = null;
            switch (machineType) {
                case MainActivity.FINITE_STATE_AUTOMATON:
                    emptySymbol = dataSource.addInputSymbol("ε", Symbol.EMPTY);
                    emptyInputSymbolId = emptySymbol.getId();
                    break;
                case MainActivity.PUSHDOWN_AUTOMATON:
                    emptySymbol = dataSource.addInputSymbol("ε", Symbol.EMPTY);
                    emptyInputSymbolId = emptySymbol.getId();
                    break;
                case MainActivity.LINEAR_BOUNDED_AUTOMATON:
                    emptySymbol = dataSource.addInputSymbol("#", Symbol.EMPTY);
                    emptyInputSymbolId = emptySymbol.getId();
                    break;
                case MainActivity.TURING_MACHINE:
                    emptySymbol = dataSource.addInputSymbol("#", Symbol.EMPTY);
                    emptyInputSymbolId = emptySymbol.getId();
                    break;
            }
            //map value->inputSymbol
            Map<String, Symbol> inputAlphabetMap = new HashMap<>();

            //map value->stackSymbol
            Map<String, Symbol> stackAlphabetMap = new HashMap<>();
            if (machineType == MainActivity.PUSHDOWN_AUTOMATON) {
                //insert start stack
                Symbol startStack = dataSource.addStackSymbol("Z", Symbol.STACK_BOTTOM);
                startStackSymbolId = startStack.getId();
                stackAlphabetMap.put(startStack.getValue(), startStack);
            }

            //initialize rootElement
            Element rootElement = document.getDocumentElement();

            //initialize automatonElement
            Element automatonElement = (Element) rootElement.getElementsByTagName(AUTOMATON).item(0);
            if (automatonElement == null) {
                throw new FileFormatException(AUTOMATON + " element not found");
            }
            Log.v(TAG, "automatonElement initialized");

            ////prepare states
            //map id->state (id is from the file, may not be the same as the one generated by database)
            LongSparseArray<State> stateMap = new LongSparseArray<>();
            NodeList stateNodeList = automatonElement.getElementsByTagName(STATE);
            int qMax = 0; //highest number from all uncreatively named states (needed to avoid name collisions when we will generate new states)
            for (int i = 0; i < stateNodeList.getLength(); i++) {
                Element stateNode = (Element) stateNodeList.item(i);
                long stateId = Long.parseLong(stateNode.getAttribute(ID));
                String stateValue = stateNode.getAttribute(NAME);
                String[] stateSubstrings = stateValue.split("q");
                if (stateSubstrings.length == 2) { //if the string is "q<something>", split returns ["", "<something>"]
                    try {
                        int number = Integer.parseInt(stateSubstrings[1]);
                        qMax = Math.max(qMax, number);
                    } catch (NumberFormatException e) {
                    }
                }

                Node xNode = stateNode.getElementsByTagName(X).item(0);
                int x = (int) Float.parseFloat(xNode.getFirstChild().getNodeValue());
                Node yNode = stateNode.getElementsByTagName(Y).item(0);
                int y = (int) Float.parseFloat(yNode.getFirstChild().getNodeValue());
                boolean initialS = stateNode.getElementsByTagName(INITIAL).getLength() > 0;
                boolean finalS = stateNode.getElementsByTagName(FINAL).getLength() > 0;

                State state = dataSource.addState(stateValue, x, y, initialS, finalS);
                stateMap.put(stateId, state);
                Log.v(TAG, "state created");
            }
            Log.v(TAG, "states initialized");

            ////prepare transitions and alphabets
            NodeList transitionNodeList = automatonElement.getElementsByTagName(TRANSITION);
            for (int i = 0; i < transitionNodeList.getLength(); i++) {
                Element transitionNode = (Element) transitionNodeList.item(i);

                Node fromStateNode = transitionNode.getElementsByTagName(FROM).item(0);
                long fromStateId = Long.parseLong(fromStateNode.getFirstChild().getNodeValue());
                State fromState = stateMap.get(fromStateId);
                Log.v(TAG, "fromStateNode initialized");

                Node toStateNode = transitionNode.getElementsByTagName(TO).item(0);
                long toStateId = Long.parseLong(toStateNode.getFirstChild().getNodeValue());
                State toState = stateMap.get(toStateId);
                Log.v(TAG, "toStateNode initialized");

                Node readSymbolNode = transitionNode.getElementsByTagName(READ).item(0);
                Symbol readSymbol = null;
                String readSymbolString = "";
                if (readSymbolNode.getFirstChild() != null) {
                    readSymbolString = readSymbolNode.getFirstChild().getNodeValue();
                    Log.v(TAG, "readSymbolNode initialized");
                    for (int j = 0; j < readSymbolString.length(); j++) {
                        try {
                            readSymbol = dataSource.addInputSymbol(readSymbolString.substring(j, j + 1), 0);
                            inputAlphabetMap.put(readSymbolString.substring(j, j + 1), readSymbol);
                            Log.v(TAG, "newSymbol added into database");
                        } catch (SQLiteConstraintException e) {
                            readSymbol = inputAlphabetMap.get(readSymbolString.substring(j, j + 1));
                            Log.e(TAG, "duplicity while adding symbol into database", e);
                        }
                    }
                } else {
                    readSymbol = emptySymbol;
                    Log.v(TAG, "empty symbol recognized");
                }

                switch (machineType) {
                    case MainActivity.FINITE_STATE_AUTOMATON:
                        //if multiple symbols are to be read, create auxiliary states
                        State fromStateTemp = fromState;
                        for (int j = 1; j < readSymbolString.length(); j++) {
                            State newState = dataSource.addState("q" + ++qMax,
                                    fromState.getPositionX() + j * (toState.getPositionX() - fromState.getPositionX()) / readSymbolString.length(),
                                    fromState.getPositionY() + j * (toState.getPositionY() - fromState.getPositionY()) / readSymbolString.length(),
                                    false, false);
                            dataSource.addFsaTransition(fromStateTemp, inputAlphabetMap.get(readSymbolString.substring(j - 1, j)), newState, emptyInputSymbolId);
                            fromStateTemp = newState;
                            Log.v(TAG, "fsaTransition created");
                        }
                        dataSource.addFsaTransition(fromStateTemp, readSymbol, toState, emptyInputSymbolId);
                        Log.v(TAG, "fsaTransition created");
                        break;
                    case MainActivity.PUSHDOWN_AUTOMATON:
                        Node popNode = transitionNode.getElementsByTagName(POP).item(0);
                        List<Symbol> popSymbolList = new ArrayList<>();
                        if (popNode.getFirstChild() != null) {
                            String popString = popNode.getFirstChild().getNodeValue();
                            Log.v(TAG, "popNode initialized");
                            for (int j = popString.length() - 1; j >= 0; j--) {
                                String popValue = String.valueOf(popString.charAt(j));
                                Symbol popSymbol;
                                try {
                                    popSymbol = dataSource.addStackSymbol(popValue, 0);
                                    stackAlphabetMap.put(popValue, popSymbol);
                                    popSymbolList.add(popSymbol);
                                    Log.v(TAG, "newSymbol added into database");
                                } catch (SQLiteConstraintException e) {
                                    popSymbol = stackAlphabetMap.get(popValue);
                                    popSymbolList.add(popSymbol);
                                    Log.e(TAG, "duplicity while adding symbol into database", e);
                                }
                            }
                        }

                        Node pushNode = transitionNode.getElementsByTagName(PUSH).item(0);
                        List<Symbol> pushSymbolList = new ArrayList<>();
                        if (pushNode.getFirstChild() != null) {
                            String pushString = pushNode.getFirstChild().getNodeValue();
                            Log.v(TAG, "pushNode initialized");
                            for (int j = pushString.length() - 1; j >= 0; j--) {
                                String pushValue = String.valueOf(pushString.charAt(j));
                                Symbol pushSymbol;
                                try {
                                    pushSymbol = dataSource.addStackSymbol(pushValue, 0);
                                    stackAlphabetMap.put(pushValue, pushSymbol);
                                    pushSymbolList.add(pushSymbol);
                                    Log.v(TAG, "newSymbol added into database");
                                } catch (SQLiteConstraintException e) {
                                    pushSymbol = stackAlphabetMap.get(pushValue);
                                    pushSymbolList.add(pushSymbol);
                                    Log.e(TAG, "duplicity while adding symbol into database", e);
                                }
                            }
                        }

                        //if multiple symbols are to be read, create auxiliary states;
                        //all stack manipulation happens during the last transition
                        fromStateTemp = fromState;
                        for (int j = 1; j < readSymbolString.length(); j++) {
                            State newState = dataSource.addState("q" + ++qMax,
                                    fromState.getPositionX() + j * (toState.getPositionX() - fromState.getPositionX()) / readSymbolString.length(),
                                    fromState.getPositionY() + j * (toState.getPositionY() - fromState.getPositionY()) / readSymbolString.length(),
                                    false, false);
                            dataSource.addPdaTransition(fromStateTemp, inputAlphabetMap.get(readSymbolString.substring(j - 1, j)), newState, emptyInputSymbolId, new ArrayList<Symbol>(), new ArrayList<Symbol>());
                            fromStateTemp = newState;
                            Log.v(TAG, "fsaTransition created");
                        }
                        dataSource.addPdaTransition(fromStateTemp, readSymbol, toState, emptyInputSymbolId, popSymbolList, pushSymbolList);
                        Log.v(TAG, "pdaTransition created");
                        break;
                    case MainActivity.LINEAR_BOUNDED_AUTOMATON:
                    case MainActivity.TURING_MACHINE:
                        Node writeSymbolNode = transitionNode.getElementsByTagName(WRITE).item(0);
                        Symbol writeSymbol = null;
                        if (writeSymbolNode.getFirstChild() != null) {
                            String writeSymbolString = writeSymbolNode.getFirstChild().getNodeValue();
                            Log.v(TAG, "writeNode initialized");
                            try {
                                writeSymbol = dataSource.addInputSymbol(writeSymbolString, 0);
                                inputAlphabetMap.put(writeSymbolString, writeSymbol);
                                Log.v(TAG, "newSymbol added into database");
                            } catch (SQLiteConstraintException e) {
                                writeSymbol = inputAlphabetMap.get(writeSymbolString);
                                Log.e(TAG, "duplicity while adding symbol into database", e);
                            }
                        } else {
                            writeSymbol = emptySymbol;
                            Log.v(TAG, "empty symbol recognized");
                        }

                        Node directionNode = transitionNode.getElementsByTagName(MOVE).item(0);
                        TmTransition.Direction direction = TmTransition.Direction.fromString(directionNode.getFirstChild().getNodeValue());
                        Log.v(TAG, "directionNode initialized");

                        dataSource.addTmTransition(fromState, readSymbol, toState, writeSymbol, direction);
                        Log.v(TAG, "tmTransition created");
                        break;
                }
            }

            ////prepare tapeElements
            switch (machineType) {
                case MainActivity.FINITE_STATE_AUTOMATON:
                case MainActivity.PUSHDOWN_AUTOMATON:
                case MainActivity.LINEAR_BOUNDED_AUTOMATON:
                    dataSource.addTapeElement(emptySymbol, 0);
                    break;
                case MainActivity.TURING_MACHINE:
                    //tape initialization
                    for (int i = 0; i < 6; i++) {
                        dataSource.addTapeElement(emptySymbol, i);
                    }
                    break;
            }
        }
    }

    private void getDataCMSG(DataSource dataSource) throws FileFormatException {
        Log.v(TAG, "getDataCMSG method started");

        NodeList nodeList = document.getElementsByTagName("rule");
        dataSource.clearGrammarRuleTable();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            NodeList rules = node.getChildNodes();

            for (int j = 1; j < rules.getLength(); j += 4) {
                if (rules.item(j) != null && rules.item(j).getNodeType() == Node.ELEMENT_NODE) {
                    String leftRule = rules.item(j).getTextContent();
                    String rightRule = rules.item(j + 2).getTextContent();
                    dataSource.addGrammarRule(leftRule, rightRule);
                    Log.v(TAG, "grammar rule created");
                }
            }
        }

        NodeList testsList = document.getElementsByTagName(TEST_SCENARIO);
        for (int i = 0; i < testsList.getLength(); i++) {
            Node node = testsList.item(i);
            NamedNodeMap attr = node.getAttributes();
            String input_word = attr.getNamedItem(INPUT_WORD).getNodeValue();

            dataSource.addGrammarTest(input_word);
        }
    }

    private void setDataCMS(List<State> stateList, List<Symbol> inputAlphabetList, List<Symbol> stackAlphabetList,
                            List<Transition> transitionList, List<TapeElement> tapeElementList,
                            List<TestScenario> testScenarios, List<TestScenario> negativeTestScenarios,
                            int machineType) {
        Log.v(TAG, "setDataCMS method started");

        String machineTypeString = null;
        switch (machineType) {
            case MainActivity.FINITE_STATE_AUTOMATON:
                machineTypeString = "fsa";
                break;
            case MainActivity.PUSHDOWN_AUTOMATON:
                machineTypeString = "pda";
                break;
            case MainActivity.LINEAR_BOUNDED_AUTOMATON:
                machineTypeString = "lba";
                break;
            case MainActivity.TURING_MACHINE:
                machineTypeString = "tm";
                break;
        }

        Element rootElement = document.getDocumentElement();
        boolean createRoot = rootElement == null;
        //create rootElement
        if (createRoot) {
            rootElement = document.createElement(ROOT);
            rootElement.setAttribute(VERSION, CURRENT_CMS_VERSION);

            Log.v(TAG, "rootElement created");
        }

        //create automatonElement
        Element automatonElement = document.createElement(AUTOMATON);
        automatonElement.setAttribute(TYPE, machineTypeString);
        rootElement.appendChild(automatonElement);
        Log.v(TAG, "automatonElement Created");

        ////create inputAlphabetElements
        Element inputAlphabetElement = document.createElement(INPUT_ALPHABET);
        for (Symbol symbol : inputAlphabetList) {
            Element SymbolElement = document.createElement(SYMBOL);
            SymbolElement.setAttribute(ID, String.valueOf(symbol.getId()));
            SymbolElement.setAttribute(NAME, symbol.getValue());
            SymbolElement.setAttribute(PROPERTIES, String.valueOf(symbol.getProperties()));
            inputAlphabetElement.appendChild(SymbolElement);
            Log.v(TAG, "inputAlphabetElement created");
        }
        automatonElement.appendChild(inputAlphabetElement);
        Log.v(TAG, "inputAlphabetElements created");

        ////create stackAlphabetElements
        if (machineType == MainActivity.PUSHDOWN_AUTOMATON) {
            Element stackAlphabetElement = document.createElement(STACK_ALPHABET);
            for (Symbol symbol : stackAlphabetList) {
                Element SymbolElement = document.createElement(SYMBOL);
                SymbolElement.setAttribute(ID, String.valueOf(symbol.getId()));
                SymbolElement.setAttribute(NAME, symbol.getValue());
                SymbolElement.setAttribute(PROPERTIES, String.valueOf(symbol.getProperties()));
                stackAlphabetElement.appendChild(SymbolElement);
                Log.v(TAG, "stackAlphabetElement created");
            }
            automatonElement.appendChild(stackAlphabetElement);
            Log.v(TAG, "stackAlphabetElements created");
        }

        ////create stateElements
        Element statesElement = document.createElement(STATES);
        for (State state : stateList) {
            Element stateElement = document.createElement(STATE);
            stateElement.setAttribute(ID, String.valueOf(state.getId()));
            stateElement.setAttribute(NAME, state.getValue());

            Element xElement = document.createElement(X);
            xElement.appendChild(document.createTextNode(String.valueOf(state.getPositionX())));
            stateElement.appendChild(xElement);

            Element yElement = document.createElement(Y);
            yElement.appendChild(document.createTextNode(String.valueOf(state.getPositionY())));
            stateElement.appendChild(yElement);

            if (state.isInitialState()) {
                Element initialElement = document.createElement(INITIAL);
                stateElement.appendChild(initialElement);
            }

            if (state.isFinalState()) {
                Element finalElement = document.createElement(FINAL);
                stateElement.appendChild(finalElement);
            }

            statesElement.appendChild(stateElement);
            Log.v(TAG, "stateElement created");
        }
        automatonElement.appendChild(statesElement);
        Log.v(TAG, "stateElements created");

        ////create transitionsElement
        Element transitionsElement = document.createElement(TRANSITIONS);
        switch (machineType) {
            case MainActivity.FINITE_STATE_AUTOMATON:
                for (Transition transition : transitionList) {
                    Element transitionElement = document.createElement(TRANSITION);

                    Element fromElement = document.createElement(FROM);
                    fromElement.appendChild(document.createTextNode(String.valueOf(transition.getFromState().getId())));
                    transitionElement.appendChild(fromElement);

                    Element toElement = document.createElement(TO);
                    toElement.appendChild(document.createTextNode(String.valueOf(transition.getToState().getId())));
                    transitionElement.appendChild(toElement);

                    Element readElement = document.createElement(READ);
                    readElement.appendChild(document.createTextNode(String.valueOf(transition.getReadSymbol().getId())));
                    transitionElement.appendChild(readElement);

                    transitionsElement.appendChild(transitionElement);
                    Log.v(TAG, "transitionElement created");
                }
                break;
            case MainActivity.PUSHDOWN_AUTOMATON:
                for (Transition transition : transitionList) {
                    Element transitionElement = document.createElement(TRANSITION);

                    Element fromElement = document.createElement(FROM);
                    fromElement.appendChild(document.createTextNode(String.valueOf(transition.getFromState().getId())));
                    transitionElement.appendChild(fromElement);

                    Element toElement = document.createElement(TO);
                    toElement.appendChild(document.createTextNode(String.valueOf(transition.getToState().getId())));
                    transitionElement.appendChild(toElement);

                    Element readElement = document.createElement(READ);
                    readElement.appendChild(document.createTextNode(String.valueOf(transition.getReadSymbol().getId())));
                    transitionElement.appendChild(readElement);

                    for (Symbol symbol : ((PdaTransition) transition).getPopSymbolList()) {
                        Element popElement = document.createElement(POP);
                        popElement.appendChild(document.createTextNode(String.valueOf(symbol.getId())));
                        transitionElement.appendChild(popElement);
                    }

                    for (Symbol symbol : ((PdaTransition) transition).getPushSymbolList()) {
                        Element pushElement = document.createElement(PUSH);
                        pushElement.appendChild(document.createTextNode(String.valueOf(symbol.getId())));
                        transitionElement.appendChild(pushElement);
                    }

                    transitionsElement.appendChild(transitionElement);
                    Log.v(TAG, "transitionElement created");
                }
                break;
            case MainActivity.LINEAR_BOUNDED_AUTOMATON:
            case MainActivity.TURING_MACHINE:
                for (Transition transition : transitionList) {
                    Element transitionElement = document.createElement(TRANSITION);

                    Element fromElement = document.createElement(FROM);
                    fromElement.appendChild(document.createTextNode(String.valueOf(transition.getFromState().getId())));
                    transitionElement.appendChild(fromElement);

                    Element toElement = document.createElement(TO);
                    toElement.appendChild(document.createTextNode(String.valueOf(transition.getToState().getId())));
                    transitionElement.appendChild(toElement);

                    Element readElement = document.createElement(READ);
                    readElement.appendChild(document.createTextNode(String.valueOf(transition.getReadSymbol().getId())));
                    transitionElement.appendChild(readElement);

                    Element writeElement = document.createElement(WRITE);
                    writeElement.appendChild(document.createTextNode(String.valueOf(((TmTransition) transition).getWriteSymbol().getId())));
                    transitionElement.appendChild(writeElement);

                    Element moveElement = document.createElement(MOVE);
                    moveElement.appendChild(document.createTextNode(((TmTransition) transition).getDirection().toString()));
                    transitionElement.appendChild(moveElement);

                    transitionsElement.appendChild(transitionElement);
                    Log.v(TAG, "transitionElement created");
                }
                break;
        }
        automatonElement.appendChild(transitionsElement);
        Log.v(TAG, "transitionElements created");

        ////create tapeElements
        Element tapeElement = document.createElement(TAPE);
        if (tapeElementList != null) {
            for (TapeElement tapeEle : tapeElementList) {
                Element symbolElement = document.createElement(SYMBOL);
                symbolElement.appendChild(document.createTextNode(String.valueOf(tapeEle.getSymbol().getId())));
                tapeElement.appendChild(symbolElement);
                Log.v(TAG, "tapeElement created");
            }
        }
        automatonElement.appendChild(tapeElement);
        Log.v(TAG, "tapeElements created");

        //create test elements
        Element tests = document.createElement(TEST_SCENARIOS);
        for (TestScenario testScenario : testScenarios) {
            Element testElement = document.createElement(TEST_SCENARIO);
            //store input word
            if (testScenario.getInputWord() != null) {
                Element inputWordElement = document.createElement(INPUT_WORD);
                for (Symbol symbol : testScenario.getInputWord()) {
                    Element symbolElement = document.createElement(SYMBOL);
                    symbolElement.setAttribute(ID, String.valueOf(symbol.getId()));
                    inputWordElement.appendChild(symbolElement);
                }

                testElement.appendChild(inputWordElement);
            }

            //store output word
            if (testScenario.getOutputWord() != null) {
                Element inputWordElement = document.createElement(OUTPUT_WORD);
                for (Symbol symbol : testScenario.getOutputWord()) {
                    Element symbolElement = document.createElement(SYMBOL);
                    symbolElement.setAttribute(ID, String.valueOf(symbol.getId()));
                    inputWordElement.appendChild(symbolElement);
                }

                testElement.appendChild(inputWordElement);
            }

            tests.appendChild(testElement);
        }
        automatonElement.appendChild(tests);

        //create negative test elements
        tests = document.createElement(NEGATIVE_SCENARIOS);
        for (TestScenario testScenario : negativeTestScenarios) {
            Element testElement = document.createElement(TEST_SCENARIO);
            //store input word
            if (testScenario.getInputWord() != null) {
                Element inputWordElement = document.createElement(INPUT_WORD);
                for (Symbol symbol : testScenario.getInputWord()) {
                    Element symbolElement = document.createElement(SYMBOL);
                    symbolElement.setAttribute(ID, String.valueOf(symbol.getId()));
                    inputWordElement.appendChild(symbolElement);
                }

                testElement.appendChild(inputWordElement);
            }

            //store output word
            if (testScenario.getOutputWord() != null) {
                Element inputWordElement = document.createElement(OUTPUT_WORD);
                for (Symbol symbol : testScenario.getOutputWord()) {
                    Element symbolElement = document.createElement(SYMBOL);
                    symbolElement.setAttribute(ID, String.valueOf(symbol.getId()));
                    symbolElement.setAttribute(NAME, symbol.getValue());

                    inputWordElement.appendChild(symbolElement);
                }

                testElement.appendChild(inputWordElement);
            }

            tests.appendChild(testElement);
        }
        automatonElement.appendChild(tests);

        if (createRoot) {
            document.appendChild(rootElement);
        }
        Log.i(TAG, "rootElement attached to document");
    }

    private void setDataJFF(List<State> stateList, List<Symbol> inputAlphabetList, List<Symbol> stackAlphabetList,
                            List<Transition> transitionList, int machineType) {
        Log.v(TAG, "setDataJFF method started");

        String machineTypeString = null;
        switch (machineType) {
            case MainActivity.FINITE_STATE_AUTOMATON:
                machineTypeString = "fa";
                break;
            case MainActivity.PUSHDOWN_AUTOMATON:
                machineTypeString = "pda";
                break;
            case MainActivity.LINEAR_BOUNDED_AUTOMATON:
            case MainActivity.TURING_MACHINE:
                machineTypeString = "turing";
                break;
        }

        //create rootElement
        Element rootElement = document.createElement(ROOT);
        Log.v(TAG, "rootElement created");

        //create typeElement
        Element element = document.createElement(TYPE);
        element.appendChild(document.createTextNode(machineTypeString));
        rootElement.appendChild(element);
        Log.v(TAG, "typeElement Created");

        Element automatonElement = document.createElement(AUTOMATON);

        //create statesElements
        for (State state : stateList) {
            Element stateElement = document.createElement(STATE);
            stateElement.setAttribute(ID, String.valueOf(state.getId()));
            stateElement.setAttribute(NAME, state.getValue());

            element = document.createElement(X);
            element.appendChild(document.createTextNode(state.getPositionX() + ".0"));
            stateElement.appendChild(element);

            element = document.createElement(Y);
            element.appendChild(document.createTextNode(state.getPositionY() + ".0"));
            stateElement.appendChild(element);

            if (state.isInitialState()) {
                element = document.createElement(INITIAL);
                stateElement.appendChild(element);
            }

            if (state.isFinalState()) {
                element = document.createElement(FINAL);
                stateElement.appendChild(element);
            }

            automatonElement.appendChild(stateElement);
            Log.v(TAG, "stateElement created");
        }
        Log.v(TAG, "stateElements created");

        LongSparseArray<String> inputSymbolReplacementMap = new LongSparseArray<>();
        LongSparseArray<String> stackSymbolReplacementMap = new LongSparseArray<>();
        //create transitionsElements
        switch (machineType) {
            case MainActivity.FINITE_STATE_AUTOMATON:
                for (Transition transition : transitionList) {
                    Element transitionElement = document.createElement(TRANSITION);

                    element = document.createElement(FROM);
                    element.appendChild(document.createTextNode(String.valueOf(transition.getFromState().getId())));
                    transitionElement.appendChild(element);

                    element = document.createElement(TO);
                    element.appendChild(document.createTextNode(String.valueOf(transition.getToState().getId())));
                    transitionElement.appendChild(element);

                    element = document.createElement(READ);
                    //check empty symbol and check length of symbol value, if more than 1, find replacement
                    String replacement = checkLength(inputAlphabetList, inputSymbolReplacementMap, transition.getReadSymbol());
                    if (replacement != null) {
                        element.appendChild(document.createTextNode(replacement));
                    }
                    transitionElement.appendChild(element);

                    automatonElement.appendChild(transitionElement);
                    Log.v(TAG, "transitionElement created");
                }
                break;
            case MainActivity.PUSHDOWN_AUTOMATON:
                for (Transition transition : transitionList) {
                    Element transitionElement = document.createElement(TRANSITION);

                    element = document.createElement(FROM);
                    element.appendChild(document.createTextNode(String.valueOf(transition.getFromState().getId())));
                    transitionElement.appendChild(element);

                    element = document.createElement(TO);
                    element.appendChild(document.createTextNode(String.valueOf(transition.getToState().getId())));
                    transitionElement.appendChild(element);

                    element = document.createElement(READ);
                    //check empty symbol and check length of symbol value, if more than 1, find replacement
                    String replacement = checkLength(inputAlphabetList, inputSymbolReplacementMap, transition.getReadSymbol());
                    if (replacement != null) {
                        element.appendChild(document.createTextNode(replacement));
                    }
                    transitionElement.appendChild(element);

                    element = document.createElement(POP);
                    //prepare popString
                    List<Symbol> popSymbolList = ((PdaTransition) transition).getPopSymbolList();
                    if (!popSymbolList.isEmpty()) {
                        StringBuilder popString = new StringBuilder();
                        for (Symbol symbol : popSymbolList) {
                            //check length of symbol value, if more than 1, find replacement
                            replacement = checkLength(stackAlphabetList, stackSymbolReplacementMap, symbol);
                            if (replacement != null) {
                                popString.append(new StringBuilder(replacement));
                            } else {
                                popString.append(new StringBuilder(stackAlphabetList.get(0).getValue()));
                            }
                        }
                        element.appendChild(document.createTextNode(popString.reverse().toString()));
                    }
                    transitionElement.appendChild(element);

                    element = document.createElement(PUSH);
                    //prepare pushString
                    List<Symbol> pushSymbolList = ((PdaTransition) transition).getPushSymbolList();
                    if (!pushSymbolList.isEmpty()) {
                        StringBuilder pushString = new StringBuilder();
                        for (Symbol symbol : pushSymbolList) {
                            //check length of symbol value, if more than 1, find replacement
                            replacement = checkLength(stackAlphabetList, stackSymbolReplacementMap, symbol);
                            if (replacement != null) {
                                pushString.append(new StringBuilder(replacement));
                            } else {
                                pushString.append(new StringBuilder(stackAlphabetList.get(0).getValue()));
                            }
                        }
                        element.appendChild(document.createTextNode(pushString.reverse().toString()));
                    }
                    transitionElement.appendChild(element);

                    automatonElement.appendChild(transitionElement);
                    Log.v(TAG, "transitionElement created");
                }
                break;
            case MainActivity.LINEAR_BOUNDED_AUTOMATON:
            case MainActivity.TURING_MACHINE:
                for (Transition transition : transitionList) {
                    Element transitionElement = document.createElement(TRANSITION);

                    element = document.createElement(FROM);
                    element.appendChild(document.createTextNode(String.valueOf(transition.getFromState().getId())));
                    transitionElement.appendChild(element);

                    element = document.createElement(TO);
                    element.appendChild(document.createTextNode(String.valueOf(transition.getToState().getId())));
                    transitionElement.appendChild(element);

                    element = document.createElement(READ);
                    //check empty symbol and check length of symbol value, if more than 1, find replacement
                    String replacement = checkLength(inputAlphabetList, inputSymbolReplacementMap, transition.getReadSymbol());
                    if (replacement != null) {
                        element.appendChild(document.createTextNode(replacement));
                    }
                    transitionElement.appendChild(element);

                    element = document.createElement(WRITE);
                    //check empty symbol and check length of symbol value, if more than 1, find replacement
                    replacement = checkLength(inputAlphabetList, inputSymbolReplacementMap, ((TmTransition) transition).getWriteSymbol());
                    if (replacement != null) {
                        element.appendChild(document.createTextNode(replacement));
                    }
                    transitionElement.appendChild(element);

                    element = document.createElement(MOVE);
                    element.appendChild(document.createTextNode(((TmTransition) transition).getDirection().toString()));
                    transitionElement.appendChild(element);

                    automatonElement.appendChild(transitionElement);
                    Log.v(TAG, "transitionElement created");
                }
                break;
        }
        Log.v(TAG, "transitionElements created");

        rootElement.appendChild(automatonElement);

        document.appendChild(rootElement);
        Log.i(TAG, "rootElement attached to document");
    }

    private void setDataJFF(List<GrammarRule> grammarRules) {
        Log.v(TAG, "setDataJFF method started");

        Element rootElement = document.createElement(ROOT);
        Element typeElement = document.createElement(TYPE);
        typeElement.appendChild(document.createTextNode(GRAMMAR));
        rootElement.appendChild(typeElement);

        for (int i = 0; i < grammarRules.size(); i++) {
            Element productionElement = document.createElement(PRODUCTION);

            Element leftElement = document.createElement(LEFT);
            leftElement.appendChild(document.createTextNode(grammarRules.get(i).getGrammarLeft()));
            productionElement.appendChild(leftElement);

            Element rightElement = document.createElement(RIGHT);
            if (grammarRules.get(i).getGrammarRight().equals("ε")) {
                rightElement.appendChild(document.createTextNode(""));
            } else {
                rightElement.appendChild(document.createTextNode(grammarRules.get(i).getGrammarRight()));
            }
            productionElement.appendChild(rightElement);

            rootElement.appendChild(productionElement);
        }
        document.appendChild(rootElement);
        Log.i(TAG, "rootElement attached to document");
    }

    private void setDataCMSG(List<GrammarRule> grammarRules, List<String> tests) {
        Log.v(TAG, "setDataCMSG method started");

        Element rootElement = document.createElement(GRAMMAR);

        for (int i = 0; i < grammarRules.size(); i++) {
            final GrammarRule current = grammarRules.get(i);
            if (current == null)
                continue;
            if (current.getGrammarLeft() == null || current.getGrammarRight() == null)
                continue;
            Element ruleElement = document.createElement(RULE);

            Element leftElement = document.createElement(LEFT);
            leftElement.appendChild(document.createTextNode(current.getGrammarLeft()));
            ruleElement.appendChild(leftElement);

            Element rightElement = document.createElement(RIGHT);
            rightElement.appendChild(document.createTextNode(current.getGrammarRight()));
            ruleElement.appendChild(rightElement);

            rootElement.appendChild(ruleElement);
        }

        Element testsElement = document.createElement(TEST_SCENARIOS);
        for (int i = 0; i < tests.size(); i++) {
            Element testScenarioElement = document.createElement(TEST_SCENARIO);
            testScenarioElement.setAttribute(INPUT_WORD, tests.get(i));
            testsElement.appendChild(testScenarioElement);
        }
        rootElement.appendChild(testsElement);
        Log.i(TAG, "ruleElement created");
        document.appendChild(rootElement);
        Log.i(TAG, "rootElement attached to document");
    }

    private void setDataCMSG(List<GrammarRule> grammarRules) {
        Log.v(TAG, "setDataCMSG method started");

        Element rootElement = document.createElement(GRAMMAR);

        for (int i = 0; i < grammarRules.size(); i++) {
            Element ruleElement = document.createElement(RULE);

            Element leftElement = document.createElement(LEFT);
            leftElement.appendChild(document.createTextNode(grammarRules.get(i).getGrammarLeft()));
            ruleElement.appendChild(leftElement);

            Element rightElement = document.createElement(RIGHT);
            rightElement.appendChild(document.createTextNode(grammarRules.get(i).getGrammarRight()));
            ruleElement.appendChild(rightElement);

            rootElement.appendChild(ruleElement);
        }
        Log.i(TAG, "ruleElement created");
        document.appendChild(rootElement);
        Log.i(TAG, "rootElement attached to document");
    }

    private String checkLength(List<Symbol> alphabetList, LongSparseArray<String> replacementMap, Symbol symbol) {
        //if empty symbol replace with null
        if (alphabetList.get(0) == symbol) {
            return null;
        }
        //length == 1 is OK
        if (symbol.getValue().length() == 1) {
            return symbol.getValue();
        }
        //replace with existing replacement
        String replacementString = replacementMap.get(symbol.getId());
        if (replacementString != null) {
            return replacementString;
        }
        //try to find new replacement in the same character type
        for (int i = 0; i < symbol.getValue().length(); i++) {
            char startValue = symbol.getValue().charAt(i);
            if (startValue >= 'a' && startValue <= 'z') {
                int range = 'z' - 'a' + 1;
                for (int j = 0; j < range; j++) {
                    char tryValueChar = (char) (((startValue - 'a' + j) % range) + 'a');
                    String tryValue = String.valueOf(tryValueChar);
                    int index = getSymbolIndex(alphabetList, tryValue);
                    //free value found
                    if (index == -1) {
                        boolean dupCheck = false;
                        //check if mapping already exists
                        for (int k = 0; k < replacementMap.size(); k++) {
                            if (replacementMap.valueAt(k).equals(tryValue)) {
                                dupCheck = true;
                                break;
                            }
                        }
                        if (!dupCheck) {
                            replacementMap.put(symbol.getId(), tryValue);
                            return tryValue;
                        }
                    }
                }
            } else if (startValue >= 'A' && startValue <= 'Z') {
                int range = 'Z' - 'A' + 1;
                for (int j = 0; j < range; j++) {
                    char tryValueChar = (char) (((startValue - 'A' + j) % range) + 'A');
                    String tryValue = String.valueOf(tryValueChar);
                    int index = getSymbolIndex(alphabetList, tryValue);
                    //free value found
                    if (index == -1) {
                        boolean dupCheck = false;
                        //check if mapping already exists
                        for (int k = 0; k < replacementMap.size(); k++) {
                            if (replacementMap.valueAt(k).equals(tryValue)) {
                                dupCheck = true;
                                break;
                            }
                        }
                        if (!dupCheck) {
                            replacementMap.put(symbol.getId(), tryValue);
                            return tryValue;
                        }
                    }
                }
            } else if (startValue >= '0' && startValue <= '9') {
                int range = '9' - '0' + 1;
                for (int j = 0; j < range; j++) {
                    char tryValueChar = (char) (((startValue - '0' + j) % range) + '0');
                    String tryValue = String.valueOf(tryValueChar);
                    int index = getSymbolIndex(alphabetList, tryValue);
                    //free value found
                    if (index == -1) {
                        boolean dupCheck = false;
                        //check if mapping already exists
                        for (int k = 0; k < replacementMap.size(); k++) {
                            if (replacementMap.valueAt(k).equals(tryValue)) {
                                dupCheck = true;
                                break;
                            }
                        }
                        if (!dupCheck) {
                            replacementMap.put(symbol.getId(), tryValue);
                            return tryValue;
                        }
                    }
                }
            }
        }
        //symbol is not in any character type, try to find any replacement
        int range = 'z' - 'a' + 1;
        for (int j = 0; j < range; j++) {
            char tryValueChar = (char) ('a' + j);
            String tryValue = String.valueOf(tryValueChar);
            int index = getSymbolIndex(alphabetList, tryValue);
            //free value found
            if (index == -1) {
                boolean dupCheck = false;
                //check if mapping already exists
                for (int k = 0; k < replacementMap.size(); k++) {
                    if (replacementMap.valueAt(k).equals(tryValue)) {
                        dupCheck = true;
                        break;
                    }
                }
                if (!dupCheck) {
                    replacementMap.put(symbol.getId(), tryValue);
                    return tryValue;
                }
            }
        }
        range = 'Z' - 'A' + 1;
        for (int j = 0; j < range; j++) {
            char tryValueChar = (char) ('A' + j);
            String tryValue = String.valueOf(tryValueChar);
            int index = getSymbolIndex(alphabetList, tryValue);
            //free value found
            if (index == -1) {
                boolean dupCheck = false;
                //check if mapping already exists
                for (int k = 0; k < replacementMap.size(); k++) {
                    if (replacementMap.valueAt(k).equals(tryValue)) {
                        dupCheck = true;
                        break;
                    }
                }
                if (!dupCheck) {
                    replacementMap.put(symbol.getId(), tryValue);
                    return tryValue;
                }
            }
        }
        range = '9' - '0' + 1;
        for (int j = 0; j < range; j++) {
            char tryValueChar = (char) ('0' + j);
            String tryValue = String.valueOf(tryValueChar);
            int index = getSymbolIndex(alphabetList, tryValue);
            //free value found
            if (index == -1) {
                boolean dupCheck = false;
                //check if mapping already exists
                for (int k = 0; k < replacementMap.size(); k++) {
                    if (replacementMap.valueAt(k).equals(tryValue)) {
                        dupCheck = true;
                        break;
                    }
                }
                if (!dupCheck) {
                    replacementMap.put(symbol.getId(), tryValue);
                    return tryValue;
                }
            }
        }
        //replacement not found
        return null;
    }

    //method to get index of Symbol
    private int getSymbolIndex(List<Symbol> list, String string) {
        int index = -1;

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getValue().equals(string)) {
                index = i;
                break;
            }
        }
        return index;
    }

    public Long getEmptyInputSymbolId() {
        return emptyInputSymbolId;
    }

    public Long getStartStackSymbolId() {
        return startStackSymbolId;
    }

    private void createDoc() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        document = db.newDocument();
    }


}